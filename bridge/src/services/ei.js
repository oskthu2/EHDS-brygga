'use strict';
const axios = require('axios');
const EI_URL = process.env.EI_URL || 'http://mock-ei:4002';

async function getEngagements(patientSystem, patientId, namespace) {
  try {
    const { data } = await axios.get(`${EI_URL}/engagement`, {
      params: { patientSystem, patientId, namespace },
      timeout: 5000,
    });
    return data.engagements || [];
  } catch (err) {
    console.error('EI getEngagements error:', err.message);
    return [];
  }
}

module.exports = { getEngagements };
