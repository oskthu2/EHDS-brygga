'use strict';

const express = require('express');
const data = require('./data.json');

const app = express();
const PORT = process.env.PORT || 4006;

const ORG_IDENTIFIER_SYSTEM = 'urn:oid:1.2.752.29.4.19';

function toOrganizationAffiliationResource(entry) {
  return {
    resourceType: 'OrganizationAffiliation',
    id: entry.id,
    active: true,
    participatingOrganization: {
      identifier: {
        system: ORG_IDENTIFIER_SYSTEM,
        value: entry.vgHsaId,
      },
      display: entry.description,
    },
  };
}

function toBundle(resources) {
  return {
    resourceType: 'Bundle',
    type: 'searchset',
    total: resources.length,
    entry: resources.map((r) => ({ fullUrl: `OrganizationAffiliation/${r.id}`, resource: r })),
  };
}

app.use(express.json());

/**
 * F1 – medlemsverifiering.
 * GET /OrganizationAffiliation?participating-organization.identifier=<system>|<value>&active=true
 *
 * Mirrors the "T2-katalogtjänster" demo's federationsmedlemskatalog: verifies that the
 * producer named by the tjänstekatalog-uppslag (T1) actually has an active federation
 * membership before the bridge is allowed to call it.
 */
app.get('/OrganizationAffiliation', (req, res) => {
  const { 'participating-organization.identifier': participatingOrgIdentifier, active } = req.query;

  if (!participatingOrgIdentifier) {
    return res.status(400).json({
      resourceType: 'OperationOutcome',
      issue: [{ severity: 'error', code: 'required', diagnostics: 'Missing required query parameter: participating-organization.identifier' }],
    });
  }

  const [system, value] = participatingOrgIdentifier.includes('|')
    ? participatingOrgIdentifier.split('|')
    : [undefined, participatingOrgIdentifier];

  if (system && system !== ORG_IDENTIFIER_SYSTEM) {
    return res.json(toBundle([]));
  }

  // The mock only ever models active affiliations, so active=false never matches.
  if (active === 'false') {
    return res.json(toBundle([]));
  }

  const matches = data.activeMembers.filter((m) => m.vgHsaId === value);
  return res.json(toBundle(matches.map(toOrganizationAffiliationResource)));
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'Federationsmedlemskatalogen' });
});

app.listen(PORT, () => {
  console.log(`[Federationsmedlemskatalogen] mock listening on port ${PORT}`);
});
