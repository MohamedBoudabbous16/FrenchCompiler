import parseur.AnaSynt;
import parseur.ast.Programme;
import semantic.AnalyseSemantique;
import semantic.ErreurSemantique;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        try {
            // 1) Lire le programme source (fichier ou string)
            String source;
            if (args.length >= 1) {
                Path inputPath = Path.of(args[0]);
                source = Files.readString(inputPath, StandardCharsets.UTF_8);
            } else {
                // Exemple par défaut (à adapter)
                source = """
                        fonction main() {
                            x = 0;
                            tantque (x < 5) {
                                x = x + 1;
                            }
                            retourne x;
                        }
                        """;
                System.out.println("Aucun fichier fourni -> utilisation d'un exemple interne.\n" +
                        "Usage: java Main <source.txt> [Sortie.java]\n");
            }

            // 2) Parser -> AST
            Programme programme = AnaSynt.analyser(source);

            // 3) Analyse sémantique (NOUVEAU)
            AnalyseSemantique sem = new AnalyseSemantique();
            sem.verifier(programme);

            // 4) Générer le code Java (seulement si sémantique OK)
            String javaCode = programme.genJava(sem);

            // 5) Afficher le Java généré
            System.out.println("===== JAVA GÉNÉRÉ =====");
            System.out.println(javaCode);

            // 6) Écrire dans un fichier .java
            String outputFileName = (args.length >= 2) ? args[1] : "ProgrammePrincipal.java";
            Path outputPath = Path.of(outputFileName);

            Files.writeString(outputPath, javaCode, StandardCharsets.UTF_8);

            System.out.println("\n✅ Fichier Java généré : " + outputPath.toAbsolutePath());

        } catch (ErreurSemantique e) {
            // Erreurs sémantiques (variables non déclarées, types incompatibles, etc.)
            System.err.println("❌ Erreur sémantique : " + e.getMessage());

        } catch (IOException e) {
            System.err.println("❌ Erreur E/S : " + e.getMessage());

        } catch (RuntimeException e) {
            // Erreurs syntaxiques (AnaSynt.erreur(...)) ou autres
            System.err.println("❌ Erreur compilation : " + e.getMessage());
        }
    }
}
