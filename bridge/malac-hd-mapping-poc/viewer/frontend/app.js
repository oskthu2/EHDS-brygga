// GetDiagnosis FML lineage viewer - vanilla JS, no build step, no framework.
// Renders the source (RIVTA XML) tree and the two target (Condition/Provenance) trees fully
// expanded (see README.md for why - a deliberate v1 simplification), draws an SVG line for
// every lineage edge between a source leaf and its target leaf(ves), and on hover highlights
// the whole chain: the hovered row, every connected row on the other side(s), and the lines
// between them. Hovering a target row also walks the lineage BACKWARDS to its source(s), and
// a Condition<->Provenance "cross link" (Provenance.target[].reference <-> Condition.id - not
// a RIVTA-sourced field, see lineage.py) highlights between the two target columns too.

const state = {
  fixtures: [],
  data: null,
  // Maps built from `data.lineage`/`data.cross_target_links` after each load:
  bySource: new Map(), // lm_path -> [{resource, target_path}]
  byTarget: new Map(), // "resource.path" -> [{lm_path}]
  crossByKey: new Map(), // "resource.path" -> [{resource, path}] (the other side)
};

const el = (sel) => document.querySelector(sel);

async function loadFixtures() {
  const res = await fetch("/api/fixtures");
  state.fixtures = await res.json();
  const sel = el("#fixture");
  sel.innerHTML = "";
  for (const f of state.fixtures) {
    const opt = document.createElement("option");
    opt.value = f.name;
    opt.textContent = f.name;
    sel.appendChild(opt);
  }
}

function fillDiagnosisSelect(count) {
  const sel = el("#diagnosis");
  sel.innerHTML = "";
  for (let i = 0; i < count; i++) {
    const opt = document.createElement("option");
    opt.value = String(i);
    opt.textContent = "diagnosis[" + i + "]";
    sel.appendChild(opt);
  }
  sel.style.display = count > 1 ? "" : "none";
}

async function loadMap() {
  const fixture = el("#fixture").value;
  const engine = el("#engine").value;
  const diagnosis = el("#diagnosis").value || "0";
  if (!fixture) return;
  const url = "/api/map?fixture=" + encodeURIComponent(fixture) + "&engine=" + engine + "&diagnosis=" + diagnosis;
  const res = await fetch(url);
  const data = await res.json();
  if (data.error) {
    el("#note").textContent = "Fel: " + data.error;
    return;
  }
  state.data = data;
  fillDiagnosisSelect(data.diagnosis_count);
  el("#diagnosis").value = String(data.diagnosis_index);
  el("#note").textContent = data.note || "";
  buildIndexes(data);
  render(data);
}

function buildIndexes(data) {
  state.bySource = new Map();
  state.byTarget = new Map();
  state.crossByKey = new Map();
  for (const e of data.lineage) {
    const tKey = e.resource + "." + e.target_path;
    if (!state.bySource.has(e.source_path)) state.bySource.set(e.source_path, []);
    state.bySource.get(e.source_path).push({ resource: e.resource, target_path: e.target_path });
    if (!state.byTarget.has(tKey)) state.byTarget.set(tKey, []);
    state.byTarget.get(tKey).push({ lm_path: e.source_path });
  }
  for (const link of data.cross_target_links || []) {
    const fromKey = link.from_resource + "." + link.from_path;
    const toKey = link.to_resource + "." + link.to_path;
    if (!state.crossByKey.has(fromKey)) state.crossByKey.set(fromKey, []);
    state.crossByKey.get(fromKey).push({ resource: link.to_resource, path: link.to_path });
    if (!state.crossByKey.has(toKey)) state.crossByKey.set(toKey, []);
    state.crossByKey.get(toKey).push({ resource: link.from_resource, path: link.from_path });
  }
}

function renderNode(node, depth, isSource) {
  const row = document.createElement("div");
  row.className = "row" + (node.kind === "leaf" ? " leaf" : "");
  row.style.paddingLeft = 6 + depth * 14 + "px";

  if (isSource) {
    row.dataset.xmlpath = node.path;
    if (node.lm_path) row.dataset.lmpath = node.lm_path;
    else if (node.kind === "leaf") row.classList.add("unmapped");
  } else {
    row.dataset.respath = node.path; // resource prefix added by caller via closure, see renderTree
  }

  const name = document.createElement("span");
  name.className = "name";
  name.textContent = node.name + (node.kind !== "leaf" ? "" : ":");
  row.appendChild(name);

  if (node.kind === "leaf") {
    const val = document.createElement("span");
    val.className = "value";
    val.textContent = node.value === null ? "null" : String(node.value);
    row.appendChild(val);
    row.classList.add("hoverable");
  }

  const wrap = document.createElement("div");
  wrap.appendChild(row);
  if (node.children && node.children.length) {
    for (const child of node.children) {
      wrap.appendChild(renderNode(child, depth + 1, isSource));
    }
  }
  return wrap;
}

function renderTree(containerId, node, isSource, resourcePrefix) {
  const container = el(containerId);
  container.innerHTML = "";
  const built = renderNode(node, 0, isSource);
  if (!isSource) {
    // Every row in a target tree also needs its full "resource.path" key for hover lookups,
    // and should be hoverable even when it is a container (e.g. category[0].coding[0] - a
    // translate() result is a whole Coding, not a single scalar - see lineage.py).
    built.querySelectorAll(".row").forEach((r) => {
      const p = r.dataset.respath;
      r.dataset.respath = p ? resourcePrefix + "." + p : resourcePrefix;
      r.classList.add("hoverable");
    });
  }
  container.appendChild(built);
}

