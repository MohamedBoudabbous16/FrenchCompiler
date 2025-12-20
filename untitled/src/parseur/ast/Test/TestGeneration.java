package parseur.ast.Test;

import lexeur.Jeton;
import lexeur.Lexeur;
import parseur.AnaSynt;
import parseur.ast.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TestGeneration {

    public static void main(String[] args) {

        // =========================
        // TEST LEXEUR (minimal)
        // =========================


        String source = """
                fonction main() {
                  // commentaire
                  s = "hello";
                  c = 'A';
                  retourne 0;
                }
                """;

        Lexeur lexeur = new Lexeur(source);
        List<Jeton> jetons = lexeur.analyser();

        System.out.println("===== JETONS PRODUITS PAR LE LEXEUR =====");
        for (Jeton j : jetons) {
            System.out.println(j.getType() + " -> '" + j.getValeur() + "' (ligne " + j.getLigne() + ", col " + j.getColonne() + ")");
        }
        var prog = AnaSynt.analyser(source);
        var sem  = new semantic.AnalyseSemantique();
        sem.verifier(prog);   // ⬅️ LIGNE MANQUANTE
        System.out.println(prog.genJava(sem));


        // =========================
        // TON TEST AST (existant)
        // =========================
        Fonction somme = new Fonction(
                "somme",
                List.of("a", "b"),
                new Bloc(List.of(
                        new Retourne(
                                new ExpressionBinaire(
                                        new Identifiant("a"),
                                        "+",
                                        new Identifiant("b")
                                )
                        )
                ))
        );

        Classe calcul = new Classe(
                "Calcul",
                List.of("Utilitaire"),
                List.of("age", "nom"),
                List.of("id"),
                List.of("PI"),
                List.of(somme)
        );

        String codeJava = calcul.genJava();

        // === Écriture dans un fichier Calcul.java ===
        try (FileWriter writer = new FileWriter("Calcul.java")) {
            writer.write(codeJava);
            System.out.println("✅ Fichier Calcul.java généré avec succès !");
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'écriture du fichier : " + e.getMessage());
        }
    }
}
