'use strict';
const express = require('express');
const router = express.Router();

router.get('/', (req, res) => {
  const contracts = req.contracts.serviceContracts || [];
  const resources = contracts.map(c => ({
    type: c.fhirResource,
    interaction: [{ code: 'search-type' }],
    searchParam: (c.searchParams || []).map(p => ({
      name: p.name,
      type: 'token',
      documentation: p.description,
    })),
  }));

  res.json({
    resourceType: 'CapabilityStatement',
    id: 'ehds-brygga',
    status: 'active',
    date: new Date().toISOString().split('T')[0],
    kind: 'instance',
    software: { name: 'EHDS-brygga', version: '0.1.0' },
    implementation: { description: 'EHDS Bridge – FHIR R4 facade over Inera RIVTA services' },
    fhirVersion: '4.0.1',
    format: ['json'],
    rest: [{
      mode: 'server',
      resource: resources,
    }],
  });
});

module.exports = router;
