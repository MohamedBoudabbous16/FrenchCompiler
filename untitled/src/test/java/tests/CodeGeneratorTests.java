package test.java.tests;

import main.java.semantic.AnalyseSemantique;
import org.junit.jupiter.api.Test;

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
        assertNotNull(javaCode);

        assertTrue(javaCode.contains("Scanner") || javaCode.contains("java.util.Scanner"),
                "Le code généré devrait inclure Scanner pour lire()");

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
        assertNotNull(javaCode);

        int countPrint = javaCode.split("System\\.out\\.print\\(").length - 1;
        assertTrue(countPrint >= 2, "affiche multi-args doit générer plusieurs System.out.print(...)");

        TestTools.assertCompiles("ProgrammePrincipal", javaCode);
    }
}
