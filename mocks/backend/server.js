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
        ? `\n            <core:end>${d.periodEnd}</core:end>`
        : '';

      const assertedDateEl = d.assertedDate
        ? `\n            <core:assertedDate>${d.assertedDate}</core:assertedDate>`
        : '';

      return `
      <ns1:diagnosis>
        <core:diagnosisHeader>
          <core:patientId>
            <core:root>1.2.752.129.2.1.3.1</core:root>
            <core:extension>${patientId}</core:extension>
          </core:patientId>
          <core:sourceSystemHSAId>${d.sourceSystemHSAId}</core:sourceSystemHSAId>
          <core:documentTime>${d.documentTime}</core:documentTime>
          <core:careUnitHSAId>${d.careUnitHSAId}</core:careUnitHSAId>
          <core:careProviderHSAId>${d.careProviderHSAId}</core:careProviderHSAId>
        </core:diagnosisHeader>
        <core:diagnosisBody>
          <core:diagnosisCode>
            <core:code>${d.diagnosisCode}</core:code>
            <core:codeSystem>${d.codeSystem}</core:codeSystem>
            <core:displayName>${d.displayName}</core:displayName>
          </core:diagnosisCode>
          <core:diagnosisType>${d.diagnosisType}</core:diagnosisType>
          <core:diagnosisTimePeriod>
            <core:start>${d.periodStart}</core:start>${periodEnd}
          </core:diagnosisTimePeriod>${assertedDateEl}
        </core:diagnosisBody>
      </ns1:diagnosis>`;
    })
    .join('');

  const logId = `mock-log-id-${Date.now()}`;

  // Namespace split mirrors the confirmed pattern on GetCareDocumentationResponder:3:
  // the Responder schema only wraps GetDiagnosis/GetDiagnosisResponse/diagnosis; nested
  // content (diagnosisHeader, diagnosisBody, result) belongs to the domain's own
  // core-components schema. The exact core namespace below is inferred (see
  // se.inera.ehds.mapping.rivta.CoreNamespace) — the official
  // clinicalprocess:activity:conditions:2 XSD could not be located in this session.
  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns1="urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2"
                  xmlns:core="urn:riv:clinicalprocess:activity:conditions:2">
  <soapenv:Body>
    <ns1:GetDiagnosisResponse>${diagnosisBlocks}
      <ns1:result>
        <core:resultCode>OK</core:resultCode>
        <core:logId>${logId}</core:logId>
      </ns1:result>
    </ns1:GetDiagnosisResponse>
  </soapenv:Body>
</soapenv:Envelope>`;
}

/**
 * Builds a SOAP GetCareDocumentationResponse envelope from an array of care documentation
 * objects (JoL-header v2.2 — see mapping-getcaredocumentation.md).
 */
// HSA-id root OID (Inera NTjP/RIVTA) — see naming-systems.yaml.
const HSA_ROOT = '1.2.752.129.2.1.4.1';

function iiType(ns, tag, root, extension) {
  if (!extension) return '';
  return `
          <${ns}:${tag}>
            <${ns}:root>${root}</${ns}:root>
            <${ns}:extension>${extension}</${ns}:extension>
          </${ns}:${tag}>`;
}

function buildCareDocumentationSoapResponse(patientId, documents) {
  const careDocumentationBlocks = documents
    .map((d) => {
      const authorBlock = d.authorId
        ? `
          <core:author>${iiType('core', 'id', HSA_ROOT, d.authorId)}
            <core:name>${d.authorName || ''}</core:name>
            <core:timestamp>${d.authorTimestamp}</core:timestamp>
          </core:author>`
        : '';

      const signatureBlock = d.signatureId
        ? `
          <core:signature>${iiType('core', 'id', HSA_ROOT, d.signatureId)}
            <core:name>${d.signatureName || ''}</core:name>
            <core:timestamp>${d.signatureTimestamp || ''}</core:timestamp>
          </core:signature>`
        : '';

      const bodyContentBlock = d.noteText
        ? `<core:clinicalDocumentNoteText>${d.noteText}</core:clinicalDocumentNoteText>`
        : `<core:multimediaEntry>
            <core:mediaType>${d.multimediaType}</core:mediaType>
            ${d.multimediaValue ? `<core:value>${d.multimediaValue}</core:value>` : `<core:reference>${d.multimediaReference}</core:reference>`}
          </core:multimediaEntry>`;

      return `
      <ns1:careDocumentation>
        <core:header>
          <core:accessControlHeader>${iiType('core', 'accountableHealthcareProvider', HSA_ROOT, d.accountableHealthcareProvider)}${iiType('core', 'accountableCareUnit', HSA_ROOT, d.accountableCareUnit)}
            <core:patientId>
              <core:root>1.2.752.129.2.1.3.1</core:root>
              <core:extension>${patientId}</core:extension>
            </core:patientId>
            <core:blockComparisonTime>${d.blockComparisonTime}</core:blockComparisonTime>
            <core:approvedForPatient>${d.approvedForPatient}</core:approvedForPatient>
          </core:accessControlHeader>${iiType('core', 'sourceSystemId', HSA_ROOT, d.sourceSystemHSAId)}
          <core:record>${iiType('core', 'id', d.sourceSystemHSAId, d.recordId)}
            <core:timestamp>${d.recordTimestamp}</core:timestamp>
          </core:record>${authorBlock}${signatureBlock}
        </core:header>
        <core:body>
          <core:clinicalDocumentNoteCode>
            <core:code>${d.noteCode}</core:code>
            <core:codeSystem>${d.noteCodeSystem}</core:codeSystem>
            <core:displayName>${d.noteDisplayName}</core:displayName>
          </core:clinicalDocumentNoteCode>
          <core:clinicalDocumentNoteTitle>${d.noteTitle}</core:clinicalDocumentNoteTitle>
          ${bodyContentBlock}
        </core:body>
      </ns1:careDocumentation>`;
    })
    .join('');

  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ns1="urn:riv:clinicalprocess:healthcond:description:GetCareDocumentationResponder:3"
                  xmlns:core="urn:riv:clinicalprocess:healthcond:description:3">
  <soapenv:Body>
    <ns1:GetCareDocumentationResponse>${careDocumentationBlocks}
      <ns1:result>
        <core:resultCode>OK</core:resultCode>
      </ns1:result>
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
