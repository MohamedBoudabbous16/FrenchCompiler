package test.java.tests;

import main.java.lexeur.Jeton;
import main.java.lexeur.Lexeur;
import main.java.lexeur.TypeJeton;
//include <assert.h>

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du main.java.lexeur : vérifie la gestion des commentaires,
 * des littéraux de chaîne et des littéraux de caractère.
 */
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
        Lexeur lex = new Lexeur(source);
        List<Jeton> jetons = lex.analyser();

        // Vérifier qu'on obtient les bons types de jetons dans l'ordre :
        assertEquals(TypeJeton.Fonction, jetons.get(0).getType());
        assertEquals(TypeJeton.Identifiant, jetons.get(1).getType()); // main
        assertEquals(TypeJeton.ParOuvr, jetons.get(2).getType());
        assertEquals(TypeJeton.ParFerm, jetons.get(3).getType());
        assertEquals(TypeJeton.AccoladeOuvr, jetons.get(4).getType());

        // Avancer jusqu'à la ligne des affectations
        // s = "hello";
        assertTrue(jetons.stream().anyMatch(j -> j.getType() == TypeJeton.TexteLitteral));
        assertTrue(jetons.stream().anyMatch(j -> j.getType() == TypeJeton.CaractereLitteral));

        // Fin de fichier
        assertEquals(TypeJeton.FinFichier,
                jetons.get(jetons.size() - 1).getType());
    }
}
