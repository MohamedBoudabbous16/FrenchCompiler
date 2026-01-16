package tests;
import org.junit.jupiter.api.Test;
import java.parseur.AnaSynt;
import java.parseur.ast.Programme;
import tests.TestTools;

import static org.junit.jupiter.api.Assertions.*;
import static tests.TestTools.parseProgramme;

/**
 * Tests unitaires du main.java.parseur : s'assure qu'un petit programme se parse
 * et que les nœuds AST retournés sont de la bonne classe.
 */
public class ParseurTest {

    @Test
    void parser_sanity() {
        Object programme = parseProgramme("""
        fonction main() {
          retourne 0;
        }
    """);
        assertNotNull(programme);
    }

    @Test
    void testProgrammeSimple() {
        String source = """
            fonction f(a, b) {
              retourne a + b;
            }
            fonction main() {
              x = 1;
              y = 2;
              z = f(x, y);
              retourne z;
            }
            """;
        Programme programme = AnaSynt.analyser(source, new utils.diag.DiagnosticCollector());
        assertNotNull(programme);
        // On doit obtenir une seule classe "ProgrammePrincipal" avec 2 fonctions
        assertEquals(1, programme.getClasses().size());
        assertEquals("ProgrammePrincipal",
                programme.getClasses().get(0).getNom());
        assertEquals(2,
                programme.getClasses().get(0).getFonctions().size());
    }
}
