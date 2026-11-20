"""Parses the SHARED .map files (../../python/fml/get-diagnosis-to-{condition,provenance}.map)
into a flat list of static "source field -> target field" edges, for the lineage viewer.

This is a small, grammar-specific recursive-descent parser, NOT a general FML implementation -
it only understands the constructs these two files actually use (confirmed by reading both in
full): "<var>[.path] [as <newvar>] [where <cond>]" on the source side, and
"<var>.path [= <rhs>]" (rhs: create('Type') [as <var>], cc(...), translate(<var>, '<url>',
'Coding'), a bare variable, or a '<literal>' string) on the target side, with "then { ... }"
blocks closed by "} \"<rule-name>\";" - see get-diagnosis-to-condition.map's own header
comment for why the file is written this way (the portable FML subset both engines accept).

Design choice that makes this tractable: every source/target variable name in both files is
GLOBALLY UNIQUE (verified by inspection - MaLaC-HD 1.10.0 itself relies on this too, see the
"documentId as docId2, not a reuse of docId" note in get-diagnosis-to-condition.map), so
resolving a path never needs a lexical-scope stack: a single {name: resolved_path} dict per
file, filled in document order, is enough (document order IS definition-before-use order for
this file's style, exactly like a real StructureMapUtilities/MaLaC-HD run would see it).

Only LEAF value-carrying rules become edges: a rule whose right-hand side is a bare source
variable, or a translate(...) call's first argument. Rules that only build structure
(create('X') as alias, a cc(...) literal CodeableConcept, or a plain string literal) register
the alias's resolved path for children to build on, but emit no edge themselves - the real
field value flows through a LATER, nested rule instead (e.g. "tgt.identifier =
create('Identifier') as docIdentifier" builds the container; "docIdentifier.value = docId2" is
the actual edge). This keeps the trace at leaf-field granularity, which is what the hover
highlight needs.
"""
import re
from dataclasses import dataclass, field

ARRAY_SOURCE_FIELDS = {"diagnosis", "relatedDiagnosis", "coding"}
# Condition/Provenance fields that are 0..* in FHIR R4 and get a fresh entry created by this
# file (meta.profile is handled separately - its two entries are positional literals, not
# data-driven, so they never produce an edge in the first place - see the module docstring).
# "identifier" is deliberately NOT listed here: Condition.identifier (off the resource root,
# "tgt.identifier") IS 0..*, but every OTHER ".identifier" in this file hangs off a
# Reference-typed variable (Reference.identifier is 0..1, singular) - see _is_array_field().
ARRAY_TARGET_FIELDS = {"extension", "category", "agent", "coding"}

# FML's generic choice-element pattern - "<ext>.value = create('<Type>') as X" for
# Extension.value[x] - is the only place this file assigns to a literal ".value" path with a
# create() rhs (every Identifier.value is a plain string field, assigned from a bare source
# variable, never via create()). The rendered FHIR JSON uses the type-suffixed choice name
# (valueBoolean/valueCode/valueReference/...), not the generic "value", so the alias's
# registered path needs the suffix for the LEAF edge beneath it to land on the real field.
def _choice_suffix(fml_type: str) -> str:
    return fml_type[0].upper() + fml_type[1:]


def _is_array_field(path: str, base_path: str) -> bool:
    if path == "identifier":
        return base_path == ""  # Condition.identifier (root) vs. Reference.identifier (0..1)
    return path in ARRAY_TARGET_FIELDS


@dataclass
class Edge:
    source_path: str
    target_path: str
    rule: str
    kind: str  # 'value' | 'translate'
    conceptmap: str | None = None


@dataclass
class _Statement:
    header: str
    children: list = field(default_factory=list)
    name: str | None = None


def _strip_comments(text: str) -> str:
    """Removes '// ... \\n' line comments, but only outside single-quoted FML string
    literals - a plain per-line ``line.split('//')`` would also truncate every line
    containing a literal URL (e.g. "cc('http://...', 'active')"), which both .map files are
    full of."""
    out = []
    in_squote = False
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if in_squote:
            out.append(c)
            if c == "'":
                in_squote = False
            i += 1
            continue
        if c == "'":
            in_squote = True
            out.append(c)
            i += 1
            continue
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            while i < n and text[i] != "\n":
                i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


