package test.java.tests.OptimzerCodeGenerator;

import main.java.semantic.AnalyseSemantique;
import org.junit.jupiter.api.Test;
import test.java.tests.TestTools;

import static org.junit.jupiter.api.Assertions.*;

public class CodeGeneratorTests {

    private static final String SRC_LIRE = """
        fonction main() {
          x = lire();
          affiche("x=", x);
          retourne x;
        }
        """;

    private static final String SRC_AFFICHE_MULTI = """
        fonction main() {
          x = 3;
          y = 4;
          affiche("x=", x, " y=", y);
          retourne 0;
        }
        """;

    @Test
    void codegenerator_genere_scanner_pour_lire() {
        Object programme = TestTools.parseProgramme(SRC_LIRE);

        // (Optionnel) Sémantique : tu peux garder si ton generate() l'utilise déjà
        AnalyseSemantique sem = new AnalyseSemantique();
        sem.verifier((main.java.parseur.ast.Programme) programme);

        Object gen = TestTools.newInstance(TestTools.mustClass("main.java.codegenerator.JavaGenerator"));

        Object result;
        try {
            result = TestTools.invokeBestPublicMethod(gen, programme, sem);
        } catch (AssertionError e) {
            result = TestTools.invokeBestPublicMethod(gen, programme);
        }

        // ✅ IMPORTANT : GenerationResult expose getJavaSource() (pas getSource()).
        // TestTools.extractJavaSource(result) doit appeler getJavaSource() si présent.
        String javaCode = TestTools.extractJavaSource(result);
        assertNotNull(javaCode, "javaCode ne doit pas être null");

        assertTrue(
                javaCode.contains("import java.util.Scanner") || javaCode.contains("java.util.Scanner") || javaCode.contains("Scanner"),
                "Le code généré devrait inclure Scanner pour lire()\n\n" + javaCode
        );

        TestTools.assertCompiles("ProgrammePrincipal", javaCode);
    }

    @Test
    void codegenerator_affiche_multi_arguments_print_plusieurs_fois() {
        Object programme = TestTools.parseProgramme(SRC_AFFICHE_MULTI);

        AnalyseSemantique sem = new AnalyseSemantique();
        sem.verifier((main.java.parseur.ast.Programme) programme);

        Object gen = TestTools.newInstance(TestTools.mustClass("main.java.codegenerator.JavaGenerator"));

        Object result;
        try {
            result = TestTools.invokeBestPublicMethod(gen, programme, sem);
        } catch (AssertionError e) {
            result = TestTools.invokeBestPublicMethod(gen, programme);
        }

        String javaCode = TestTools.extractJavaSource(result);
        assertNotNull(javaCode, "javaCode ne doit pas être null");

        // ✅ Multi-args => au moins 4 prints attendus pour ("x=", x, " y=", y)
        int countPrint = countOccurrences(javaCode, "System.out.print(");
        assertTrue(
                countPrint >= 4,
                "affiche multi-args doit générer plusieurs System.out.print(...). Trouvé=" + countPrint + "\n\n" + javaCode
        );

        TestTools.assertCompiles("ProgrammePrincipal", javaCode);
    }

    private static int countOccurrences(String haystack, String needle) {
        int c = 0, idx = 0;
        while (true) {
            idx = haystack.indexOf(needle, idx);
            if (idx < 0) return c;
            c++;
            idx += needle.length();
        }
    }
}
