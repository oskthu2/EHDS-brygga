'use strict';

const express = require('express');
const { XMLParser } = require('fast-xml-parser');
const responses = require('./responses.json');

const app = express();
const PORT = process.env.PORT || 4005;

// Accept raw text/xml and application/soap+xml bodies
app.use(
  express.text({
    type: ['text/xml', 'application/xml', 'application/soap+xml', '*/xml'],
    limit: '1mb',
  })
);
// Also accept plain JSON for health-checking convenience
app.use(express.json());

const xmlParser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '@_',
  removeNSPrefix: true,
});

/**
 * Extracts the patient ID extension from a parsed SOAP GetDiagnosis request.
 * Handles both with-NS and without-NS attribute names.
 */
function extractPatientId(parsed) {
  try {
    // Navigate: Envelope > Body > GetDiagnosisRequest > patientId / PersonId > extension
    const body =
      parsed['Envelope']?.['Body'] ||
      parsed['soapenv:Envelope']?.['soapenv:Body'];

    if (!body) return null;

    // The first child key of Body that contains patientId / PersonId
    const requestKey = Object.keys(body).find((k) =>
      k.toLowerCase().includes('getdiagnosis')
    );
    if (!requestKey) return null;

    const request = body[requestKey];

    // RIVTA uses either "patientId" or "PersonId"
    const patientIdNode =
      request['patientId'] ||
      request['PersonId'] ||
      request['ns1:patientId'] ||
      request['ns1:PersonId'];

    if (!patientIdNode) return null;

    return (
      patientIdNode['extension'] ||
      patientIdNode['ns1:extension'] ||
      patientIdNode['Extension'] ||
      null
    );
  } catch {
    return null;
  }
}

/**
 * Extracts the patient ID extension from a parsed SOAP GetDocumentList request.
 */
function extractDocumentListPatientId(parsed) {
  try {
    const body =
      parsed['Envelope']?.['Body'] ||
      parsed['soapenv:Envelope']?.['soapenv:Body'];

    if (!body) return null;

    const requestKey = Object.keys(body).find((k) =>
      k.toLowerCase().includes('getdocumentlist')
    );
    if (!requestKey) return null;

    const request = body[requestKey];

    const patientIdNode =
      request['patientId'] ||
      request['PersonId'] ||
      request['ns1:patientId'] ||
      request['ns1:PersonId'];

    if (!patientIdNode) return null;

    return (
      patientIdNode['extension'] ||
      patientIdNode['ns1:extension'] ||
      patientIdNode['Extension'] ||
      null
    );
  } catch {
    return null;
  }
}

/**
 * Builds a SOAP GetDiagnosisResponse envelope from an array of diagnosis objects.
 */
function buildSoapResponse(patientId, diagnoses) {
  const diagnosisBlocks = diagnoses
    .map((d) => {
      const periodEnd = d.periodEnd
        ? `\n            <ns1:end>${d.periodEnd}</ns1:end>`
        : '';

      const assertedDateEl = d.assertedDate
        ? `\n            <ns1:assertedDate>${d.assertedDate}</ns1:assertedDate>`
        : '';

      return `
      <ns1:diagnosis>
        <ns1:diagnosisHeader>
          <ns1:patientId>
            <ns1:root>1.2.752.129.2.1.3.1</ns1:root>
            <ns1:extension>${patientId}</ns1:extension>
          </ns1:patientId>
          <ns1:sourceSystemHSAId>${d.sourceSystemHSAId}</ns1:sourceSystemHSAId>
          <ns1:documentTime>${d.documentTime}</ns1:documentTime>
          <ns1:careUnitHSAId>${d.careUnitHSAId}</ns1:careUnitHSAId>
          <ns1:careProviderHSAId>${d.careProviderHSAId}</ns1:careProviderHSAId>
        </ns1:diagnosisHeader>
        <ns1:diagnosisBody>
          <ns1:diagnosisCode>
            <ns1:code>${d.diagnosisCode}</ns1:code>
            <ns1:codeSystem>${d.codeSystem}</ns1:codeSystem>
            <ns1:displayName>${d.displayName}</ns1:displayName>
          </ns1:diagnosisCode>
          <ns1:diagnosisType>${d.diagnosisType}</ns1:diagnosisType>
          <ns1:diagnosisTimePeriod>
            <ns1:start>${d.periodStart}</ns1:start>${periodEnd}
          </ns1:diagnosisTimePeriod>${assertedDateEl}
        </ns1:diagnosisBody>
      </ns1:diagnosis>`;
    })
    .join('');

  const logId = `mock-log-id-${Date.now()}`;

  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns1="urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2">
  <soapenv:Body>
    <ns1:GetDiagnosisResponse>
      <ns1:result>
        <ns1:resultCode>OK</ns1:resultCode>
        <ns1:logId>${logId}</ns1:logId>
      </ns1:result>${diagnosisBlocks}
    </ns1:GetDiagnosisResponse>
  </soapenv:Body>
</soapenv:Envelope>`;
}

/**
 * Builds a SOAP GetDocumentListResponse envelope from an array of document objects.
 */
function buildDocumentListSoapResponse(patientId, documents) {
  const documentBlocks = documents
    .map((d) => {
      return `
      <ns1:documentEntry>
        <ns1:documentId>${d.documentId}</ns1:documentId>
        <ns1:patientId>
          <ns1:root>1.2.752.129.2.1.3.1</ns1:root>
          <ns1:extension>${patientId}</ns1:extension>
        </ns1:patientId>
        <ns1:sourceSystemHSAId>${d.sourceSystemHSAId}</ns1:sourceSystemHSAId>
        <ns1:documentTime>${d.documentTime}</ns1:documentTime>
        <ns1:careUnitHSAId>${d.careUnitHSAId}</ns1:careUnitHSAId>
        <ns1:careProviderHSAId>${d.careProviderHSAId}</ns1:careProviderHSAId>
        <ns1:title>${d.title}</ns1:title>
        <ns1:typeCode>
          <ns1:code>${d.typeCode}</ns1:code>
          <ns1:codeSystem>${d.typeCodeSystem}</ns1:codeSystem>
          <ns1:displayName>${d.typeDisplayName}</ns1:displayName>
        </ns1:typeCode>
        <ns1:statusCode>${d.statusCode}</ns1:statusCode>
      </ns1:documentEntry>`;
    })
    .join('');

  const logId = `mock-log-id-${Date.now()}`;

  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns1="urn:riv:clinicalprocess:healthrecord:GetDocumentListResponder:1">
  <soapenv:Body>
    <ns1:GetDocumentListResponse>
      <ns1:result>
        <ns1:resultCode>OK</ns1:resultCode>
        <ns1:logId>${logId}</ns1:logId>
      </ns1:result>${documentBlocks}
    </ns1:GetDocumentListResponse>
  </soapenv:Body>
</soapenv:Envelope>`;
}

