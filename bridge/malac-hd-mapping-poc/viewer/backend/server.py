"""Stdlib-only HTTP server for the GetDiagnosis lineage viewer - see ../README.md for what this
is and how to run it. Deliberately plain http.server (no FastAPI/Flask): the only new runtime
dependency this viewer needs beyond what ../../python/requirements.txt already installs is
none at all.

Routes:
  GET /                         -> frontend/index.html
  GET /app.js, /style.css       -> static frontend files
  GET /api/fixtures             -> [{"name": ..., "diagnosisCount": ...}, ...]
  GET /api/map?fixture=<name>&engine=python|java[&diagnosis=<index>]
                                 -> {source_tree, target_condition_tree, target_provenance_tree,
                                     lineage, cross_target_links, guard_ok, diagnosis_count,
                                     related_count, engine, fixture, diagnosis_index}
"""
import json
import sys
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from lxml import etree

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import engines  # noqa: E402
import lineage  # noqa: E402
import source_tree  # noqa: E402
import target_tree  # noqa: E402

PYTHON_DIR = HERE.parent.parent / "python"
sys.path.insert(0, str(PYTHON_DIR))
import guard  # noqa: E402
import lm_diagnosis_json_builder  # noqa: E402

FIXTURES_DIR = HERE.parent.parent / "src" / "test" / "resources" / "testmessages"
FRONTEND_DIR = HERE.parent / "frontend"


def _local_tag(elem):
    return etree.QName(elem).localname


def list_fixtures():
    out = []
    for p in sorted(FIXTURES_DIR.glob("*.xml")):
        root = etree.parse(str(p)).getroot()
        count = sum(1 for c in root if _local_tag(c) == "diagnosis")
        out.append({"name": p.name, "diagnosisCount": count})
    return out


def build_response(fixture_name: str, engine_name: str, diagnosis_index: int):
    xml_path = FIXTURES_DIR / fixture_name
    if not xml_path.is_file():
        raise ValueError("unknown fixture %r" % fixture_name)
    tree = etree.parse(str(xml_path))
    root = tree.getroot()
    all_diag = [c for c in root if _local_tag(c) == "diagnosis"]
    if not all_diag:
        raise ValueError("fixture %r has no <diagnosis> entries" % fixture_name)
    diagnosis_index = max(0, min(diagnosis_index, len(all_diag) - 1))
    diag_elem = all_diag[diagnosis_index]

    kept = guard.valid_diagnoses_in_root(root)
    guard_ok = any(d is diag_elem for d in kept)

    body_elem = None
    for c in diag_elem:
        if _local_tag(c) == "diagnosisBody":
            body_elem = c
            break
    related_count = sum(
        1 for c in (body_elem if body_elem is not None else []) if _local_tag(c) == "relatedDiagnosis"
    )

    src_tree = source_tree.build_diagnosis_tree(diag_elem, diagnosis_index)

    condition, provenance, lineage_edges, cross_links, runtime_hints = {}, {}, [], [], {}
    note = None
    if not guard_ok:
        note = ("DIAG-001: denna diagnospost saknar en giltig 12-siffrig patientidentitet "
                "(eller saknar header/body, eller svarets resultCode != OK) - ingen Condition/"
                "Provenance produceras (fail-closed).")
    else:
        cond_src = lm_diagnosis_json_builder.build_source(diag_elem, author_time_as_instant=False)
        prov_src = lm_diagnosis_json_builder.build_source(diag_elem, author_time_as_instant=True)
        if engine_name == "python":
            condition, provenance, runtime_hints = engines.run_python_engine(cond_src, prov_src)
        elif engine_name == "java":
            condition, provenance, runtime_hints = engines.run_java_engine(cond_src, prov_src)
        else:
            raise ValueError("unknown engine %r (want 'python' or 'java')" % engine_name)

        lineage_edges = lineage.resolve_edges(
            diagnosis_index, related_count, {"condition": condition, "provenance": provenance}, runtime_hints
        )
        cross_links = lineage.resolve_cross_target_links({"condition": condition, "provenance": provenance})

    # Honest provenance of the relatedDiagnosis -> extension[] pairing specifically (the one
    # place a repeating target container's index used to be an order-based ASSUMPTION rather
    # than a FACT - see lineage.py/traced_python_engine.py) - shown by the frontend so nobody
    # mistakes the Java engine's fallback for a real trace.
    related_index_source = (
        "runtime" if (runtime_hints or {}).get("related_extension_index") else
        "heuristic (order-based assumption - no real execution trace for this engine, see README)"
    ) if related_count and guard_ok else None

    return {
        "fixture": fixture_name,
        "engine": engine_name,
        "diagnosis_index": diagnosis_index,
        "diagnosis_count": len(all_diag),
        "related_count": related_count,
        "related_index_source": related_index_source,
        "guard_ok": guard_ok,
        "note": note,
        "source_tree": src_tree,
        "target_condition_tree": target_tree.build(condition, name="Condition"),
        "target_provenance_tree": target_tree.build(provenance, name="Provenance"),
        "lineage": lineage_edges,
        "cross_target_links": cross_links,
    }


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):  # quieter default logging
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def _send_json(self, payload, status=200):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_file(self, path: Path, content_type: str):
        try:
            data = path.read_bytes()
        except OSError:
            self.send_error(404)
            return
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        parsed = urlparse(self.path)
        qs = parse_qs(parsed.query)
        try:
            if parsed.path == "/" or parsed.path == "/index.html":
                self._send_file(FRONTEND_DIR / "index.html", "text/html; charset=utf-8")
            elif parsed.path == "/app.js":
                self._send_file(FRONTEND_DIR / "app.js", "application/javascript; charset=utf-8")
            elif parsed.path == "/style.css":
                self._send_file(FRONTEND_DIR / "style.css", "text/css; charset=utf-8")
            elif parsed.path == "/api/fixtures":
                self._send_json(list_fixtures())
            elif parsed.path == "/api/map":
                fixture = (qs.get("fixture") or [None])[0]
                engine_name = (qs.get("engine") or ["python"])[0]
                diagnosis_index = int((qs.get("diagnosis") or ["0"])[0])
                if not fixture:
                    self._send_json({"error": "missing ?fixture="}, status=400)
                    return
                self._send_json(build_response(fixture, engine_name, diagnosis_index))
            else:
                self.send_error(404)
        except Exception as exc:  # noqa: BLE001 - demo tool: surface the error to the UI
            traceback.print_exc()
            self._send_json({"error": str(exc)}, status=500)


def main():
    port = 8008
    if len(sys.argv) > 1:
        port = int(sys.argv[1])
    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    print("GetDiagnosis lineage viewer: http://127.0.0.1:%d/" % port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
