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
        void link_med_type_blank_ger_target_blank() {
            String result = t.transform(
                    "<article><para>"
                    + "<link url=\"https://www.1177.se/\" type=\"_blank\">1177.se</link>"
                    + "</para></article>");
            assertTrue(result.contains("<a href=\"https://www.1177.se/\" target=\"_blank\">1177.se</a>"));
        }

        @Test
        void link_utan_type_blank_ger_ingen_target() {
            String result = t.transform(
                    "<article><para>"
                    + "<link url=\"https://www.1177.se/\">1177.se</link>"
                    + "</para></article>");
            assertFalse(result.contains("target="));
        }

        @Test
        void link_med_linkend_ger_intern_ankar() {
            String result = t.transform(
                    "<article><para>"
                    + "<link linkend=\"avsnitt1\">Gå till avsnitt 1</link>"
                    + "</para></article>");
            assertTrue(result.contains("<a href=\"#avsnitt1\">Gå till avsnitt 1</a>"));
        }

        @Test
        void link_inuti_listitem_renderas_korrekt() {
            String result = t.transform(
                    "<article><itemizedlist mark=\"bullet\">"
                    + "<listitem>Läs mer: <link url=\"https://www.skanetrafiken.se/\" type=\"_blank\">länstrafiken.se</link></listitem>"
                    + "</itemizedlist></article>");
            assertTrue(result.contains("<li>Läs mer: <a href=\"https://www.skanetrafiken.se/\" target=\"_blank\">länstrafiken.se</a></li>"));
        }
    }

    // ── Iteration 7: bindestreckslista och tom ruta ───────────────────────────

    @Nested
    class AvanceradaElement {

        @Test
        void itemizedlist_mark_hyphen_ger_ul_med_css_klass() {
            String result = t.transform(
                    "<article><itemizedlist mark=\"hyphen\">"
                    + "<listitem>Ta med legitimation</listitem>"
                    + "</itemizedlist></article>");
            assertTrue(result.contains("<ul class=\"hyphen\">"));
            assertTrue(result.contains("<li>Ta med legitimation</li>"));
        }

        @Test
        void itemizedlist_mark_bullet_ger_ul_utan_klass() {
            String result = t.transform(
                    "<article><itemizedlist mark=\"bullet\">"
                    + "<listitem>Punkt</listitem>"
                    + "</itemizedlist></article>");
            assertTrue(result.contains("<ul>"));
            assertFalse(result.contains("class="));
        }

        @Test
        void tom_emphasis_i_title_ger_box_utan_rubrik() {
            String result = t.transform(
                    "<article><section>"
                    + "<title><emphasis role=\"frame\"/></title>"
                    + "<para>Hör av dig om du inte kan komma.</para>"
                    + "</section></article>");
            assertTrue(result.contains("<div class=\"framed-box\">"));
            assertFalse(result.contains("<h"), "tom ruta ska inte ha heading");
            assertTrue(result.contains("<p>Hör av dig om du inte kan komma.</p>"));
        }

        @Test
        void tom_information_emphasis_ger_info_box_utan_rubrik() {
            String result = t.transform(
                    "<article><section>"
                    + "<title><emphasis role=\"information\"/></title>"
                    + "<para>Viktig info.</para>"
                    + "</section></article>");
            assertTrue(result.contains("<div class=\"info-box\">"));
            assertTrue(result.contains("<p>Viktig info.</p>"));
        }
    }

    // ── Integrationsscenarion: verkliga exempel ur 1177 Inkorg-dokumentationen ──

    @Nested
    class Integrationsscenarion {

        // "Bokad tid med textrutor" – exakt ur Ineras DocBook-exempel PDF
        static final String BOKAD_TID_XML = """
                <?xml version="1.0"?>
                <article>
                  <section>
                    <title>Välkommen till Söderby Vårdcentral</title>
                    <para>Vi har bokat tid till dig hos Anton Andersson, läkare, för undersökning.</para>
                  </section>
                  <section>
                    <variablelist>
                      <varlistentry><term>Datum:</term><listitem>Tisdag 10 mars 2022</listitem></varlistentry>
                      <varlistentry><term>Klockan:</term><listitem>11.00</listitem></varlistentry>
                      <varlistentry><term>Plats:</term><listitem>Sandstigen 15, Söderby Våningsplan 5</listitem></varlistentry>
                    </variablelist>
                  </section>
                  <section>
                    <title><emphasis role="information">Viktigt inför ditt besök.</emphasis></title>
                    <itemizedlist mark="bullet">
                      <listitem>Covid-19: stannar hemma vid förkylning.</listitem>
                      <listitem>Utför inte kraftig fysisk aktivitet.</listitem>
                      <listitem><link url="https://www.dn.se/" type="_blank">Läs mer om undersökningen</link></listitem>
                    </itemizedlist>
                  </section>
                  <section>
                    <title><emphasis role="observe">Tänk på.</emphasis></title>
                    <itemizedlist mark="hyphen">
                      <listitem>Ta med legitimation.</listitem>
                      <listitem>Läs om resor: <link url="https://www.skanetrafiken.se/sjukresor" type="_blank">länstrafiken.se/sjukresor</link></listitem>
                      <listitem>Avboka senast 24 timmar före besöket.</listitem>
                    </itemizedlist>
                  </section>
                  <para><link url="https://www.dn.se/" type="_blank">Kontakta Söderby Vårdcentral</link></para>
                </article>
                """;

        @Test
        void bokad_tid_producerar_validt_xhtml_fragment() {
            String result = t.transform(BOKAD_TID_XML);
            assertTrue(result.startsWith("<div xmlns=\"http://www.w3.org/1999/xhtml\">"));
            assertTrue(result.endsWith("</div>"));
        }

        @Test
        void bokad_tid_innehaller_alla_nyckelelement() {
            String result = t.transform(BOKAD_TID_XML);
            assertTrue(result.contains("<h2>Välkommen till Söderby Vårdcentral</h2>"));
            assertTrue(result.contains("<dt>Datum:</dt>"));
            assertTrue(result.contains("<dd>Tisdag 10 mars 2022</dd>"));
            assertTrue(result.contains("<div class=\"info-box\">"));
            assertTrue(result.contains("<h2>Viktigt inför ditt besök.</h2>"));
            assertTrue(result.contains("<div class=\"warning-box\">"));
            assertTrue(result.contains("<h2>Tänk på.</h2>"));
            assertTrue(result.contains("<ul class=\"hyphen\">"));
            assertTrue(result.contains("<a href=\"https://www.dn.se/\" target=\"_blank\">Läs mer om undersökningen</a>"));
            assertTrue(result.contains("<a href=\"https://www.dn.se/\" target=\"_blank\">Kontakta Söderby Vårdcentral</a>"));
        }

        // Ruta utan rubrik – "Vit informationsruta utan rubrik" ur PDF
        @Test
        void ruta_utan_rubrik_renderas_utan_heading() {
            String result = t.transform("""
                    <?xml version="1.0"?>
                    <article><section>
                      <title><emphasis role="frame"/></title>
                      <para>Hör alltid av dig om du inte kan komma på utsatt tid.</para>
                    </section></article>""");
            assertTrue(result.contains("<div class=\"framed-box\">"));
            assertFalse(result.contains("<h"), "tom ruta ska inte ha heading");
            assertTrue(result.contains("<p>Hör alltid av dig"));
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
