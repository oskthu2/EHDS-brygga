#!/usr/bin/env python3
"""THREE-WAY comparison, same fixtures, all pairs: GetDiagnosisMapper.java's actual production
output (dumped by JavaMapperDumpTest, ../target/java-mapper-dump/*.json) vs. the MaLaC-HD/
Python pipeline's output (../python/run_pipeline.py) vs. HAPI FHIR's StructureMapUtilities
running the SAME shared ../python/fml/*.map files (dumped by HapiMapperDumpTest,
../target/hapi-mapper-dump/*.json) - field by field, every difference found, for all three
pairings (java-python, java-hapi, python-hapi).

Random, per-invocation identifiers (Condition.id, Provenance.id, Provenance.target[].reference
- "urn:uuid:" + that same Condition.id) are normalized away before comparing, since none of
the three mappers is expected to produce the SAME random UUID as another.

Usage: python3 run_comparison.py
"""
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
POC_ROOT = HERE.parent
JAVA_DUMP_DIR = POC_ROOT / "target" / "java-mapper-dump"
HAPI_DUMP_DIR = POC_ROOT / "target" / "hapi-mapper-dump"
PYTHON_DIR = POC_ROOT / "python"
FIXTURES_DIR = POC_ROOT / "src" / "test" / "resources" / "testmessages"

FIXTURES = [
    ("get-diagnosis-huvuddiagnos.xml", "get-diagnosis-huvuddiagnos.json"),
    ("get-diagnosis-ogiltigt-personnummer.xml", "get-diagnosis-ogiltigt-personnummer.json"),
]

UUID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.IGNORECASE
)


def normalize(obj):
    """Replaces every random UUID (Condition.id, Provenance.id, and the matching
    urn:uuid:... reference) with a fixed placeholder, recursively."""
    if isinstance(obj, dict):
        return {k: normalize(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [normalize(v) for v in obj]
    if isinstance(obj, str):
        if UUID_RE.match(obj):
            return "<uuid>"
        if obj.startswith("urn:uuid:") and UUID_RE.match(obj[len("urn:uuid:"):]):
            return "urn:uuid:<uuid>"
        return obj
    return obj


def run_python_pipeline(xml_path):
    result = subprocess.run(
        [sys.executable, str(PYTHON_DIR / "run_pipeline.py"), str(xml_path)],
        capture_output=True, text=True,
    )
    if result.returncode != 0:
        raise RuntimeError("run_pipeline.py failed for %s:\n%s" % (xml_path, result.stderr))
    return json.loads(result.stdout)


def _sort_key_for_unordered_list(items):
    """FHIR arrays like .extension/.coding/.profile have no meaningful order - sort by a
    representative field (url, system, or the whole JSON-dumped item as a last resort) so an
    order difference alone is never reported as a mapping difference."""
    def key(item):
        if isinstance(item, dict):
            for field in ("url", "system", "code"):
                if field in item:
                    return (0, str(item[field]))
        return (1, json.dumps(item, sort_keys=True))
    return sorted(items, key=key)


def diff_values(path, a, b, label_a, label_b, diffs):
    if isinstance(a, dict) and isinstance(b, dict):
        for key in sorted(set(a) | set(b)):
            if key not in a:
                diffs.append("%s: missing in %s, present in %s = %r" % (path + "." + key, label_a, label_b, b[key]))
            elif key not in b:
                diffs.append("%s: present in %s = %r, missing in %s" % (path + "." + key, label_a, a[key], label_b))
            else:
                diff_values(path + "." + key, a[key], b[key], label_a, label_b, diffs)
    elif isinstance(a, list) and isinstance(b, list):
        if len(a) != len(b):
            diffs.append("%s: %s has %d item(s), %s has %d item(s)" % (path, label_a, len(a), label_b, len(b)))
            return
        a_sorted = _sort_key_for_unordered_list(a)
        b_sorted = _sort_key_for_unordered_list(b)
        for i, (av, bv) in enumerate(zip(a_sorted, b_sorted)):
            diff_values("%s[%d]" % (path, i), av, bv, label_a, label_b, diffs)
    else:
        if a != b:
            diffs.append("%s: %s = %r, %s = %r" % (path, label_a, a, label_b, b))


def compare_pair(label_a, entries_a, label_b, entries_b):
    """Prints and returns the diff list for one pair of {"condition", "provenance"} entry
    lists over the SAME fixture."""
    if len(entries_a) != len(entries_b):
        print("    DIFFERENT ENTRY COUNT: %s=%d, %s=%d" % (label_a, len(entries_a), label_b, len(entries_b)))
        return None
    if not entries_a:
        print("    %s vs %s: OK - both empty (record correctly dropped)" % (label_a, label_b))
        return []

    diffs = []
    for i, (ea, eb) in enumerate(zip(entries_a, entries_b)):
        diff_values("[%d].condition" % i, ea["condition"], eb["condition"], label_a, label_b, diffs)
        diff_values("[%d].provenance" % i, ea["provenance"], eb["provenance"], label_a, label_b, diffs)

    if not diffs:
        print("    %s vs %s: OK - identical (modulo normalized UUIDs)" % (label_a, label_b))
    else:
        print("    %s vs %s: %d difference(s):" % (label_a, label_b, len(diffs)))
        for d in diffs:
            print("      - " + d)
    return diffs


def _load_dump(dump_dir, json_name, engine_label):
    path = dump_dir / json_name
    if not path.exists():
        print("  SKIPPED (%s) - %s not found; run `mvn test -pl malac-hd-mapping-poc` first." % (engine_label, path))
        return None
    with open(path) as f:
        return normalize(json.load(f))


def compare_fixture(xml_name, java_json_name):
    print("=== %s ===" % xml_name)
    java_entries = _load_dump(JAVA_DUMP_DIR, java_json_name, "Java/production")
    hapi_entries = _load_dump(HAPI_DUMP_DIR, java_json_name, "Java/HAPI")
    python_entries = normalize(run_python_pipeline(FIXTURES_DIR / xml_name))

    if java_entries is not None:
        compare_pair("Java/production", java_entries, "Python/MaLaC-HD", python_entries)
    if java_entries is not None and hapi_entries is not None:
        compare_pair("Java/production", java_entries, "Java/HAPI", hapi_entries)
    if hapi_entries is not None:
        compare_pair("Python/MaLaC-HD", python_entries, "Java/HAPI", hapi_entries)


def main():
    for xml_name, java_json_name in FIXTURES:
        compare_fixture(xml_name, java_json_name)


if __name__ == "__main__":
    main()
