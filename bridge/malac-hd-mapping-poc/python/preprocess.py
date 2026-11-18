"""Pre-processing pass run on a RIVTA GetDiagnosis XML fixture BEFORE it is ever handed to
the MaLaC-HD-generated Python mapper. See ../python/fml/lib-timestamp-type.map for why
GENERAL-001 (timezone) conversion must happen outside the mapping language regardless of
engine (the same conclusion the HAPI/FML track reached, fml-evaluation.md), and
malac-hd-evaluation.md, "En motorbugg: strangkonkatenering (&) over RIVTA-falt", for why the
OID->URI / meta.source string-building also had to move here: a confirmed MaLaC-HD 1.10.0 bug
makes the FML `&` operator silently drop its right-hand operand whenever that operand comes
from this proof of concept's custom (non-FHIR) generateDS model, regardless of whether the
left operand is a literal or the surrounding file uses "imports".

This is the Python equivalent of RivDateParser.parse()/parseInstant()
(se.inera.ehds.mapping.tk.RivDateParser) plus the two string-building steps
GetDiagnosisMapper.java does directly in Java (NamingSystemRegistry.oidToUri(...) and the
meta.source "TJANSTEKATALOG_BASE + '/Endpoint/' + hsaId" concatenation) - kept here, not in a
.map file, for the reasons above, and deliberately kept as plain, well-tested Python rather
than re-derived FHIRPath.
"""
import copy
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
from lxml import etree

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


def _local_tag(elem):
    return etree.QName(elem).localname


def _set_text_for_all(root, tag_name, fn):
    for elem in root.iter():
        if _local_tag(elem) == tag_name and elem.text is not None:
            elem.text = fn(elem.text)


def preprocess_root(root, *, author_time_as_instant=False):
    """Same rewriting as preprocess() (see its docstring), but operating on an ALREADY
    in-memory lxml element (e.g. a single-diagnosis slice built by run_pipeline.py) instead of
    re-parsing a file from disk. Returns a NEW root; the input is left untouched."""
    root = copy.deepcopy(root)

    _set_text_for_all(root, "diagnosisTime", riv_to_offset_datetime)
    _set_text_for_all(root, "signatureTime", riv_to_offset_datetime)
    _set_text_for_all(
        root, "authorTime",
        riv_to_utc_instant if author_time_as_instant else riv_to_offset_datetime,
    )

    _set_text_for_all(root, "codeSystem", oid_to_uri)

    # patientId/type is the ONLY <type> element in this document shape.
    for elem in root.iter():
        if _local_tag(elem) == "type" and elem.getparent() is not None \
                and _local_tag(elem.getparent()) == "patientId":
            elem.text = oid_to_uri(elem.text)

    _set_text_for_all(
        root, "sourceSystemHSAId",
        lambda hsa: "%s/Endpoint/%s" % (TJANSTEKATALOG_BASE, hsa),
    )

    return etree.ElementTree(root)


def preprocess(xml_path, *, author_time_as_instant=False):
    """Returns a NEW lxml tree (the input file is left untouched) with:
      - diagnosisTime, authorTime, signatureTime rewritten to ISO-8601 with an explicit
        Stockholm offset (or, when author_time_as_instant, authorTime rewritten to a true UTC
        instant instead - Provenance.recorded needs the instant form, Condition.recordedDate
        needs the offset form, from the SAME raw field, see ProvenanceBuilder.build() vs.
        GetDiagnosisMapper.java).
      - patientId/type and diagnosisCode/codeSystem rewritten from a bare OID to the resolved
        URI (urn:oid: fallback for an unknown OID).
      - sourceSystemHSAId rewritten to the full tjänstekatalogen Endpoint URL
        (meta.source's final value, computed here instead of in the .map file).
    """
    tree = etree.parse(xml_path)
    return preprocess_root(tree.getroot(), author_time_as_instant=author_time_as_instant)


def preprocess_to_file(xml_path, out_path, **kwargs):
    preprocess(xml_path, **kwargs).write(out_path, xml_declaration=True, encoding="UTF-8")
    return out_path
