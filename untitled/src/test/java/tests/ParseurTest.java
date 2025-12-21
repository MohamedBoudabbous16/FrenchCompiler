package test.java.tests;
import org.junit.jupiter.api.Test;
import main.java.parseur.AnaSynt;
import main.java.parseur.ast.Programme;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du main.java.parseur : s'assure qu'un petit programme se parse
 * et que les nœuds AST retournés sont de la bonne classe.
 */
public class ParseurTest {

    @Test
    void parser_sanity() {
        Object programme = TestTools.parseProgramme("""
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
        Programme programme = AnaSynt.analyser(source);
        assertNotNull(programme);
        // On doit obtenir une seule classe "ProgrammePrincipal" avec 2 fonctions
        assertEquals(1, programme.getClasses().size());
        assertEquals("ProgrammePrincipal",
                programme.getClasses().get(0).getNom());
        assertEquals(2,
                programme.getClasses().get(0).getFonctions().size());
    }
}
