'use strict';

const express = require('express');
const data = require('./data.json');

const app = express();
const PORT = process.env.PORT || 4001;

// BACKEND_URL lets local dev override Docker hostnames in data.json, e.g.:
//   BACKEND_URL=http://localhost:4005 node server.js
const BACKEND_URL = process.env.BACKEND_URL;

function resolvePhysical(physicalAddress) {
  if (!BACKEND_URL) return physicalAddress;
  // Replace the mock-backend Docker hostname with the configured override
  return physicalAddress.replace(/^http:\/\/mock-backend:\d+/, BACKEND_URL);
}

app.use(express.json());

app.get('/routing', (req, res) => {
  const { namespace } = req.query;
  if (!namespace) {
    return res.status(400).json({ error: 'Missing required query parameter: namespace' });
  }
  const routes = data.routes
    .filter(r => r.namespace === namespace)
    .map(({ logicalAddress, physicalAddress, description }) => ({
      logicalAddress,
      physicalAddress: resolvePhysical(physicalAddress),
      description,
    }));
  return res.json({ routes });
});

app.get('/routing/address', (req, res) => {
  const { namespace, logicalAddress } = req.query;
  if (!namespace || !logicalAddress) {
    return res.status(400).json({ error: 'Missing required query parameters: namespace, logicalAddress' });
  }
  const route = data.routes.find(
    r => r.namespace === namespace && r.logicalAddress === logicalAddress
  );
  if (!route) {
    return res.status(404).json({ error: 'No route found for given namespace and logicalAddress' });
  }
  return res.json({ physicalAddress: resolvePhysical(route.physicalAddress) });
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'TAK' });
});

app.listen(PORT, () => {
  console.log(`[TAK] Tjänsteadresskatalog mock listening on port ${PORT}`);
});
