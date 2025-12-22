package test.java.tests.utils.text;



import org.junit.jupiter.api.Test;
import utils.text.IdentWriter;
import utils.text.StringEspaces;

import static org.junit.jupiter.api.Assertions.*;

class StringEspacesIdentWriterTest {

    @Test
    void repeter_et_indent() {
        assertEquals("", StringEspaces.repeter('x', 0));
        assertEquals("xxx", StringEspaces.repeter('x', 3));

        assertEquals("abab", StringEspaces.repeter("ab", 2));
        assertEquals("", StringEspaces.repeter((String) null, 3));

        assertEquals("    ", StringEspaces.espaces(4));
        assertEquals("    ", StringEspaces.indent(2, 2));
        assertEquals("  ", StringEspaces.indent(1));
    }

    @Test
    void espacesPourCaret_avecTab() {
        // "\t" compte 4 colonnes visuelles (par défaut)
        String line = "a\tb";
        // colonne 3 => après "a" + une tab (visuellement ça saute)
        String spaces = StringEspaces.espacesPourCaret(line, 3);
        assertNotNull(spaces);
        assertTrue(spaces.length() >= 2); // robuste, dépend du calcul visuel
    }

    @Test
    void identWriter_indentations() {
        IdentWriter w = new IdentWriter("  ");
        w.ligne("a")
                .indent()
                .ligne("b")
                .dedent()
                .ligne("c");

        String s = w.toString();
        assertTrue(s.contains("a\n"));
        assertTrue(s.contains("  b\n"));
        assertTrue(s.contains("c\n"));
    }

    @Test
    void identWriter_blocAccolades() {
        IdentWriter w = new IdentWriter("  ");
        w.ligne("class X")
                .blocAccolades(ww -> ww.ligne("int a;"));

        String s = w.toString();
        assertTrue(s.contains("{"));
        assertTrue(s.contains("  int a;"));
        assertTrue(s.contains("}"));
    }
}