/**
 * Requires an "Authorization: Bearer <token>" header, mirroring how the WSO2 API
 * Gateway in the "T2-katalogtjänster" demo stops calls that lack an åtkomstintyg
 * (401) and lets them through once one is presented (no signature check here —
 * this mock only plays the gateway's enforcement role, not the token issuer's).
 */
function hasBearerToken(req) {
  const authHeader = req.get('Authorization') || '';
  const [scheme, token] = authHeader.split(' ');
  return scheme === 'Bearer' && !!token;
}

function unauthorizedSoapFault(res) {
  res.status(401).set('Content-Type', 'text/xml; charset=utf-8').send(`<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>soapenv:Client</faultcode>
      <faultstring>Missing or invalid Authorization header (Bearer token required)</faultstring>
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>`);
}

/**
 * POST /soap
 * Receives a SOAP request (GetDiagnosis or GetDocumentList), parses the patient ID,
 * and returns the appropriate SOAP response.
 */
app.post('/soap', (req, res) => {
  if (!hasBearerToken(req)) {
    return unauthorizedSoapFault(res);
  }

  const body = req.body;

  if (!body || typeof body !== 'string' || body.trim() === '') {
    res.status(400).set('Content-Type', 'text/xml; charset=utf-8').send(`<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>soapenv:Client</faultcode>
      <faultstring>Empty or missing SOAP body</faultstring>
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>`);
    return;
  }

  let parsed;
  try {
    parsed = xmlParser.parse(body);
  } catch (err) {
    console.error('[Backend] Failed to parse SOAP XML:', err.message);
    res.status(400).set('Content-Type', 'text/xml; charset=utf-8').send(`<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>soapenv:Client</faultcode>
      <faultstring>Invalid XML: ${err.message}</faultstring>
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>`);
    return;
  }

  // Detect operation type
  const bodyNode =
    parsed['Envelope']?.['Body'] ||
    parsed['soapenv:Envelope']?.['soapenv:Body'];
  const requestKey = Object.keys(bodyNode || {}).find((k) => k);
  const isDocumentList =
    requestKey && requestKey.toLowerCase().includes('getdocumentlist');

  if (isDocumentList) {
    const patientId = extractDocumentListPatientId(parsed);

    if (!patientId) {
      console.warn('[Backend] Could not extract patientId from GetDocumentList SOAP request');
    } else {
      console.log(`[Backend] GetDocumentList request for patientId: ${patientId}`);
    }

    const documents =
      patientId && responses.documents[patientId]
        ? responses.documents[patientId]
        : [];

    if (documents.length === 0) {
      console.log(`[Backend] No documents found for patientId: ${patientId}`);
    } else {
      console.log(`[Backend] Returning ${documents.length} document(s) for patientId: ${patientId}`);
    }

    const soapResponse = buildDocumentListSoapResponse(patientId || 'unknown', documents);
    res.set('Content-Type', 'text/xml; charset=utf-8').send(soapResponse);
  } else {
    // GetDiagnosis (default)
    const patientId = extractPatientId(parsed);

    if (!patientId) {
      console.warn('[Backend] Could not extract patientId from SOAP request');
    } else {
      console.log(`[Backend] GetDiagnosis request for patientId: ${patientId}`);
    }

    const diagnoses =
      patientId && responses.diagnoses[patientId]
        ? responses.diagnoses[patientId]
        : [];

    if (diagnoses.length === 0) {
      console.log(`[Backend] No diagnoses found for patientId: ${patientId}`);
    } else {
      console.log(`[Backend] Returning ${diagnoses.length} diagnosis(es) for patientId: ${patientId}`);
    }

    const soapResponse = buildSoapResponse(patientId || 'unknown', diagnoses);
    res.set('Content-Type', 'text/xml; charset=utf-8').send(soapResponse);
  }
});

/**
 * GET /health
 */
app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'GetDiagnosis + GetDocumentList mock backend' });
});

app.listen(PORT, () => {
  console.log(`[Backend] RIVTA SOAP mock (GetDiagnosis + GetDocumentList) listening on port ${PORT}`);
});
