'use strict';
const path = require('path');

const registry = new Map();

function get(name) {
  if (!registry.has(name)) {
    const transformer = require(path.join(__dirname, name, 'index.js'));
    registry.set(name, transformer);
  }
  return registry.get(name);
}

module.exports = { get };
