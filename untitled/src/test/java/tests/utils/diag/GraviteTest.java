package tests.utils.diag;

import org.junit.jupiter.api.Test;
import utils.diag.Gravite;

import static org.junit.jupiter.api.Assertions.*;

class GraviteTest {

    @Test
    void niveauEtLibelle() {
        assertEquals(3, Gravite.ERREUR.niveau());
        assertEquals("ERREUR", Gravite.ERREUR.libelle());

        assertEquals(2, Gravite.AVERTISSEMENT.niveau());
        assertEquals(1, Gravite.INFO.niveau());
    }

    @Test
    void estErreur() {
        assertTrue(Gravite.ERREUR.estErreur());
        assertFalse(Gravite.AVERTISSEMENT.estErreur());
        assertFalse(Gravite.INFO.estErreur());
    }
}
