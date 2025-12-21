import parseur.AnaSynt;
import parseur.ast.Programme;
import semantic.AnalyseSemantique;
import semantic.ErreurSemantique;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    // =========================
    // TEST AFFICHE OK
    // =========================
    public static void mainAfficheTest() {
        String source = """
            fonction main() {
              x = 1;
              affiche(x);
              affiche("hello");
              affiche('A');
              affiche(x + 1);
              retourne 0;
            }
            """;

        lancerTest("TEST AFFICHE OK", source);
    }

    // =========================
    // TEST AFFICHE KO (VIDE)
    // =========================
    public static void mainAfficheVideTest() {
        String source = """
            fonction vide() {
            }

            fonction main() {
              affiche(vide());
              retourne 0;
            }
            """;

        lancerTest("TEST AFFICHE VIDE (ERREUR ATTENDUE)", source);
    }

    // =========================
    // TEST CONCAT OK
    // =========================
    public static void mainConcatOKTest() {
        String source = """
            fonction main() {
              x = 3;
              s = "valeur = " + x;
              retourne s;
            }
            """;

        lancerTest("TEST CONCAT OK", source);
    }

    // =========================
    // TEST CONCAT KO
    // =========================
    public static void mainConcatKOTest() {
        String source = """
            fonction main() {
              x = 3;
              s = x + "test";
              retourne s;
            }
            """;

        lancerTest("TEST CONCAT KO (ERREUR ATTENDUE)", source);
    }

    // =========================
    // MÉTHODE COMMUNE DE TEST
    // =========================
    private static void lancerTest(String nom, String source) {
        System.out.println("\n===== " + nom + " =====");

        try {
            Programme programme = AnaSynt.analyser(source);
            AnalyseSemantique sem = new AnalyseSemantique();
            sem.verifier(programme);

            System.out.println(programme.genJava(sem));
            System.out.println("✅ Test réussi");

        } catch (ErreurSemantique e) {
            System.err.println("❌ Erreur sémantique : " + e.getMessage());
        }
    }

    // =========================
    // MAIN PRINCIPAL
    // =========================
    public static void main(String[] args) {

        // 🔹 Modes de test dédiés
        if (args.length == 1) {
            switch (args[0]) {
                case "--test-affiche" -> {
                    mainAfficheTest();
                    return;
                }
                case "--test-affiche-vide" -> {
                    mainAfficheVideTest();
                    return;
                }
                case "--test-concat-ok" -> {
                    mainConcatOKTest();
                    return;
                }
                case "--test-concat-ko" -> {
                    mainConcatKOTest();
                    return;
                }
            }
        }

        // =========================
        // MODE NORMAL (fichier)
        // =========================
        try {
            String source;

            if (args.length >= 1) {
                Path inputPath = Path.of(args[0]);
                source = Files.readString(inputPath, StandardCharsets.UTF_8);
            } else {
                source = """
                        fonction main() {
                            x = 0;
                            tantque (x < 5) {
                                x = x + 1;
                            }
                            retourne x;
                        }
                        """;

                System.out.println(
                        "Aucun fichier fourni -> utilisation d'un exemple interne.\n" +
                                "Usage:\n" +
                                "  java Main fichier.txt\n" +
                                "  java Main --test-affiche\n" +
                                "  java Main --test-affiche-vide\n" +
                                "  java Main --test-concat-ok\n" +
                                "  java Main --test-concat-ko\n"
                );
            }

            Programme programme = AnaSynt.analyser(source);
            AnalyseSemantique sem = new AnalyseSemantique();
            sem.verifier(programme);

            String javaCode = programme.genJava(sem);

            System.out.println("===== JAVA GÉNÉRÉ =====");
            System.out.println(javaCode);

            Path outputPath = Path.of("ProgrammePrincipal.java");
            Files.writeString(outputPath, javaCode, StandardCharsets.UTF_8);

            System.out.println("\n✅ Fichier Java généré : " + outputPath.toAbsolutePath());

        } catch (ErreurSemantique e) {
            System.err.println("❌ Erreur sémantique : " + e.getMessage());
        } catch (IOException e) {
            System.err.println("❌ Erreur E/S : " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("❌ Erreur compilation : " + e.getMessage());
        }
    }
}
