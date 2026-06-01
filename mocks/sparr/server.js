'use strict';

const express = require('express');

const app = express();
const PORT = process.env.PORT || 4003;

app.use(express.json());

/**
 * POST /check
 * Body: { patientSystem, patientId, careProviderHsaId }
 *
 * Returns whether the patient has blocked the given careProviderHsaId (organisationsnivå).
 * Special case: patientId "000000000000" is fully blocked.
 */
app.post('/check', (req, res) => {
  const { patientSystem, patientId, careProviderHsaId } = req.body || {};

  if (!patientSystem || !patientId || !careProviderHsaId) {
    return res
      .status(400)
      .json({ error: 'Missing required body fields: patientSystem, patientId, careProviderHsaId' });
  }

  // Special test case: magic patient ID is fully blocked
  if (patientId === '000000000000') {
    return res.json({ blocked: true, sparrRecords: ['all'] });
  }

  // No blocks for anyone else in the mock
  return res.json({ blocked: false, sparrRecords: [] });
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'Spärrtjänst' });
});

app.listen(PORT, () => {
  console.log(`[Spärr] Spärrtjänst mock listening on port ${PORT}`);
});
