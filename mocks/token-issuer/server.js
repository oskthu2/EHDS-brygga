'use strict';

const crypto = require('crypto');
const express = require('express');

const app = express();
const PORT = process.env.PORT || 4007;

const ISSUER = process.env.ISSUER_URL || `http://localhost:${PORT}`;
// Dev-only HMAC key so the mock issues a real, decodable JWT (header.payload.signature).
// Nothing downstream verifies the signature — matching the demo's WSO2 Key Manager mock,
// which is also "avkodad ... utan signaturkontroll" (decoded without signature check).
const DEV_SIGNING_KEY = 'ehds-brygga-mock-token-issuer-dev-key';
const DEFAULT_EXPIRES_IN_SECONDS = 3600;

app.use(express.json());
app.use(express.urlencoded({ extended: false }));

function base64url(input) {
  return Buffer.from(input).toString('base64url');
}

function issueAccessToken(clientId) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const payload = {
    iss: `${ISSUER}/token`,
    sub: clientId,
    client_id: clientId,
    aud: 'ehds-katalogtjanster',
    iat: now,
    exp: now + DEFAULT_EXPIRES_IN_SECONDS,
  };
  const unsigned = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(payload))}`;
  const signature = crypto.createHmac('sha256', DEV_SIGNING_KEY).update(unsigned).digest('base64url');
  return { token: `${unsigned}.${signature}`, expiresIn: DEFAULT_EXPIRES_IN_SECONDS };
}

function extractClientCredentials(req) {
  const authHeader = req.headers.authorization || '';
  if (authHeader.startsWith('Basic ')) {
    const decoded = Buffer.from(authHeader.slice(6), 'base64').toString('utf8');
    const separatorIndex = decoded.indexOf(':');
    if (separatorIndex !== -1) {
      return { clientId: decoded.slice(0, separatorIndex), clientSecret: decoded.slice(separatorIndex + 1) };
    }
  }
  const body = req.body || {};
  return { clientId: body.client_id, clientSecret: body.client_secret };
}

/**
 * Åtkomstintygsutfärdare — mimics the WSO2 Key Manager's OAuth2 client_credentials
 * endpoint in the "T2-katalogtjänster" demo.
 */
app.get('/.well-known/openid-configuration', (_req, res) => {
  res.json({
    issuer: ISSUER,
    token_endpoint: `${ISSUER}/token`,
    grant_types_supported: ['client_credentials'],
    token_endpoint_auth_methods_supported: ['client_secret_basic', 'client_secret_post'],
  });
});

app.post('/token', (req, res) => {
  const grantType = req.body?.grant_type;
  if (grantType !== 'client_credentials') {
    return res.status(400).json({ error: 'unsupported_grant_type' });
  }

  const { clientId, clientSecret } = extractClientCredentials(req);
  if (!clientId || !clientSecret) {
    return res.status(401).json({ error: 'invalid_client', error_description: 'Missing client_id/client_secret' });
  }

  const { token, expiresIn } = issueAccessToken(clientId);
  return res.json({ access_token: token, token_type: 'Bearer', expires_in: expiresIn });
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'Åtkomstintygsutfärdare' });
});

app.listen(PORT, () => {
  console.log(`[Åtkomstintygsutfärdare] mock listening on port ${PORT}`);
});
