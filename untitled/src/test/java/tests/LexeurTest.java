package tests;

import java.lexeur.Jeton;
import java.lexeur.Lexeur;
import java.lexeur.TypeJeton;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LexeurTest {

    @Test
    void testCommentairesEtLitteraux() {
        String source = """
            fonction main() {
              // commentaire
              s = "hello";
              c = 'A';
              retourne 0;
            }
            """;

        Lexeur lex = new Lexeur(source, new utils.diag.DiagnosticCollector());
        List<Jeton> jetons = lex.analyser();

        assertEquals(TypeJeton.Fonction, jetons.get(0).getType());
        assertEquals(TypeJeton.Identifiant, jetons.get(1).getType());
        assertEquals(TypeJeton.ParOuvr, jetons.get(2).getType());
        assertEquals(TypeJeton.ParFerm, jetons.get(3).getType());
        assertEquals(TypeJeton.AccoladeOuvr, jetons.get(4).getType());

        assertTrue(jetons.stream().anyMatch(j -> j.getType() == TypeJeton.TexteLitteral));
        assertTrue(jetons.stream().anyMatch(j -> j.getType() == TypeJeton.CaractereLitteral));

        assertEquals(TypeJeton.FinFichier, jetons.get(jetons.size() - 1).getType());
    }
}
