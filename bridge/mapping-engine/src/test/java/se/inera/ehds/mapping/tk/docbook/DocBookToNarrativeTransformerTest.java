package se.inera.ehds.mapping.tk.docbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocBookToNarrativeTransformerTest {

    private DocBookToNarrativeTransformer t;

    @BeforeEach
    void setUp() {
        t = new DocBookToNarrativeTransformer();
    }

    // ── Iteration 1: grundstruktur ────────────────────────────────────────────

    @Nested
    class Grundstruktur {

        @Test
        void null_ger_tomt_div() {
            assertEquals("<div xmlns=\"http://www.w3.org/1999/xhtml\"></div>", t.transform(null));
        }

        @Test
        void tomt_article_ger_tomt_div() {
            assertEquals(
                    "<div xmlns=\"http://www.w3.org/1999/xhtml\"></div>",
                    t.transform("<article></article>"));
        }

        @Test
        void tomt_section_ger_ingen_css_klass() {
            String result = t.transform("<article><section></section></article>");
            assertFalse(result.contains("class="), "tom section utan styld titel ska inte ha CSS-klass");
        }

        @Test
        void para_mappas_till_p() {
            String result = t.transform("<article><para>Hej världen</para></article>");
            assertTrue(result.contains("<p>Hej världen</p>"));
        }

        @Test
        void section_med_title_ger_rubrik_h2() {
            String result = t.transform(
                    "<article><section><title>Min rubrik</title><para>Text</para></section></article>");
            assertTrue(result.contains("<h2>Min rubrik</h2>"));
            assertTrue(result.contains("<p>Text</p>"));
        }

        @Test
        void djupt_nästlad_section_ger_h3() {
            String result = t.transform(
                    "<article><section><title>Nivå 1</title>"
                    + "<section><title>Nivå 2</title></section>"
                    + "</section></article>");
            assertTrue(result.contains("<h2>Nivå 1</h2>"));
            assertTrue(result.contains("<h3>Nivå 2</h3>"));
        }
    }

    // ── Iteration 2: inline-formattering ─────────────────────────────────────

    @Nested
    class InlineFormattering {

        @Test
        void bold_ger_strong() {
            String result = t.transform(
                    "<article><para><emphasis role=\"bold\">Viktigt</emphasis></para></article>");
            assertTrue(result.contains("<strong>Viktigt</strong>"));
        }

        @Test
        void italics_ger_em() {
            String result = t.transform(
                    "<article><para><emphasis role=\"italics\">kursivt</emphasis></para></article>");
            assertTrue(result.contains("<em>kursivt</em>"));
        }

        @Test
        void underline_ger_u() {
            String result = t.transform(
                    "<article><para><emphasis role=\"underline\">stryks under</emphasis></para></article>");
            assertTrue(result.contains("<u>stryks under</u>"));
        }

        @Test
        void okand_role_strippas_till_text() {
            String result = t.transform(
                    "<article><para><emphasis role=\"okand\">text</emphasis></para></article>");
            assertFalse(result.contains("<emphasis"));
            assertTrue(result.contains("text"));
        }

        @Test
        void xmltecken_escapas() {
            String result = t.transform(
                    "<article><para>a &lt; b &amp; c &gt; d</para></article>");
            assertTrue(result.contains("a &lt; b &amp; c &gt; d"));
        }
    }

    // ── Iteration 3: listor ───────────────────────────────────────────────────

    @Nested
    class Listor {

        @Test
        void itemizedlist_ger_ul_med_li() {
            String result = t.transform(
                    "<article><itemizedlist>"
                    + "<listitem>Punkt ett</listitem>"
                    + "<listitem>Punkt två</listitem>"
                    + "</itemizedlist></article>");
            assertTrue(result.contains("<ul>"));
            assertTrue(result.contains("<li>Punkt ett</li>"));
            assertTrue(result.contains("<li>Punkt två</li>"));
            assertTrue(result.contains("</ul>"));
        }

        @Test
        void orderedlist_ger_ol() {
            String result = t.transform(
                    "<article><orderedlist>"
                    + "<listitem>Steg 1</listitem>"
                    + "</orderedlist></article>");
            assertTrue(result.contains("<ol>"));
            assertTrue(result.contains("<li>Steg 1</li>"));
            assertTrue(result.contains("</ol>"));
        }

        @Test
        void listitem_kan_innehalla_inline_formattering() {
            String result = t.transform(
                    "<article><itemizedlist>"
                    + "<listitem><emphasis role=\"bold\">Viktigt</emphasis>: ta inte cellprov</listitem>"
                    + "</itemizedlist></article>");
            assertTrue(result.contains("<li><strong>Viktigt</strong>: ta inte cellprov</li>"));
        }
    }

    // ── Iteration 4: nyckelvärdelistor ───────────────────────────────────────

    @Nested
    class NyckelvardeLista {

        @Test
        void variablelist_ger_dl_med_dt_dd() {
            String result = t.transform(
                    "<article><variablelist>"
                    + "<varlistentry><term>Datum:</term><listitem>Tisdag 10 mars</listitem></varlistentry>"
                    + "<varlistentry><term>Tid:</term><listitem>11:00</listitem></varlistentry>"
                    + "</variablelist></article>");
            assertTrue(result.contains("<dl>"));
            assertTrue(result.contains("<dt>Datum:</dt>"));
            assertTrue(result.contains("<dd>Tisdag 10 mars</dd>"));
            assertTrue(result.contains("<dt>Tid:</dt>"));
            assertTrue(result.contains("<dd>11:00</dd>"));
            assertTrue(result.contains("</dl>"));
        }

        @Test
        void term_kan_innehalla_formattering() {
            String result = t.transform(
                    "<article><variablelist>"
                    + "<varlistentry>"
                    + "<term><emphasis role=\"bold\">Mottagning:</emphasis></term>"
                    + "<listitem>Barnmorskemottagningen</listitem>"
                    + "</varlistentry>"
                    + "</variablelist></article>");
            assertTrue(result.contains("<dt><strong>Mottagning:</strong></dt>"));
        }
    }

    // ── Iteration 5: semantiska boxar ─────────────────────────────────────────

    @Nested
    class SemantikaBoxar {

        @Test
        void information_i_title_ger_info_box() {
            String result = t.transform(
                    "<article><section>"
                    + "<title><emphasis role=\"information\">Viktigt inför besöket</emphasis></title>"
                    + "<para>Tänk på att...</para>"
                    + "</section></article>");
            assertTrue(result.contains("<div class=\"info-box\">"));
            assertTrue(result.contains(">Viktigt inför besöket<"), "Titel ska finnas i rubriken");
            assertTrue(result.contains("<p>Tänk på att...</p>"));
            assertFalse(result.contains("<emphasis"), "emphasis-tagg ska inte läcka igenom");
        }

        @Test
        void observe_i_title_ger_warning_box() {
            String result = t.transform(
                    "<article><section>"
                    + "<title><emphasis role=\"observe\">Om du inte kan komma</emphasis></title>"
                    + "<para>Ring oss</para>"
                    + "</section></article>");
            assertTrue(result.contains("<div class=\"warning-box\">"));
            assertTrue(result.contains(">Om du inte kan komma<"));
        }

        @Test
        void frame_i_title_ger_framed_box() {
            String result = t.transform(
                    "<article><section>"
                    + "<title><emphasis role=\"frame\">Bakgrundsfakta</emphasis></title>"
                    + "<para>Fakta</para>"
                    + "</section></article>");
            assertTrue(result.contains("<div class=\"framed-box\">"));
        }

        @Test
        void collapsible_i_title_ger_collapsible_statiskt_expanderat() {
            String result = t.transform(
                    "<article><section>"
                    + "<title><emphasis role=\"collapsible\">Varför kallad</emphasis></title>"
                    + "<para>Förklaring</para>"
                    + "</section></article>");
            assertTrue(result.contains("<div class=\"collapsible\">"));
            // Statiskt expanderat – innehållet ska synas direkt
            assertTrue(result.contains("<p>Förklaring</p>"));
        }

        @Test
        void ostyld_section_ger_ingen_extra_div() {
            String result = t.transform(
                    "<article><section>"
                    + "<title>Vanlig rubrik</title>"
                    + "<para>Text</para>"
                    + "</section></article>");
            assertFalse(result.contains("class="), "ostyld section ska inte ha CSS-klass");
        }
    }

    // ── Iteration 6: länkar ───────────────────────────────────────────────────

    @Nested
    class Lankar {

        @Test
        void ulink_ger_a_med_href() {
            String result = t.transform(
                    "<article><para>"
                    + "<ulink url=\"tel:08-123456\">08-123 456</ulink>"
                    + "</para></article>");
            assertTrue(result.contains("<a href=\"tel:08-123456\">08-123 456</a>"));
        }

        @Test
        void link_med_linkend_ger_intern_ankar() {
            String result = t.transform(
                    "<article><para>"
                    + "<link linkend=\"avsnitt1\">Gå till avsnitt 1</link>"
                    + "</para></article>");
            assertTrue(result.contains("<a href=\"#avsnitt1\">Gå till avsnitt 1</a>"));
        }
    }

    // ── Iteration 7: fullständigt kallelsebrev ────────────────────────────────

    @Nested
    class Integrationsscenarion {

        static final String KALLELSE_XML = """
                <?xml version="1.0"?>
                <article>
                  <section>
                    <title>Kallelse till cellprovtagning</title>
                    <para>Du kallas nu till cellprovtagning.</para>
                  </section>
                  <section>
                    <variablelist>
                      <varlistentry>
                        <term>Datum:</term>
                        <listitem>Tisdag 15 april 2025</listitem>
                      </varlistentry>
                      <varlistentry>
                        <term>Tid:</term>
                        <listitem>Kl. 10:00</listitem>
                      </varlistentry>
                    </variablelist>
                  </section>
                  <section>
                    <title><emphasis role="information">Viktig information</emphasis></title>
                    <itemizedlist mark="bullet">
                      <listitem>Ta inte cellprov om du har mens.</listitem>
                      <listitem>Du behöver inte förbereda dig.</listitem>
                    </itemizedlist>
                  </section>
                  <section>
                    <title><emphasis role="observe">Om du inte kan komma</emphasis></title>
                    <para>Kontakta mottagningen senast 24 timmar innan. Ring
                      <ulink url="tel:08-123456">08-123 456</ulink>.
                    </para>
                  </section>
                </article>
                """;

        @Test
        void kallelsebrev_producerar_validt_xhtml_fragment() {
            String result = t.transform(KALLELSE_XML);
            assertTrue(result.startsWith("<div xmlns=\"http://www.w3.org/1999/xhtml\">"));
            assertTrue(result.endsWith("</div>"));
        }

        @Test
        void kallelsebrev_innehaller_alla_nyckelelement() {
            String result = t.transform(KALLELSE_XML);
            assertTrue(result.contains("<h2>Kallelse till cellprovtagning</h2>"));
            assertTrue(result.contains("<dt>Datum:</dt>"));
            assertTrue(result.contains("<dd>Tisdag 15 april 2025</dd>"));
            assertTrue(result.contains("<div class=\"info-box\">"));
            assertTrue(result.contains("<div class=\"warning-box\">"));
            assertTrue(result.contains("<a href=\"tel:08-123456\">08-123 456</a>"));
        }

        @Test
        void ogiltigt_xml_ger_tomt_div_utan_undantag() {
            assertDoesNotThrow(() -> {
                String result = t.transform("<article><ej-stängt>");
                assertEquals("<div xmlns=\"http://www.w3.org/1999/xhtml\"></div>", result);
            });
        }
    }
}
