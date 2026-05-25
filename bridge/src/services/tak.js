'use strict';
const axios = require('axios');
const TAK_URL = process.env.TAK_URL || 'http://mock-tak:4001';

async function getRoutes(namespace) {
  try {
    const { data } = await axios.get(`${TAK_URL}/routing`, { params: { namespace }, timeout: 5000 });
    return data.routes || [];
  } catch (err) {
    console.error('TAK getRoutes error:', err.message);
    return [];
  }
}

async function getPhysicalAddress(namespace, logicalAddress) {
  try {
    const { data } = await axios.get(`${TAK_URL}/routing/address`, {
      params: { namespace, logicalAddress },
      timeout: 5000,
    });
    return data.physicalAddress;
  } catch (err) {
    console.error('TAK getPhysicalAddress error:', err.message);
    return null;
  }
}

module.exports = { getRoutes, getPhysicalAddress };
