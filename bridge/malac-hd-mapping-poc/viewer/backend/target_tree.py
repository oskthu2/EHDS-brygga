"""Generic FHIR-JSON -> navigable-tree walker, shared by both engines' output (Condition and
Provenance are plain dicts by the time they reach here - ca.uhn.fhir's JSON parser on the Java
side, json.loads on the Python side). Every node's `path` uses the SAME "name" / "name[idx]"
dotted convention lineage.py's resolved target paths use, so the frontend can match a lineage
edge's target_path to a tree node by exact string equality - no re-interpretation needed.
"""


def build(value, path="", name="(root)"):
    """Returns a tree node: {name, path, kind: 'leaf'|'object'|'array', value, children}."""
    if isinstance(value, dict):
        children = [build(v, _join(path, k), k) for k, v in value.items()]
        return {"name": name, "path": path, "kind": "object", "value": None, "children": children}
    if isinstance(value, list):
        children = [build(v, "%s[%d]" % (path, i), "[%d]" % i) for i, v in enumerate(value)]
        return {"name": name, "path": path, "kind": "array", "value": None, "children": children}
    # (list items' own path is "<array-field-path>[idx]", attached directly with no dot - see
    # resolve_target_path in lineage.py, which builds paths the same way.)
    return {"name": name, "path": path, "kind": "leaf", "value": value, "children": []}


def _join(path, key):
    return "%s.%s" % (path, key) if path else key
