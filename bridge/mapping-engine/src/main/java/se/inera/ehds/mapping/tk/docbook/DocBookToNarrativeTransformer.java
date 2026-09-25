package se.inera.ehds.mapping.tk.docbook;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforms a DocBook XML subset (as used by 1177 Inkorg and RIVTA document services)
 * into a FHIR Narrative div (restricted XHTML 1.0 Strict).
 *
 * Design: one small method per DocBook element type. Add new elements here as they
 * are discovered in production data — do not pre-implement elements that haven't
 * been observed.
 *
 * Supported elements: article, section, title, para, emphasis, itemizedlist,
 * orderedlist, listitem, variablelist, varlistentry, term, ulink, link.
 *
 * Unknown elements are passed through (children rendered, wrapper tag dropped).
 */
public class DocBookToNarrativeTransformer {

    /**
     * One semantic section extracted from a DocBook document for Strategy B
     * (mapping-getcaredocumentation.md, DOC-004): mapping into Composition.section
     * instead of a single flat XHTML narrative (Strategy A, see {@link #transform}).
     *
     * {@code narrativeDiv} carries only this section's own content (paragraphs, lists,
     * styled call-out boxes) — nested unstyled {@code <section>} children are pulled out
     * into {@code subsections} instead of being rendered inline, mirroring Composition's
     * own recursive section nesting. A styled section (info/warning/frame/collapsible box,
     * see {@link #detectSectionStyle}) is a call-out inside its parent's content, not a
     * structural heading, so it stays embedded in {@code narrativeDiv} and is never split out.
     */
    public record DocSection(String title, String narrativeDiv, List<DocSection> subsections) {}

    /**
     * Extracts the structural {@code <section>} hierarchy of a DocBook document as a list
     * of {@link DocSection}, for building FHIR Composition.section trees.
     *
     * When the document has no structural sections at all (e.g. a bare {@code <article>}
     * with only paragraphs), the whole document is returned as a single untitled section,
     * as long as it has some content — an empty or unparsable document yields an empty list.
     *
     * Content at the document root that sits alongside structural sections rather than inside
     * one — a styled call-out box or a loose paragraph directly under {@code <article>}, as in
     * the trailing {@code <para>} of the "Bokad tid" example — is not lost: it is collected into
     * one trailing untitled section appended after the structural ones, rather than kept in its
     * original document position. That reordering is a deliberate simplification for Strategy B;
     * Strategy A's flat narrative (see {@link #transform}) always preserves the exact source order.
     */
    public List<DocSection> extractSections(String docBookXml) {
        if (docBookXml == null || docBookXml.isBlank()) return List.of();
        try {
            Element root = parse(docBookXml).getDocumentElement();
            if ("section".equals(root.getTagName()) && detectSectionStyle(root) == null) {
                return List.of(buildSection(root));
            }

            List<DocSection> sections = new ArrayList<>(collectSections(root));
            String remainder = renderOwnContent(root);
            if (hasRenderableContent(remainder)) {
                sections.add(new DocSection(sections.isEmpty() ? titleText(root) : null, remainder, List.of()));
            }
            return sections;
        } catch (Exception e) {
            return List.of();
        }
    }

    private DocSection buildSection(Element sectionEl) {
        return new DocSection(titleText(sectionEl), renderOwnContent(sectionEl), collectSections(sectionEl));
    }

