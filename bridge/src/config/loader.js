'use strict';
const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

let cached = null;

function loadContracts() {
  if (cached) return cached;
  const file = path.join(__dirname, 'services.yaml');
  cached = yaml.load(fs.readFileSync(file, 'utf8'));
  return cached;
}

module.exports = { loadContracts };
