package Tests;

import org.junit.jupiter.api.Test;
//include <assert.h>
import parseur.AnaSynt;
import parseur.ast.Programme;
import semantic.AnalyseSemantique;
import semantic.ErreurSemantique;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de l'analyse sémantique : vérifie que certaines erreurs
 * déclenchent bien une ErreurSemantique avec un message approprié.
 */
public class SemantiqueTest {

    @Test
    void testAffectationVide() {
        String source = """
            fonction main() {
              x = vide();
            }
            """;
        Programme programme = AnaSynt.analyser(source);
        AnalyseSemantique sem = new AnalyseSemantique();
        assertThrows(ErreurSemantique.class,
                () -> sem.verifier(programme),
                "Affecter une valeur VIDE devrait déclencher une ErreurSemantique");
    }

    @Test
    void testRetourVideNonVoid() {
        String source = """
            fonction main() {
              retourne vide();
            }
            """;
        Programme programme = AnaSynt.analyser(source);
        AnalyseSemantique sem = new AnalyseSemantique();
        assertThrows(ErreurSemantique.class,
                () -> sem.verifier(programme),
                "Retourner vide() dans main() doit être interdit");
    }

    @Test
    void testArityMismatch() {
        String source = """
            fonction f(a) {
              retourne a + 1;
            }
            fonction main() {
              x = f(1, 2); // mauvaise arité
              retourne x;
            }
            """;
        Programme programme = AnaSynt.analyser(source);
        AnalyseSemantique sem = new AnalyseSemantique();
        assertThrows(ErreurSemantique.class,
                () -> sem.verifier(programme),
                "Une mauvaise arité doit provoquer une ErreurSemantique");
    }
}
