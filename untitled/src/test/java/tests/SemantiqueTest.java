package tests;

import org.junit.jupiter.api.Test;
//include <assert.h>
import java.parseur.AnaSynt;
import java.parseur.ast.Programme;
import java.semantic.AnalyseSemantique;
import java.semantic.ErreurSemantique;
import utils.diag.DiagnosticCollector;

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
        Programme programme = AnaSynt.analyser(source, new DiagnosticCollector());
        AnalyseSemantique sem = new AnalyseSemantique(new DiagnosticCollector());
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
        Programme programme = AnaSynt.analyser(source, new DiagnosticCollector());
        AnalyseSemantique sem = new AnalyseSemantique(new DiagnosticCollector());
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
        Programme programme = AnaSynt.analyser(source, new DiagnosticCollector());
        AnalyseSemantique sem = new AnalyseSemantique(new DiagnosticCollector());
        assertThrows(ErreurSemantique.class,
                () -> sem.verifier(programme),
                "Une mauvaise arité doit provoquer une ErreurSemantique");
    }
}
