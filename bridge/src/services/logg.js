'use strict';
const axios = require('axios');
const LOGG_URL = process.env.LOGG_URL || 'http://mock-logg:4004';

async function log(entry) {
  await axios.post(`${LOGG_URL}/log`, {
    timestamp: new Date().toISOString(),
    ...entry,
  }, { timeout: 3000 });
}

module.exports = { log };
