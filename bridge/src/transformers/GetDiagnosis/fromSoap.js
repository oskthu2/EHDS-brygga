'use strict';
const { XMLParser } = require('fast-xml-parser');
const { v4: uuidv4 } = require('uuid');
const { codeSystemUri, idSystemUri, parseRivDate, diagnosisTypeCategory } = require('./oidMap');

const CANONICAL_BASE = 'https://ehds-brygga.inera.se/fhir';

const parser = new XMLParser({
  ignoreAttributes: false,
  removeNSPrefix: true,
  isArray: (name) => ['diagnosis'].includes(name),
});

function fromSoap(soapXml, { patientSystem, patientValue }) {
  let parsed;
  try {
    parsed = parser.parse(soapXml);
  } catch (err) {
    console.error('SOAP parse error:', err.message);
    return [];
  }

  const body = parsed?.Envelope?.Body;
  if (!body) return [];

  const response = body.GetDiagnosisResponse || body['GetDiagnosis1Response'];
  if (!response) return [];

  const result = response.result;
  if (result?.resultCode && result.resultCode !== 'OK') {
    console.warn('GetDiagnosis result:', result.resultCode, result.resultText);
    return [];
  }

  const diagnoses = response.diagnosis;
  if (!diagnoses || diagnoses.length === 0) return [];

  return diagnoses.map(diag => diagnosisToCondition(diag, patientSystem, patientValue));
}

function diagnosisToCondition(diag, patientSystem, patientValue) {
  const header = diag.diagnosisHeader || {};
  const body   = diag.diagnosisBody   || {};

  const diagCode   = body.diagnosisCode   || {};
  const timePeriod = body.diagnosisTimePeriod || {};
  const diagType   = body.diagnosisType;

  const onsetDate      = parseRivDate(timePeriod.start);
  const abatementDate  = parseRivDate(timePeriod.end);
  const recordedDate   = parseRivDate(header.documentTime);
  const sourceHSAId    = header.sourceSystemHSAId;

  const catCoding = diagnosisTypeCategory(diagType);
  const clinicalStatus = abatementDate ? 'resolved' : 'active';

  const condition = {
    resourceType: 'Condition',
    id: uuidv4(),
    meta: {
      profile: [`${CANONICAL_BASE}/StructureDefinition/se-ehds-condition`],
    },
    extension: [],
    clinicalStatus: {
      coding: [{
        system: 'http://terminology.hl7.org/CodeSystem/condition-clinical',
        code: clinicalStatus,
      }],
    },
    verificationStatus: {
      coding: [{
        system: 'http://terminology.hl7.org/CodeSystem/condition-ver-status',
        code: 'confirmed',
      }],
    },
    category: [{
      coding: [catCoding],
    }],
    code: {
      coding: [{
        system: codeSystemUri(diagCode.codeSystem),
        code:   diagCode.code,
        display: diagCode.displayName,
      }],
      text: diagCode.displayName,
    },
    subject: {
      identifier: {
        system: idSystemUri(patientSystem.replace('urn:oid:', '')),
        value: patientValue,
      },
    },
  };

  if (onsetDate)     condition.onsetDateTime     = onsetDate;
  if (abatementDate) condition.abatementDateTime = abatementDate;
  if (recordedDate)  condition.recordedDate      = recordedDate;

  if (sourceHSAId) {
    condition.recorder = {
      identifier: {
        system: idSystemUri('1.2.752.129.2.1.4.1'),
        value: sourceHSAId,
      },
    };
    condition.extension.push({
      url: `${CANONICAL_BASE}/StructureDefinition/ext-source-system`,
      valueIdentifier: {
        system: idSystemUri('1.2.752.129.2.1.4.1'),
        value: sourceHSAId,
      },
    });
  }

  if (condition.extension.length === 0) delete condition.extension;

  return condition;
}

module.exports = { fromSoap };