function render(data) {
  renderTree("#tree-source", data.source_tree, true, null);
  renderTree("#tree-condition", data.target_condition_tree, false, "condition");
  renderTree("#tree-provenance", data.target_provenance_tree, false, "provenance");
  attachHoverHandlers();
  clearLines();
}

function attachHoverHandlers() {
  document.querySelectorAll(".row.hoverable").forEach((row) => {
    row.addEventListener("mouseenter", () => onHover(row));
    row.addEventListener("mouseleave", clearHighlight);
  });
}

function clearHighlight() {
  document.querySelectorAll(".hl-source, .hl-target").forEach((r) => r.classList.remove("hl-source", "hl-target"));
  clearLines();
}

function clearLines() {
  const svg = el("#lines");
  svg.innerHTML = "";
}

function findSourceRowsByLmPath(lmPath) {
  return Array.from(document.querySelectorAll('#tree-source [data-lmpath]')).filter(
    (r) => r.dataset.lmpath === lmPath
  );
}

function findTargetRow(resource, path) {
  const container = resource === "condition" ? "#tree-condition" : "#tree-provenance";
  const key = resource + "." + path;
  return Array.from(document.querySelectorAll(container + " [data-respath]")).find(
    (r) => r.dataset.respath === key
  );
}

function onHover(row) {
  clearHighlight();
  const pairs = []; // {a: element, b: element, kind: 'lineage'|'crosslink'}

  if (row.dataset.lmpath) {
    // Hovering a SOURCE leaf: highlight it + every target it feeds.
    row.classList.add("hl-source");
    const targets = state.bySource.get(row.dataset.lmpath) || [];
    for (const t of targets) {
      const tRow = findTargetRow(t.resource, t.target_path);
      if (tRow) {
        tRow.classList.add("hl-target");
        pairs.push({ a: row, b: tRow, kind: "lineage" });
      }
    }
  } else if (row.dataset.respath) {
    // Hovering a TARGET row: highlight it + every source that feeds it (reverse lookup),
    // plus any cross-target link (Condition.id <-> Provenance.target[].reference).
    row.classList.add("hl-target");
    const key = row.dataset.respath;
    const sources = state.byTarget.get(key) || [];
    for (const s of sources) {
      for (const sRow of findSourceRowsByLmPath(s.lm_path)) {
        sRow.classList.add("hl-source");
        pairs.push({ a: sRow, b: row, kind: "lineage" });
      }
    }
    // A container row (e.g. "extension[2]") also inherits its children's edges visually -
    // look up any lineage edge whose target_path starts with this row's own path + ".".
    const prefix = key + ".";
    for (const [tKey, srcs] of state.byTarget.entries()) {
      if (tKey.startsWith(prefix)) {
        for (const s of srcs) {
          for (const sRow of findSourceRowsByLmPath(s.lm_path)) {
            sRow.classList.add("hl-source");
            pairs.push({ a: sRow, b: row, kind: "lineage" });
          }
        }
      }
    }
    for (const link of state.crossByKey.get(key) || []) {
      const otherRow = findTargetRow(link.resource, link.path);
      if (otherRow) {
        otherRow.classList.add("hl-target");
        pairs.push({ a: row, b: otherRow, kind: "crosslink" });
      }
    }
  }

  drawLines(pairs);
}

function drawLines(pairs) {
  const svg = el("#lines");
  svg.innerHTML = "";
  const layoutRect = el("#layout").getBoundingClientRect();
  for (const { a, b, kind } of pairs) {
    const ra = a.getBoundingClientRect();
    const rb = b.getBoundingClientRect();
    // Source rows sit to the left of target rows in every pairing we draw (source->condition,
    // source->provenance, condition->provenance) given the column order in index.html, so
    // "a" exits its right edge and "b" enters its left edge.
    const x1 = ra.right - layoutRect.left;
    const y1 = ra.top + ra.height / 2 - layoutRect.top;
    const x2 = rb.left - layoutRect.left;
    const y2 = rb.top + rb.height / 2 - layoutRect.top;
    const dx = Math.max(24, (x2 - x1) * 0.4);
    const d = `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", d);
    path.setAttribute("class", (kind === "crosslink" ? "crosslink" : "lineage") + " active");
    svg.appendChild(path);
  }
}

window.addEventListener("resize", () => {
  const active = document.querySelector(".hl-source, .hl-target");
  if (active) clearHighlight();
});

el("#fixture").addEventListener("change", () => { fillDiagnosisSelectPlaceholder(); loadMap(); });
el("#diagnosis").addEventListener("change", loadMap);
el("#engine").addEventListener("change", loadMap);

function fillDiagnosisSelectPlaceholder() {
  // Reset to diagnosis 0 whenever the fixture changes; loadMap() re-fills the real options
  // once it knows the new fixture's diagnosis count.
  el("#diagnosis").innerHTML = "";
}

(async function init() {
  await loadFixtures();
  if (state.fixtures.length) {
    await loadMap();
  }
})();
