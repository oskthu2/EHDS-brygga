'use strict';

const express = require('express');

const app = express();
const PORT = process.env.PORT || 4004;

app.use(express.json());

/** In-memory log store */
const logEntries = [];

/**
 * POST /log
 * Accepts any JSON body, stores it with a timestamp, and logs to stdout.
 */
app.post('/log', (req, res) => {
  const entry = {
    timestamp: new Date().toISOString(),
    sequenceNumber: logEntries.length + 1,
    payload: req.body,
  };

  logEntries.push(entry);
  console.log('[Logg] New entry:', JSON.stringify(entry));

  return res.status(201).json({ stored: true, sequenceNumber: entry.sequenceNumber });
});

/**
 * GET /logs
 * Returns all stored log entries.
 * Optional query param ?limit=N returns the last N entries.
 */
app.get('/logs', (req, res) => {
  const limit = req.query.limit !== undefined ? parseInt(req.query.limit, 10) : null;

  if (limit !== null && (isNaN(limit) || limit < 0)) {
    return res.status(400).json({ error: 'limit must be a non-negative integer' });
  }

  const entries =
    limit !== null && limit > 0 ? logEntries.slice(-limit) : logEntries.slice();

  return res.json({ count: entries.length, total: logEntries.length, entries });
});

/**
 * DELETE /logs
 * Clears all stored log entries (useful for test cleanup).
 */
app.delete('/logs', (_req, res) => {
  const cleared = logEntries.length;
  logEntries.length = 0;
  console.log(`[Logg] Cleared ${cleared} entries`);
  return res.json({ cleared });
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'Loggtjänst', count: logEntries.length });
});

app.listen(PORT, () => {
  console.log(`[Logg] Loggtjänst mock listening on port ${PORT}`);
});
