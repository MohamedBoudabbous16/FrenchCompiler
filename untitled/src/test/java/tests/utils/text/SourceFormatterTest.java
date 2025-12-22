package test.java.tests.utils.text;



import org.junit.jupiter.api.Test;
import utils.diag.Intervalle;
import utils.diag.Position;
import utils.diag.SourceTexte;
import utils.text.SourceFormatter;

import static org.junit.jupiter.api.Assertions.*;

class SourceFormatterTest {

    @Test
    void formater_intervalle_point() {
        SourceTexte src = new SourceTexte("file", "aaa\nbb\tb\nccc");
        Intervalle itv = Intervalle.at(new Position(null, 2, 3));

        String out = SourceFormatter.formater(src, itv);

        assertTrue(out.contains("2 | bb\tb"));
        assertTrue(out.contains("^"));
    }
}
