'use strict';

// OID → FHIR URI mappings for code systems used in GetDiagnosis
const CODE_SYSTEM_MAP = {
  '1.2.752.116.1.1.1.1.3': 'https://www.icd10.se/',
  '2.16.840.1.113883.6.3':  'http://hl7.org/fhir/sid/icd-10',
  '2.16.840.1.113883.6.90': 'http://hl7.org/fhir/sid/icd-10-cm',
  '1.2.752.116.1.1.1.1.1': 'https://www.socialstyrelsen.se/KVA',  // KVÅ
};

// OID for identifiers
const ID_SYSTEM_MAP = {
  '1.2.752.129.2.1.3.1': 'urn:oid:1.2.752.129.2.1.3.1', // personnummer
  '1.2.752.129.2.1.3.3': 'urn:oid:1.2.752.129.2.1.3.3', // samordningsnummer
  '1.2.752.129.2.1.4.1': 'urn:oid:1.2.752.129.2.1.4.1', // HSA-id
};

function codeSystemUri(oid) {
  return CODE_SYSTEM_MAP[oid] || `urn:oid:${oid}`;
}

function idSystemUri(oid) {
  return ID_SYSTEM_MAP[oid] || `urn:oid:${oid}`;
}

// YYYYMMDD or YYYYMMDDHHmmss → ISO 8601
function parseRivDate(rivDate) {
  if (!rivDate) return undefined;
  const s = String(rivDate);
  if (s.length === 8) {
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}`;
  }
  if (s.length >= 14) {
    return `${s.slice(0, 4)}-${s.slice(4, 6)}-${s.slice(6, 8)}T${s.slice(8, 10)}:${s.slice(10, 12)}:${s.slice(12, 14)}`;
  }
  return s;
}

// RIVTA diagnosisType → FHIR category coding
const DIAGNOSIS_TYPE_CATEGORY = {
  HD: {
    system: 'http://terminology.hl7.org/CodeSystem/condition-category',
    code: 'encounter-diagnosis',
    display: 'Encounter Diagnosis',
  },
  BY: {
    system: 'https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType',
    code: 'bi-diagnos',
    display: 'Bidiagnos',
  },
};

function diagnosisTypeCategory(type) {
  return DIAGNOSIS_TYPE_CATEGORY[type] || {
    system: 'https://ehds-brygga.inera.se/fhir/CodeSystem/DiagnosisType',
    code: type,
    display: type,
  };
}

module.exports = { codeSystemUri, idSystemUri, parseRivDate, diagnosisTypeCategory };
