package tests.utils.text;



import org.junit.jupiter.api.Test;
import utils.text.StringEscape;

import static org.junit.jupiter.api.Assertions.*;

class StringEscapeTest {

    @Test
    void echappement_java_string() {
        String in = "Bonjour \"toi\"\n\\";
        String out = StringEscape.echapperPourJavaString(in);

        assertTrue(out.contains("\\\""));
        assertTrue(out.contains("\\n"));
        assertTrue(out.contains("\\\\"));
    }

    @Test
    void literal_java_string_et_char() {
        assertEquals("\"a\\n\"", StringEscape.literalJavaString("a\n"));
        assertEquals("'\\n'", StringEscape.literalJavaChar('\n'));
        assertEquals("'\\''", StringEscape.literalJavaChar('\''));
    }

    @Test
    void desechapper_basique() {
        assertEquals("a\nb", StringEscape.desechapper("a\\nb"));
        assertEquals("tab\tX", StringEscape.desechapper("tab\\tX"));
        assertEquals("\\", StringEscape.desechapper("\\\\"));
    }
}
