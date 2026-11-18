"""RIVTA date-string and OID conversion helpers, used by lm_diagnosis_json_builder.py when
building a source JSON instance for EITHER compiled StructureMap transform (Condition or
Provenance). This is the Python equivalent of RivDateParser.parse()/parseInstant()
(se.inera.ehds.mapping.tk.RivDateParser) plus NamingSystemRegistry.oidToUri(...) and the
tjanstekatalog Endpoint-URL string build GetDiagnosisMapper.java does directly in Java - kept
here, in plain, well-tested Python, rather than re-derived FHIRPath, for two independent
reasons, unchanged by the source-model switch documented in malac-hd-evaluation.md ("Den
enade kallmodellen"):

  - GENERAL-001 (timezone): FML/FHIRPath has no timezone database, on either engine, for
    either source model. A real ENGINE/LANGUAGE limitation, not a MaLaC-HD bug.
  - OID -> URI (mapped + urn:oid: fallback) and the Endpoint-URL build: these WOULD work as
    pure FML ("&" string concatenation + translate()/ConceptMap, see HAPI's own
    lm-diagnosis-sourced get-diagnosis-to-condition.map on claude/fml-mapping-9ovqyh, which
    does exactly that) - but a confirmed MaLaC-HD 1.10.0 bug makes "&" silently drop its right
    operand whenever it is sourced from ANY non-built-in-FHIR model (verified again after the
    model switch: still reproduces with inera-ehds-lm-diagnosis-shaped SimpleNamespace
    objects, not just the old generateDS RivtaResponse model - see
    malac-hd-evaluation.md, "Vilka buggar overlevde modellbytet"). Since
    get-diagnosis-to-condition.map/get-diagnosis-to-provenance.map must compile IDENTICALLY on
    both engines, not just work on one, this conversion stays in Python (and, symmetrically,
    in Java - see LmDiagnosisJsonSourceBuilder.java) for BOTH tracks now, not only MaLaC-HD's.
"""
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

STOCKHOLM = ZoneInfo("Europe/Stockholm")
TJANSTEKATALOG_BASE = "https://tjanstekatalogen.inera.se"
HSA_URI = "urn:oid:1.2.752.129.2.1.4.1"

# Mirrors mapping-engine/src/main/resources/naming-systems.yaml (the subset GetDiagnosis uses).
OID_TO_URI = {
    "1.2.752.129.2.1.3.1": "http://electronichealth.se/identifier/personnummer",
    "1.2.752.129.2.1.3.3": "http://electronichealth.se/identifier/samordningsnummer",
    "1.2.752.116.1.1.1.1.3": "https://www.icd10.se/",
    "2.16.840.1.113883.6.3": "http://hl7.org/fhir/sid/icd-10",
    "2.16.840.1.113883.6.96": "http://snomed.info/sct",
}


def oid_to_uri(oid):
    """NamingSystemRegistry.oidToUri(...) equivalent: known OID -> URI, else urn:oid: fallback."""
    return OID_TO_URI.get(oid, "urn:oid:%s" % oid)


def _parse_riv_parts(raw):
    s = raw.strip()
    y, mo, d = int(s[0:4]), int(s[4:6]), int(s[6:8])
    if len(s) >= 14:
        h, mi, sec = int(s[8:10]), int(s[10:12]), int(s[12:14])
    else:
        h, mi, sec = 0, 0, 0
    return datetime(y, mo, d, h, mi, sec, tzinfo=STOCKHOLM)


def riv_to_offset_datetime(raw):
    """RivDateParser.parse(...) equivalent: YYYYMMDD[HHmmss] (Stockholm local) -> ISO-8601 with
    an explicit UTC offset (never a bare "Z" unless it truly is UTC at that instant)."""
    if not raw:
        return raw
    dt = _parse_riv_parts(raw)
    return dt.isoformat(timespec="seconds")


def riv_to_utc_instant(raw):
    """RivDateParser.parseInstant(...) equivalent: converts to a TRUE UTC instant, never just
    an unconverted local timestamp with "Z" appended."""
    if not raw:
        return raw
    dt = _parse_riv_parts(raw).astimezone(timezone.utc)
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")
