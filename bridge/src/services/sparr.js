'use strict';
const axios = require('axios');
const SPARR_URL = process.env.SPARR_URL || 'http://mock-sparr:4003';

async function isBlocked(patientSystem, patientId, sourceSystem) {
  try {
    const { data } = await axios.post(`${SPARR_URL}/check`, { patientSystem, patientId, sourceSystem }, { timeout: 5000 });
    return data.blocked === true;
  } catch (err) {
    console.error('Spärr check error:', err.message);
    return false; // fail open in mock/dev
  }
}

module.exports = { isBlocked };
