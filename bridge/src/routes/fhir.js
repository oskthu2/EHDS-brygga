'use strict';
const express = require('express');
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');
const tak = require('../services/tak');
const ei = require('../services/ei');
const sparr = require('../services/sparr');
const logg = require('../services/logg');
const transformerRegistry = require('../transformers/registry');

const router = express.Router();

router.get('/:resource', async (req, res, next) => {
  const { resource } = req.params;
  const contracts = req.contracts.serviceContracts || [];
  const contract = contracts.find(c => c.fhirResource === resource);
  if (!contract) {
    return res.status(404).json({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'not-found', diagnostics: `No contract for resource: ${resource}` }],
    });
  }

  try {
    const patientIdentifier = req.query['patient.identifier'] || req.query['patient'];
    if (!patientIdentifier) {
      return res.status(400).json({
        resourceType: 'OperationOutcome',
        issue: [{ severity: 'error', code: 'required', diagnostics: 'patient.identifier is required' }],
      });
    }

    // Parse patient identifier (system|value or just value)
    const [patientSystem, patientValue] = patientIdentifier.includes('|')
      ? patientIdentifier.split('|')
      : ['urn:oid:1.2.752.129.2.1.3.1', patientIdentifier];

    const requestId = uuidv4();
    const entries = [];
    const issues = [];

    // Step 1: TAK – get all known routes (logical addresses) for this contract
    let takRoutes = [];
    if (contract.useTAK) {
      takRoutes = await tak.getRoutes(contract.namespace);
    }

    // Step 2: EI – which logical addresses have data for this patient?
    // EI provides logical addresses only; TAK is the authoritative source for physical addresses.
    let logicalAddresses;
    if (contract.useEI) {
      const engagements = await ei.getEngagements(patientSystem, patientValue, contract.namespace);
      logicalAddresses = engagements.length > 0
        ? engagements.map(e => e.logicalAddress)
        : takRoutes.map(r => r.logicalAddress);
    } else {
      logicalAddresses = takRoutes.map(r => r.logicalAddress);
    }

    const transformer = transformerRegistry.get(contract.transformer);

    for (const logicalAddress of logicalAddresses) {
      // Step 3: Spärrtjänst check
      if (contract.useSparr) {
        const blocked = await sparr.isBlocked(patientSystem, patientValue, logicalAddress);
        if (blocked) {
          console.log(`Spärr: patient ${patientValue} blocked for ${logicalAddress}`);
          issues.push({ severity: 'information', code: 'suppressed', diagnostics: `Spärr: ${logicalAddress}` });
          continue;
        }
      }

      // Step 4: TAK – resolve logical address → physical URL
      const physUrl = await tak.getPhysicalAddress(contract.namespace, logicalAddress);
      if (!physUrl) {
        console.warn(`TAK: no physical address for ${logicalAddress}`);
        issues.push({ severity: 'warning', code: 'not-found', diagnostics: `TAK: ingen adress för ${logicalAddress}` });
        continue;
      }

      // Step 5: Call SOAP backend
      const soapRequest = transformer.toSoap({ patientSystem, patientValue, logicalAddress });
      let soapResponse;
      try {
        const { data } = await axios.post(physUrl, soapRequest, {
          headers: {
            'Content-Type': 'text/xml; charset=utf-8',
            'SOAPAction': `"${contract.soapAction}"`,
          },
          timeout: 10000,
        });
        soapResponse = data;
      } catch (err) {
        console.error(`SOAP call failed for ${logicalAddress} (${physUrl}):`, err.message);
        issues.push({ severity: 'error', code: 'exception', diagnostics: `SOAP-fel för ${logicalAddress}: ${err.message}` });
        continue;
      }

      // Step 6: Transform SOAP response → FHIR resources
      const resources = transformer.fromSoap(soapResponse, { patientSystem, patientValue });
      entries.push(...resources);

      // Step 7: Log to Loggtjänst
      if (contract.useLogg) {
        logg.log({
          requestId,
          patientId: patientValue,
          patientIdSystem: patientSystem,
          serviceContract: contract.namespace,
          logicalAddress,
          resourceType: contract.fhirResource,
          count: resources.length,
        }).catch(err => console.error('Log error:', err.message));
      }
    }

    // Build FHIR Bundle; include OperationOutcome if any issues occurred
    const bundleEntries = entries.map(resource => ({
      fullUrl: `urn:uuid:${resource.id}`,
      resource,
      search: { mode: 'match' },
    }));

    if (issues.length > 0) {
      bundleEntries.push({
        fullUrl: `urn:uuid:${uuidv4()}`,
        resource: { resourceType: 'OperationOutcome', issue: issues },
        search: { mode: 'outcome' },
      });
    }

    res.json({
      resourceType: 'Bundle',
      id: requestId,
      meta: { lastUpdated: new Date().toISOString() },
      type: 'searchset',
      total: entries.length,
      entry: bundleEntries,
    });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