def _split_trailing_name(header: str):
    header = header.strip()
    m = re.search(r'"([^"]*)"\s*$', header)
    if m:
        return header[: m.start()].strip(), m.group(1)
    return header, None


def _parse_statements(text: str, pos: int):
    """Recursive-descent over a brace/quote-aware character stream. Returns (statements, pos)
    where pos stops right before the enclosing '}' (or at EOF for the outermost call)."""
    n = len(text)
    stmts = []
    while True:
        while pos < n and text[pos] in " \t\r\n":
            pos += 1
        if pos >= n or text[pos] == "}":
            return stmts, pos
        start = pos
        in_squote = in_dquote = False
        depth = 0
        while pos < n:
            c = text[pos]
            if in_squote:
                if c == "'":
                    in_squote = False
            elif in_dquote:
                if c == '"':
                    in_dquote = False
            else:
                if c == "'":
                    in_squote = True
                elif c == '"':
                    in_dquote = True
                elif c == "(":
                    depth += 1
                elif c == ")":
                    depth -= 1
                elif c == "{" and depth == 0:
                    break
                elif c == ";" and depth == 0:
                    break
            pos += 1
        header = text[start:pos].strip()
        if pos < n and text[pos] == "{":
            pos += 1
            children, pos = _parse_statements(text, pos)
            while pos < n and text[pos] in " \t\r\n":
                pos += 1
            if pos >= n or text[pos] != "}":
                raise ValueError("expected '}' near: %r" % text[max(0, pos - 40) : pos + 40])
            pos += 1
            while pos < n and text[pos] in " \t\r\n":
                pos += 1
            name = None
            if pos < n and text[pos] == '"':
                qend = text.index('"', pos + 1)
                name = text[pos + 1 : qend]
                pos = qend + 1
                while pos < n and text[pos] in " \t\r\n":
                    pos += 1
            if pos < n and text[pos] == ";":
                pos += 1
            stmts.append(_Statement(header=header, children=children, name=name))
        elif pos < n and text[pos] == ";":
            pos += 1
            body, name = _split_trailing_name(header)
            stmts.append(_Statement(header=body, name=name))
        else:
            if header:
                body, name = _split_trailing_name(header)
                stmts.append(_Statement(header=body, name=name))
            return stmts, pos


def _join(base: str, seg: str, is_array: bool) -> str:
    token = seg + "[]" if is_array else seg
    return (base + "." + token) if base else token


_SOURCE_RE = re.compile(
    r"^(?P<base>\w+)(?P<path>(?:\.\w+)*)\s*(?:as\s+(?P<alias>\w+))?\s*(?:where\s+(?P<cond>.*))?$"
)
_TARGET_NOEQ_RE = re.compile(r"^(?P<base>\w+)\.(?P<path>\w+)\s+as\s+(?P<alias>\w+)$")
_TARGET_LEFT_RE = re.compile(r"^(?P<base>\w+)\.(?P<path>\w+)$")
_TRAILING_AS_RE = re.compile(r"\s+as\s+(\w+)\s*$")
_TRANSLATE_RE = re.compile(r"^translate\(\s*(\w+)\s*,\s*'([^']*)'")


