'use strict';

const express = require('express');
const path = require('path');
const data = require('./data.json');

const app = express();
const PORT = process.env.PORT || 4001;

app.use(express.json());

/**
 * GET /routing?namespace=<ns>
 * Returns all routes matching the given namespace.
 */
app.get('/routing', (req, res) => {
  const { namespace } = req.query;

  if (!namespace) {
    return res.status(400).json({ error: 'Missing required query parameter: namespace' });
  }

  const routes = data.routes
    .filter((r) => r.namespace === namespace)
    .map(({ logicalAddress, physicalAddress, description }) => ({
      logicalAddress,
      physicalAddress,
      description,
    }));

  return res.json({ routes });
});

/**
 * GET /routing/address?namespace=<ns>&logicalAddress=<hsaid>
 * Returns the physicalAddress for a specific namespace + logicalAddress pair.
 */
app.get('/routing/address', (req, res) => {
  const { namespace, logicalAddress } = req.query;

  if (!namespace || !logicalAddress) {
    return res
      .status(400)
      .json({ error: 'Missing required query parameters: namespace, logicalAddress' });
  }

  const route = data.routes.find(
    (r) => r.namespace === namespace && r.logicalAddress === logicalAddress
  );

  if (!route) {
    return res.status(404).json({ error: 'No route found for given namespace and logicalAddress' });
  }

  return res.json({ physicalAddress: route.physicalAddress });
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
