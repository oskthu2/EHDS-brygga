'use strict';
const express = require('express');
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

    // Step 1: TAK – find physical address(es) for logical address(es)
    let logicalAddresses = [];
    if (contract.useTAK) {
      logicalAddresses = await tak.getRoutes(contract.namespace);
    }

    // Step 2: EI – find which source systems have data for this patient
    let engagements = [];
    if (contract.useEI) {
      engagements = await ei.getEngagements(patientSystem, patientValue, contract.namespace);
    }

    // If neither TAK nor EI found anything, use defaults
    const targets = engagements.length > 0
      ? engagements
      : logicalAddresses.map(la => ({ logicalAddress: la.logicalAddress, physicalAddress: la.physicalAddress }));

    const transformer = transformerRegistry.get(contract.transformer);

    for (const target of targets) {
      // Step 3: Spärrtjänst check
      if (contract.useSparr) {
        const blocked = await sparr.isBlocked(patientSystem, patientValue, target.logicalAddress);
        if (blocked) {
          console.log(`Spärr: patient ${patientValue} blocked for ${target.logicalAddress}`);
          continue;
        }
      }

      // Step 4: Call SOAP backend
      const physUrl = target.physicalAddress || await tak.getPhysicalAddress(contract.namespace, target.logicalAddress);
      const soapRequest = transformer.toSoap({ patientSystem, patientValue, logicalAddress: target.logicalAddress });

      let soapResponse;
      try {
        const axios = require('axios');
        const { data } = await axios.post(physUrl, soapRequest, {
          headers: {
            'Content-Type': 'text/xml; charset=utf-8',
            'SOAPAction': `"${contract.soapAction}"`,
          },
          timeout: 10000,
        });
        soapResponse = data;
      } catch (err) {
        console.error(`SOAP call failed for ${target.logicalAddress}:`, err.message);
        continue;
      }

      // Step 5: Transform SOAP response → FHIR resources
      const resources = transformer.fromSoap(soapResponse, { patientSystem, patientValue });
      entries.push(...resources);

      // Step 6: Log to Loggtjänst
      if (contract.useLogg) {
        logg.log({
          requestId,
          patientId: patientValue,
          patientIdSystem: patientSystem,
          serviceContract: contract.namespace,
          logicalAddress: target.logicalAddress,
          resourceType: contract.fhirResource,
          count: resources.length,
        }).catch(err => console.error('Log error:', err.message));
      }
    }

    // Build FHIR Bundle
    const bundle = {
      resourceType: 'Bundle',
      id: requestId,
      meta: { lastUpdated: new Date().toISOString() },
      type: 'searchset',
      total: entries.length,
      entry: entries.map(resource => ({
        fullUrl: `urn:uuid:${resource.id}`,
        resource,
        search: { mode: 'match' },
      })),
    };

    res.json(bundle);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
