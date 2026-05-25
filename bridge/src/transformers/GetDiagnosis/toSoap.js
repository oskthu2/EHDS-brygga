'use strict';

const NS_SOAP = 'http://schemas.xmlsoap.org/soap/envelope/';
const NS_RIV   = 'urn:riv:itintegration:registry:1';
const NS_SVC   = 'urn:riv:clinicalprocess:activity:conditions:GetDiagnosisResponder:2';

function toSoap({ patientSystem, patientValue, logicalAddress }) {
  // Strip urn:oid: prefix for root
  const root = patientSystem.replace('urn:oid:', '');

  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope
    xmlns:soapenv="${NS_SOAP}"
    xmlns:ric="${NS_RIV}"
    xmlns:ns1="${NS_SVC}">
  <soapenv:Header>
    <ric:LogicalAddress>${escapeXml(logicalAddress)}</ric:LogicalAddress>
  </soapenv:Header>
  <soapenv:Body>
    <ns1:GetDiagnosis>
      <ns1:patientId>
        <ns1:root>${escapeXml(root)}</ns1:root>
        <ns1:extension>${escapeXml(patientValue)}</ns1:extension>
      </ns1:patientId>
    </ns1:GetDiagnosis>
  </soapenv:Body>
</soapenv:Envelope>`;
}

function escapeXml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

module.exports = { toSoap };
