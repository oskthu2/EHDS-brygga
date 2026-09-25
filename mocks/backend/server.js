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
 * Extracts the patient ID extension from a parsed SOAP GetCareDocumentation request.
 */
function extractCareDocumentationPatientId(parsed) {
  try {
    const body =
      parsed['Envelope']?.['Body'] ||
      parsed['soapenv:Envelope']?.['soapenv:Body'];

    if (!body) return null;

    const requestKey = Object.keys(body).find((k) =>
      k.toLowerCase().includes('getcaredocumentation')
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
 * Builds a SOAP GetCareDocumentationResponse envelope from an array of care documentation
 * objects (JoL-header v2.2 — see mapping-getcaredocumentation.md).
 */
function buildCareDocumentationSoapResponse(patientId, documents) {
  const careDocumentationBlocks = documents
    .map((d) => {
      const authorBlock = d.authorId
        ? `
          <ns1:author>
            <ns1:authorId>${d.authorId}</ns1:authorId>
            <ns1:name>${d.authorName || ''}</ns1:name>
            <ns1:timestamp>${d.authorTimestamp}</ns1:timestamp>
          </ns1:author>`
        : '';

      const signatureBlock = d.signatureId
        ? `
          <ns1:signature>
            <ns1:signatureId>${d.signatureId}</ns1:signatureId>
            <ns1:name>${d.signatureName || ''}</ns1:name>
            <ns1:timestamp>${d.signatureTimestamp || ''}</ns1:timestamp>
          </ns1:signature>`
        : '';

      const bodyContentBlock = d.noteText
        ? `<ns1:clinicalDocumentNoteText>${d.noteText}</ns1:clinicalDocumentNoteText>`
        : `<ns1:multimediaEntry>
            <ns1:mediaType>${d.multimediaType}</ns1:mediaType>
            ${d.multimediaValue ? `<ns1:value>${d.multimediaValue}</ns1:value>` : `<ns1:reference>${d.multimediaReference}</ns1:reference>`}
          </ns1:multimediaEntry>`;

      return `
      <ns1:careDocumentation>
        <ns1:header>
          <ns1:accessControlHeader>
            <ns1:patientId>
              <ns1:root>1.2.752.129.2.1.3.1</ns1:root>
              <ns1:extension>${patientId}</ns1:extension>
            </ns1:patientId>
            <ns1:accountableHealthcareProvider>${d.accountableHealthcareProvider}</ns1:accountableHealthcareProvider>
            <ns1:accountableCareUnit>${d.accountableCareUnit}</ns1:accountableCareUnit>
            <ns1:blockComparisonTime>${d.blockComparisonTime}</ns1:blockComparisonTime>
            <ns1:approvedForPatient>${d.approvedForPatient}</ns1:approvedForPatient>
          </ns1:accessControlHeader>
          <ns1:sourceSystemId>${d.sourceSystemHSAId}</ns1:sourceSystemId>
          <ns1:record>
            <ns1:recordId>${d.recordId}</ns1:recordId>
            <ns1:timestamp>${d.recordTimestamp}</ns1:timestamp>
          </ns1:record>${authorBlock}${signatureBlock}
        </ns1:header>
        <ns1:body>
          <ns1:clinicalDocumentNoteCode>
            <ns1:code>${d.noteCode}</ns1:code>
            <ns1:codeSystem>${d.noteCodeSystem}</ns1:codeSystem>
            <ns1:displayName>${d.noteDisplayName}</ns1:displayName>
          </ns1:clinicalDocumentNoteCode>
          <ns1:clinicalDocumentNoteTitle>${d.noteTitle}</ns1:clinicalDocumentNoteTitle>
          ${bodyContentBlock}
        </ns1:body>
      </ns1:careDocumentation>`;
    })
    .join('');

  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns1="urn:riv:clinicalprocess:healthcond:description:GetCareDocumentationResponder:3">
  <soapenv:Body>
    <ns1:GetCareDocumentationResponse>
      <ns1:result>
        <ns1:resultCode>OK</ns1:resultCode>
      </ns1:result>${careDocumentationBlocks}
    </ns1:GetCareDocumentationResponse>
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
 * Receives a SOAP request (GetDiagnosis or GetCareDocumentation), parses the patient ID,
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
  const isCareDocumentation =
    requestKey && requestKey.toLowerCase().includes('getcaredocumentation');

  if (isCareDocumentation) {
    const patientId = extractCareDocumentationPatientId(parsed);

    if (!patientId) {
      console.warn('[Backend] Could not extract patientId from GetCareDocumentation SOAP request');
    } else {
      console.log(`[Backend] GetCareDocumentation request for patientId: ${patientId}`);
    }

    const documents =
      patientId && responses.careDocumentations[patientId]
        ? responses.careDocumentations[patientId]
        : [];

    if (documents.length === 0) {
      console.log(`[Backend] No care documentation found for patientId: ${patientId}`);
    } else {
      console.log(`[Backend] Returning ${documents.length} careDocumentation(s) for patientId: ${patientId}`);
    }

    const soapResponse = buildCareDocumentationSoapResponse(patientId || 'unknown', documents);
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
  res.json({ status: 'ok', service: 'GetDiagnosis + GetCareDocumentation mock backend' });
});

app.listen(PORT, () => {
  console.log(`[Backend] RIVTA SOAP mock (GetDiagnosis + GetCareDocumentation) listening on port ${PORT}`);
});
