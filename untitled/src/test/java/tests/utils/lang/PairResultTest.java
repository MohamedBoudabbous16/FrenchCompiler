package tests.utils.lang;

import org.junit.jupiter.api.Test;
import utils.lang.Pair;
import utils.lang.Result;

import static org.junit.jupiter.api.Assertions.*;

class PairResultTest {

    @Test
    void pair_of_equals_hash() {
        Pair<Integer, String> p1 = Pair.of(1, "a");
        Pair<Integer, String> p2 = Pair.of(1, "a");
        Pair<Integer, String> p3 = Pair.of(2, "a");

        assertEquals(1, p1.premier());
        assertEquals("a", p1.second());
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
        assertTrue(p1.toString().contains("Pair"));
    }

    @Test
    void result_ok_err_et_accesseurs() {
        Result<Integer, String> ok = Result.ok(42);
        Result<Integer, String> err = Result.err("nope");

        assertTrue(ok.estSucces());
        assertFalse(ok.estEchec());
        assertEquals(42, ok.valeur());
        assertEquals(42, ok.orElse(0));

        assertFalse(err.estSucces());
        assertTrue(err.estEchec());
        assertEquals("nope", err.erreur());
        assertEquals(0, err.orElse(0));

        assertThrows(IllegalStateException.class, ok::erreur);
        assertThrows(IllegalStateException.class, err::valeur);
    }

    @Test
    void result_map_flatMap_mapErreur() {
        Result<Integer, String> ok = Result.ok(10);
        Result<Integer, String> err = Result.err("x");

        assertEquals(20, ok.map(v -> v * 2).valeur());
        assertTrue(err.map(v -> v * 2).estEchec());

        assertEquals("X", err.mapErreur(String::toUpperCase).erreur());

        Result<Integer, String> chained =
                ok.flatMap(v -> Result.ok(v + 1))
                        .flatMap(v -> Result.ok(v * 3));

        assertEquals(33, chained.valeur());
    }
}
