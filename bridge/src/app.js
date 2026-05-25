'use strict';
const express = require('express');
const { loadContracts } = require('./config/loader');
const fhirRouter = require('./routes/fhir');
const metadataRouter = require('./routes/metadata');

const app = express();
app.use(express.json());
app.use(express.text({ type: ['application/xml', 'text/xml'], limit: '10mb' }));

app.use((req, _res, next) => {
  req.contracts = loadContracts();
  next();
});

app.use('/metadata', metadataRouter);
app.use('/', fhirRouter);

app.use((_req, res) => res.status(404).json({ resourceType: 'OperationOutcome', issue: [{ severity: 'error', code: 'not-found' }] }));
app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ resourceType: 'OperationOutcome', issue: [{ severity: 'fatal', code: 'exception', diagnostics: err.message }] });
});

module.exports = app;