    /** Direct {@code <section>} children of {@code parent} that are structural, not styled call-out boxes. */
    private List<DocSection> collectSections(Element parent) {
        List<DocSection> sections = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && "section".equals(((Element) n).getTagName())
                    && detectSectionStyle((Element) n) == null) {
                sections.add(buildSection((Element) n));
            }
        }
        return sections;
    }

    /** Renders el's own content as a narrative div, excluding its title and its unstyled (structural) subsections. */
    private String renderOwnContent(Element el) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) n;
                if ("title".equals(child.getTagName())) continue;
                if ("section".equals(child.getTagName()) && detectSectionStyle(child) == null) continue;
                transformElement(child, sb, 1);
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                appendText(n.getNodeValue(), sb);
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private boolean hasRenderableContent(String narrativeDiv) {
        return narrativeDiv != null && !narrativeDiv.equals(emptyDiv());
    }

    private String titleText(Element el) {
        Element title = firstChild(el, "title");
        if (title == null) return null;
        StringBuilder sb = new StringBuilder();
        appendTextContent(title, sb);
        String text = sb.toString().trim();
        return text.isEmpty() ? null : text;
    }

    public String transform(String docBookXml) {
        if (docBookXml == null || docBookXml.isBlank()) return emptyDiv();
        try {
            Document doc = parse(docBookXml);
            StringBuilder sb = new StringBuilder();
            sb.append("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
            transformChildren(doc.getDocumentElement(), sb, 1);
            sb.append("</div>");
            return sb.toString();
        } catch (Exception e) {
            return emptyDiv();
        }
    }

    // ── Dispatcher ────────────────────────────────────────────────────────────

    private void transformChildren(Element parent, StringBuilder sb, int depth) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                transformElement((Element) n, sb, depth);
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                appendText(n.getNodeValue(), sb);
            }
        }
    }

    private void transformElement(Element el, StringBuilder sb, int depth) {
        switch (el.getTagName()) {
            case "article"       -> transformChildren(el, sb, depth);
            case "section"       -> handleSection(el, sb, depth);
            case "title"         -> handleTitle(el, sb, depth);
            case "para"          -> handlePara(el, sb, depth);
            case "emphasis"      -> handleEmphasis(el, sb, depth);
            case "itemizedlist"  -> handleItemizedList(el, sb, depth);
            case "orderedlist"   -> handleOrderedList(el, sb, depth);
            case "listitem"      -> handleListItem(el, sb, depth);
            case "variablelist"  -> handleVariableList(el, sb, depth);
            case "varlistentry"  -> handleVarListEntry(el, sb, depth);
            case "ulink", "link" -> handleLink(el, sb, depth);
            default              -> transformChildren(el, sb, depth); // passthrough
        }
    }

    // ── Element handlers ──────────────────────────────────────────────────────

    private void handleSection(Element el, StringBuilder sb, int depth) {
        String cssClass = detectSectionStyle(el);
        if (cssClass != null) {
            sb.append("<div class=\"").append(cssClass).append("\">");
            renderStyledSectionTitle(el, sb, depth);
            transformChildrenExcept(el, sb, depth + 1, "title");
            sb.append("</div>");
        } else {
            transformChildren(el, sb, depth + 1);
        }
    }

    private void handleTitle(Element el, StringBuilder sb, int depth) {
        String tag = headingTag(depth);
        sb.append("<").append(tag).append(">");
        transformChildren(el, sb, depth);
        sb.append("</").append(tag).append(">");
    }

    private void handlePara(Element el, StringBuilder sb, int depth) {
        sb.append("<p>");
        transformChildren(el, sb, depth);
        sb.append("</p>");
    }

    private void handleEmphasis(Element el, StringBuilder sb, int depth) {
        String role = el.getAttribute("role");
        String tag = switch (role) {
            case "bold"      -> "strong";
            case "italics"   -> "em";
            case "underline" -> "u";
            // Semantic roles (information/observe/frame/collapsible) are handled
            // at section level; inside title they are stripped to text only.
            default          -> null;
        };
        if (tag != null) {
            sb.append("<").append(tag).append(">");
            transformChildren(el, sb, depth);
            sb.append("</").append(tag).append(">");
        } else {
            transformChildren(el, sb, depth);
        }
    }

    private void handleItemizedList(Element el, StringBuilder sb, int depth) {
        // mark="hyphen" → CSS-klass för bindestreck istället för bullet
        String mark = el.getAttribute("mark");
        if ("hyphen".equals(mark)) {
            sb.append("<ul class=\"hyphen\">");
        } else {
            sb.append("<ul>");
        }
        transformChildren(el, sb, depth);
        sb.append("</ul>");
    }

    private void handleOrderedList(Element el, StringBuilder sb, int depth) {
        sb.append("<ol>");
        transformChildren(el, sb, depth);
        sb.append("</ol>");
    }

    private void handleListItem(Element el, StringBuilder sb, int depth) {
        sb.append("<li>");
        transformChildren(el, sb, depth);
        sb.append("</li>");
    }

    private void handleVariableList(Element el, StringBuilder sb, int depth) {
        sb.append("<dl>");
        transformChildren(el, sb, depth);
        sb.append("</dl>");
    }

    private void handleVarListEntry(Element el, StringBuilder sb, int depth) {
        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element child = (Element) n;
            if ("term".equals(child.getTagName())) {
                sb.append("<dt>");
                transformChildren(child, sb, depth);
                sb.append("</dt>");
            } else if ("listitem".equals(child.getTagName())) {
                sb.append("<dd>");
                transformChildren(child, sb, depth);
                sb.append("</dd>");
            }
        }
    }

    private void handleLink(Element el, StringBuilder sb, int depth) {
        String href = el.getAttribute("url");
        if (href.isEmpty()) href = "#" + el.getAttribute("linkend");
        String type = el.getAttribute("type");
        sb.append("<a href=\"").append(escape(href)).append("\"");
        if ("_blank".equals(type)) sb.append(" target=\"_blank\"");
        sb.append(">");
        transformChildren(el, sb, depth);
        sb.append("</a>");
    }

    // ── Section style helpers ─────────────────────────────────────────────────

    /** Returns a CSS class name if the section's title carries a semantic emphasis role. */
    private String detectSectionStyle(Element sectionEl) {
        Element title = firstChild(sectionEl, "title");
        if (title == null) return null;
        Element emphasis = firstChild(title, "emphasis");
        if (emphasis == null) return null;
        return switch (emphasis.getAttribute("role")) {
            case "information" -> "info-box";
            case "observe"     -> "warning-box";
            case "frame"       -> "framed-box";
            case "collapsible" -> "collapsible";
            default            -> null;
        };
    }

    /** Renders the title of a styled section as a heading, stripping the emphasis wrapper.
     *  Om emphasis-elementet är tomt (ruta utan rubrik) renderas ingen heading. */
    private void renderStyledSectionTitle(Element sectionEl, StringBuilder sb, int depth) {
        Element title = firstChild(sectionEl, "title");
        if (title == null) return;
        StringBuilder titleText = new StringBuilder();
        appendTextContent(title, titleText);
        String text = titleText.toString().trim();
        if (text.isEmpty()) return; // tom ruta utan rubrik
        String tag = headingTag(depth + 1);
        sb.append("<").append(tag).append(">").append(text).append("</").append(tag).append(">");
    }

    // ── Low-level helpers ─────────────────────────────────────────────────────

    private void transformChildrenExcept(Element parent, StringBuilder sb, int depth, String skipTag) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (!skipTag.equals(((Element) n).getTagName())) {
                    transformElement((Element) n, sb, depth);
                }
            } else if (n.getNodeType() == Node.TEXT_NODE) {
                appendText(n.getNodeValue(), sb);
            }
        }
    }

    /** Recursively collects only text content (no XHTML tags). */
    private void appendTextContent(Node node, StringBuilder sb) {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                appendText(n.getNodeValue(), sb);
            } else if (n.getNodeType() == Node.ELEMENT_NODE) {
                appendTextContent(n, sb);
            }
        }
    }

    private void appendText(String raw, StringBuilder sb) {
        if (raw != null) sb.append(escape(raw));
    }

    private String headingTag(int depth) {
        return "h" + Math.max(2, Math.min(depth, 6));
    }

    private Element firstChild(Element parent, String localName) {
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && localName.equals(((Element) n).getTagName())) {
                return (Element) n;
            }
        }
        return null;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        // XXE hardening
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = f.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String emptyDiv() {
        return "<div xmlns=\"http://www.w3.org/1999/xhtml\"></div>";
    }
}