def _parse_file(text: str, group_source_var: str, group_target_var: str) -> list[Edge]:
    text = _strip_comments(text)
    gm = re.search(r"\bgroup\s+\w+\([^)]*\)\s*\{", text)
    if not gm:
        raise ValueError("no group{...} found")
    body = text[gm.end() :]
    # Trim the matching final '}' (the group's own closing brace).
    last_close = body.rfind("}")
    body = body[:last_close]
    stmts, _ = _parse_statements(body, 0)

    src_vars = {group_source_var: ""}
    tgt_vars = {group_target_var: ""}
    # Aliases created via create('<lowercase-FHIR-primitive-type>') (boolean/code/canonical/
    # string/dateTime/...), as opposed to a complex type (Identifier/Reference/Meta/...). A
    # further ".value" navigation on one of THESE is FML's generic-Element-wrapper pattern
    # reaching into the primitive's own scalar content - it serializes to the SAME JSON field
    # (e.g. Extension.valueBoolean), not a nested one, unlike ".value" on a complex type
    # (Identifier.value really is a separate nested field). See chronic-value/related-value
    # for why this matters: without it, the trace would point one level too deep.
    primitive_aliases: set[str] = set()
    edges: list[Edge] = []

    def resolve_source(expr: str):
        expr = re.sub(r"\s+then\s*$", "", expr).strip()
        m = _SOURCE_RE.match(expr)
        if not m:
            return None
        base = m.group("base")
        if base not in src_vars:
            return None
        full = src_vars[base]
        path = m.group("path") or ""
        for seg in [s for s in path.split(".") if s]:
            full = _join(full, seg, seg in ARRAY_SOURCE_FIELDS)
        alias = m.group("alias")
        if alias:
            src_vars[alias] = full
        return full

    def handle_target(expr: str, rule_name: str | None, resolved_source):
        expr = re.sub(r"\s+then\s*$", "", expr).strip()
        if "=" in expr:
            tleft, trhs = expr.split("=", 1)
            tleft, trhs = tleft.strip(), trhs.strip()
            tm = _TARGET_LEFT_RE.match(tleft)
            if not tm:
                return
            base, path = tm.group("base"), tm.group("path")
            base_path = tgt_vars.get(base, "")
            if path == "value" and base in primitive_aliases:
                # Navigating ".value" on a FHIR-primitive-typed alias stays on the SAME path
                # (see primitive_aliases' comment) - it does not descend a level.
                resolved_target = base_path
            else:
                resolved_target = _join(base_path, path, _is_array_field(path, base_path))
            am = _TRAILING_AS_RE.search(trhs)
            value_expr = trhs
            if am:
                value_expr = trhs[: am.start()].strip()
                alias_target = resolved_target
                if value_expr.startswith("create("):
                    tm3 = re.match(r"create\('([^']+)'\)", value_expr)
                    if tm3:
                        fml_type = tm3.group(1)
                        if fml_type[0].islower():
                            primitive_aliases.add(am.group(1))
                        if path == "value":
                            # Extension.value[x]: the generic "value" FML path serializes as
                            # the type-suffixed choice field (valueBoolean/valueReference/...)
                            # regardless of whether the type itself is primitive or complex.
                            alias_target = _join(base_path, "value" + _choice_suffix(fml_type), False)
                tgt_vars[am.group(1)] = alias_target
            ve = value_expr.strip()
            if ve.startswith("create(") or ve.startswith("cc(") or ve.startswith("'"):
                return
            if ve.startswith("translate("):
                tm2 = _TRANSLATE_RE.match(ve)
                if tm2 and tm2.group(1) in src_vars:
                    edges.append(
                        Edge(src_vars[tm2.group(1)], resolved_target, rule_name or "?", "translate", tm2.group(2))
                    )
                return
            if re.match(r"^\w+$", ve) and ve in src_vars:
                edges.append(Edge(src_vars[ve], resolved_target, rule_name or "?", "value"))
        else:
            tm = _TARGET_NOEQ_RE.match(expr)
            if tm:
                base_path = tgt_vars.get(tm.group("base"), "")
                resolved_target = _join(base_path, tm.group("path"), True)
                tgt_vars[tm.group("alias")] = resolved_target

    def walk(statements):
        for stmt in statements:
            header = stmt.header
            if "->" in header:
                left, right = header.split("->", 1)
            else:
                left, right = header, None
            resolved_source = resolve_source(left)
            if right is not None:
                handle_target(right, stmt.name, resolved_source)
            walk(stmt.children)

    walk(stmts)
    return edges


def parse_condition_map(path: str) -> list[Edge]:
    with open(path, encoding="utf-8") as fh:
        return _parse_file(fh.read(), "src", "tgt")


def parse_provenance_map(path: str) -> list[Edge]:
    with open(path, encoding="utf-8") as fh:
        return _parse_file(fh.read(), "src", "tgt")


if __name__ == "__main__":
    import sys

    for p in sys.argv[1:]:
        print("== %s ==" % p)
        for e in parse_condition_map(p):
            print("  %-55s -> %-45s [%s]%s" % (e.source_path, e.target_path, e.rule,
                                                 (" via " + e.conceptmap) if e.conceptmap else ""))
