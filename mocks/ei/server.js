'use strict';

const express = require('express');
const data = require('./data.json');

const app = express();
const PORT = process.env.PORT || 4002;

app.use(express.json());

/**
 * GET /engagement?patientSystem=<s>&patientId=<id>&namespace=<ns>
 * Returns engagements matching patient and namespace.
 * If patient not found (or no matching namespace), returns { engagements: [] }.
 */
app.get('/engagement', (req, res) => {
  const { patientSystem, patientId, namespace } = req.query;

  if (!patientSystem || !patientId) {
    return res
      .status(400)
      .json({ error: 'Missing required query parameters: patientSystem, patientId' });
  }

  const engagements = data.engagements.filter(
    (e) =>
      e.patientId === patientId &&
      e.patientSystem === patientSystem &&
      (!namespace || e.namespace === namespace)
  );

  return res.json({ engagements });
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'EI' });
});

app.listen(PORT, () => {
  console.log(`[EI] Engagemangsindex mock listening on port ${PORT}`);
});
