package tests.parseur.Test;

import java.lexeur.Jeton;
import java.lexeur.Lexeur;
import java.parseur.AnaSynt;
import java.parseur.ast.*;

import java.io.FileWriter;
import java.io.IOException;
import java.semantic.AnalyseSemantique;
import java.util.List;
import utils.diag.*;
import utils.diag.DiagnosticCollector;
import utils.diag.Position;

import static java.lexeur.TypeJeton.Fonction;

public class TestGeneration {

    public static void main(String[] args) {

        String source = """
                fonction main() {
                  // commentaire
                  s = "hello";
                  c = 'A';
                  retourne 0;
                }
                """;

        DiagnosticCollector collecteur = new DiagnosticCollector();
        Lexeur lexeur = new Lexeur(source, collecteur);
        List<Jeton> jetons = lexeur.analyser();

        System.out.println("===== JETONS PRODUITS PAR LE LEXEUR =====");
        for (Jeton j : jetons) {
            System.out.println(j.getType() + " -> '" + j.getValeur() + "' (ligne " + j.getLigne() + ", col " + j.getColonne() + ")");
        }

        var prog = AnaSynt.analyser(source, collecteur);
        var sem  = new AnalyseSemantique(collecteur);
        sem.verifier(prog);
        System.out.println(prog.genJava(sem));

        // ✅ Position neutre pour les tests
        Position pos = new Position("test", 1, 1);


        Fonction somme = new Fonction(
                pos, "somme", List.of("a", "b"),
                new Bloc(pos, List.of(new Retourne(pos, new ExpressionBinaire(pos, new Identifiant(pos, "a"),
                                "+", new Identifiant(pos, "b"))))
                )
        );


        Classe calcul = new Classe(
                pos,
                "Calcul",
                List.of("Utilitaire"),
                List.of("age", "nom"),
                List.of("id"),
                List.of("PI"),
                List.of(somme)
        );

        String codeJava = calcul.genJava(sem);

        try (FileWriter writer = new FileWriter("Calcul.java")) {
            writer.write(codeJava);
            System.out.println("✅ Fichier Calcul.java généré avec succès !");
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'écriture du fichier : " + e.getMessage());
        }
    }
}
