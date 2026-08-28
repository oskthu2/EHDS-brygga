'use strict';

const express = require('express');
const data = require('./data.json');

const app = express();
const PORT = process.env.PORT || 4001;

// BACKEND_URL lets local dev override Docker hostnames in data.json, e.g.:
//   BACKEND_URL=http://localhost:4005 node server.js
const BACKEND_URL = process.env.BACKEND_URL;

const ORG_IDENTIFIER_SYSTEM = 'urn:oid:1.2.752.29.4.19';

function resolvePhysical(physicalAddress) {
  if (!BACKEND_URL) return physicalAddress;
  // Replace the mock-backend Docker hostname with the configured override
  return physicalAddress.replace(/^http:\/\/mock-backend:\d+/, BACKEND_URL);
}

function toEndpointResource(entry) {
  return {
    resourceType: 'Endpoint',
    id: entry.id,
    status: 'active',
    connectionType: {
      system: 'http://terminology.hl7.org/CodeSystem/endpoint-connection-type',
      code: 'hl7-fhir-rest',
    },
    name: entry.description,
    managingOrganization: {
      identifier: {
        system: ORG_IDENTIFIER_SYSTEM,
        value: entry.vgHsaId,
      },
    },
    payloadType: [
      {
        coding: [
          {
            system: 'urn:riv:itintegration:registry:tjanstekontrakt',
            code: entry.namespace,
          },
        ],
      },
    ],
    address: resolvePhysical(entry.physicalAddress),
  };
}

function toBundle(resources) {
  return {
    resourceType: 'Bundle',
    type: 'searchset',
    total: resources.length,
    entry: resources.map((r) => ({ fullUrl: `Endpoint/${r.id}`, resource: r })),
  };
}

app.use(express.json());

/**
 * T1 – tjänstesökning.
 * GET /Endpoint?organization.identifier=<system>|<value>&status=active[&implements=<namespace>]
 *
 * Mirrors the "T2-katalogtjänster" demo's tjänstekatalog contract: consumers resolve the
 * physical address of a producer's endpoint themselves, instead of relying on an
 * implicit NTjP-internal routing table.
 */
app.get('/Endpoint', (req, res) => {
  const { 'organization.identifier': organizationIdentifier, status, implements: implementsNs } = req.query;

  if (!organizationIdentifier) {
    return res.status(400).json({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'required', diagnostics: 'Missing required query parameter: organization.identifier' }],
    });
  }

  const [system, value] = organizationIdentifier.includes('|')
    ? organizationIdentifier.split('|')
    : [undefined, organizationIdentifier];

  if (system && system !== ORG_IDENTIFIER_SYSTEM) {
    return res.json(toBundle([]));
  }

  let matches = data.endpoints.filter((e) => e.vgHsaId === value);

  if (status && status !== 'active') {
    matches = [];
  }
  if (implementsNs) {
    matches = matches.filter((e) => e.namespace === implementsNs);
  }

  return res.json(toBundle(matches.map(toEndpointResource)));
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'Tjänstekatalogen' });
});

app.listen(PORT, () => {
  console.log(`[Tjänstekatalogen] mock listening on port ${PORT}`);
});
