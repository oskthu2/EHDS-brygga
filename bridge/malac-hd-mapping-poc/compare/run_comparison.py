#!/usr/bin/env python3
"""Diffs GetDiagnosisMapper.java's actual output (dumped by JavaMapperDumpTest, see
../target/java-mapper-dump/*.json) against the MaLaC-HD/Python pipeline's output for the
SAME two fixtures (../python/run_pipeline.py over ../src/test/resources/testmessages/*.xml),
field by field, and reports every difference found.

Random, per-invocation identifiers (Condition.id, Provenance.id, Provenance.target[].reference
- "urn:uuid:" + that same Condition.id) are normalized away before comparing, since neither
mapper is expected to produce the SAME random UUID as the other.

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


def diff_values(path, a, b, diffs):
    if isinstance(a, dict) and isinstance(b, dict):
        for key in sorted(set(a) | set(b)):
            if key not in a:
                diffs.append("%s: missing in Java, present in Python = %r" % (path + "." + key, b[key]))
            elif key not in b:
                diffs.append("%s: present in Java = %r, missing in Python" % (path + "." + key, a[key]))
            else:
                diff_values(path + "." + key, a[key], b[key], diffs)
    elif isinstance(a, list) and isinstance(b, list):
        if len(a) != len(b):
            diffs.append("%s: Java has %d item(s), Python has %d item(s)" % (path, len(a), len(b)))
            return
        a_sorted = _sort_key_for_unordered_list(a)
        b_sorted = _sort_key_for_unordered_list(b)
        for i, (av, bv) in enumerate(zip(a_sorted, b_sorted)):
            diff_values("%s[%d]" % (path, i), av, bv, diffs)
    else:
        if a != b:
            diffs.append("%s: Java = %r, Python = %r" % (path, a, b))


def compare_fixture(xml_name, java_json_name):
    print("=== %s ===" % xml_name)
    java_path = JAVA_DUMP_DIR / java_json_name
    if not java_path.exists():
        print("  SKIPPED - %s not found; run `mvn test -pl malac-hd-mapping-poc` first." % java_path)
        return
    with open(java_path) as f:
        java_entries = normalize(json.load(f))

    python_entries = normalize(run_python_pipeline(FIXTURES_DIR / xml_name))

    if len(java_entries) != len(python_entries):
        print("  DIFFERENT ENTRY COUNT: Java=%d, Python=%d" % (len(java_entries), len(python_entries)))
        return
    if not java_entries:
        print("  OK - both empty (record correctly dropped)")
        return

    diffs = []
    for i, (je, pe) in enumerate(zip(java_entries, python_entries)):
        diff_values("[%d].condition" % i, je["condition"], pe["condition"], diffs)
        diff_values("[%d].provenance" % i, je["provenance"], pe["provenance"], diffs)

    if not diffs:
        print("  OK - identical (modulo normalized UUIDs)")
    else:
        print("  %d difference(s):" % len(diffs))
        for d in diffs:
            print("    - " + d)


def main():
    for xml_name, java_json_name in FIXTURES:
        compare_fixture(xml_name, java_json_name)


if __name__ == "__main__":
    main()
