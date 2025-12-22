package test.java.tests.utils.diag;



import org.junit.jupiter.api.Test;
import utils.diag.*;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticTest {

    @Test
    void builder_et_toBuilder_clonage() {
        Intervalle itv = Intervalle.at(new Position("f", 2, 3));

        Diagnostic d = Diagnostic.builder(Gravite.ERREUR, "msg")
                .code("LEX001")
                .intervalle(itv)
                .aide("corrige ça")
                .note("note1")
                .build();

        assertEquals(Gravite.ERREUR, d.gravite());
        assertEquals("LEX001", d.code());
        assertEquals("msg", d.message());
        assertEquals(itv, d.intervalle());
        assertEquals("corrige ça", d.aide());
        assertEquals(1, d.notes().size());

        Diagnostic d2 = d.toBuilder().code("LEX002").build();
        assertEquals("LEX002", d2.code());
        assertEquals(d.message(), d2.message());
        assertEquals(d.intervalle(), d2.intervalle());
    }

    @Test
    void formatSimple_contientInfosEssentielles() {
        Intervalle itv = Intervalle.at(new Position("f", 2, 3));
        Diagnostic d = Diagnostic.erreur("boom", itv).toBuilder().code("PAR001").build();

        String s = d.formatSimple();
        assertTrue(s.contains("ERREUR"));
        assertTrue(s.contains("PAR001"));
        assertTrue(s.contains("f:2:3"));
        assertTrue(s.contains("boom"));
    }

    @Test
    void formatAvecSource_afficheExtraitEtCaret() {
        SourceTexte src = new SourceTexte("main", "ligne1\nab\tcd\nligne3");
        // Position colonne 4 => au milieu de "ab\tcd"
        Diagnostic d = Diagnostic.erreur("oops", Intervalle.at(new Position(null, 2, 4)));

        String out = d.formatAvecSource(src);
        assertTrue(out.contains("ERREUR"));
        assertTrue(out.contains("main:2:4"));  // nom() préféré si dispo
        assertTrue(out.contains("2 | ab\tcd"));
        assertTrue(out.contains("^"));
    }
}
