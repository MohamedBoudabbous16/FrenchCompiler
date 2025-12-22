package  test.java.tests.utils.diag;


import org.junit.jupiter.api.Test;
import utils.diag.Intervalle;
import utils.diag.Position;
import utils.diag.SourceTexte;

import static org.junit.jupiter.api.Assertions.*;

class PositionIntervalleSourceTexteTest {

    @Test
    void position_toString_et_compareTo() {
        Position p1 = new Position("f.txt", 2, 3);
        Position p2 = new Position("autre.txt", 2, 4);
        Position p3 = new Position(1, 10);

        assertEquals("f.txt:2:3", p1.toString());
        assertEquals("1:10", p3.toString());

        // compareTo ignore la source (seulement ligne/colonne)
        assertTrue(p1.compareTo(p2) < 0);
        assertTrue(p2.compareTo(p1) > 0);
    }

    @Test
    void intervalle_toString_memeSource_compact() {
        Position d = new Position("a", 1, 2);
        Position f = new Position("a", 1, 5);
        Intervalle itv = new Intervalle(d, f);

        assertEquals("a:1:2-1:5", itv.toString());
        assertTrue(itv.aUneFin());
    }

    @Test
    void intervalle_at_pointUnique() {
        Position p = new Position("x", 3, 7);
        Intervalle itv = Intervalle.at(p);

        assertEquals("x:3:7", itv.toString());
        assertFalse(itv.aUneFin());
        assertEquals(p, itv.debut());
        assertNull(itv.fin());
    }

    @Test
    void sourceTexte_decoupeEtAccesLignes() {
        SourceTexte src = new SourceTexte("main", "a\nb\r\nc\r\nd");

        assertEquals("main", src.nom());
        assertEquals(4, src.nbLignes());
        assertEquals("a", src.ligne(1));
        assertEquals("b", src.ligne(2));
        assertEquals("c", src.ligne(3));
        assertEquals("d", src.ligne(4));
        assertNull(src.ligne(0));
        assertNull(src.ligne(99));
    }
}
