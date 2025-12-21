package test.java.tests.OptimzerCodeGenerator;

import main.java.semantic.AnalyseSemantique;
import org.junit.jupiter.api.Test;
import test.java.tests.TestTools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DiagnosticCodegenTests2 {

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
    void diag_lire_scanner_ou_runtime_ou_ast() {
        Object programme = TestTools.parseProgramme(SRC_LIRE);
        assertNotNull(programme);

        int nbLire = countNodes(programme, "main.java.parseur.ast.Lire");
        int nbAffiche = countNodes(programme, "main.java.parseur.ast.Affiche");

        AnalyseSemantique sem = new AnalyseSemantique();
        sem.verifier((main.java.parseur.ast.Programme) programme);

        String astJava = ((main.java.parseur.ast.Programme) programme).genJava(sem);

        Object gen = TestTools.newInstance(TestTools.mustClass("main.java.codegenerator.JavaGenerator"));
        String genJava = extractGeneratedJava(gen, programme, sem);

        String report =
                "\n===== DIAG LIRE =====\n" +
                        "AST: nb Lire = " + nbLire + ", nb Affiche = " + nbAffiche + "\n" +
                        "AST gen contains 'Scanner'? " + containsScanner(astJava) + "\n" +
                        "GEN gen contains 'Scanner'? " + containsScanner(genJava) + "\n" +
                        "AST gen head:\n" + head(astJava, 350) + "\n" +
                        "GEN gen head:\n" + head(genJava, 350) + "\n";

        System.out.println(report);

        assertTrue(nbLire > 0, "DIAG: aucun noeud Lire trouvé dans l’AST. " + report);

        // Si ton Lire.genJava contient Scanner (ce qui est ton cas), astJava devrait contenir Scanner
        // Si ce n'est pas le cas -> Lire.genJava utilisé n'est pas celui que tu crois (vieux bytecode / mauvaise classe).
        assertTrue(containsScanner(astJava),
                "DIAG: Programme.genJava(sem) ne contient pas Scanner alors qu'il y a Lire dans l'AST. "
                        + "=> suspect: Lire.genJava différent, ou classe Lire dupliquée/ancienne. " + report);

        // Si astJava a Scanner mais genJava non -> JavaGenerator renvoie autre chose / mauvais extracteur / vieux build
        if (containsScanner(astJava) && !containsScanner(genJava)) {
            fail("DIAG: Programme.genJava(sem) contient Scanner mais le code final NON. "
                    + "=> suspect: JavaGenerator renvoie un autre code, ou tu exécutes une ancienne version. " + report);
        }
    }

    @Test
    void diag_affiche_multi_args_ast_vs_gen() {
        Object programme = TestTools.parseProgramme(SRC_AFFICHE_MULTI);
        assertNotNull(programme);

        AnalyseSemantique sem = new AnalyseSemantique();
        sem.verifier((main.java.parseur.ast.Programme) programme);

        List<Object> afficheNodes = collectNodes(programme, "main.java.parseur.ast.Affiche");
        int nbAffiche = afficheNodes.size();
        int maxArgs = 0;
        for (Object a : afficheNodes) maxArgs = Math.max(maxArgs, readAfficheArgsSize(a));

        String astJava = ((main.java.parseur.ast.Programme) programme).genJava(sem);
        int astPrintCount = countPrint(astJava);

        Object gen = TestTools.newInstance(TestTools.mustClass("main.java.codegenerator.JavaGenerator"));
        String genJava = extractGeneratedJava(gen, programme, sem);
        int genPrintCount = countPrint(genJava);

        String report =
                "\n===== DIAG AFFICHE MULTI =====\n" +
                        "AST: nb Affiche = " + nbAffiche + ", max args = " + maxArgs + "\n" +
                        "AST print count = " + astPrintCount + "\n" +
                        "GEN print count = " + genPrintCount + "\n" +
                        "AST gen head:\n" + head(astJava, 350) + "\n" +
                        "GEN gen head:\n" + head(genJava, 350) + "\n";

        System.out.println(report);

        assertTrue(nbAffiche > 0, "DIAG: aucun noeud Affiche dans l’AST. " + report);
        assertTrue(maxArgs >= 2, "DIAG: Affiche présent mais pas multi-args (maxArgs=" + maxArgs + "). " + report);

        // Si ton Affiche.genJava est celle que tu as montrée, astJava devrait déjà avoir >=2 print
        assertTrue(astPrintCount >= 2,
                "DIAG: Programme.genJava(sem) n’a pas plusieurs System.out.print(...) "
                        + "=> suspect: ancienne classe Affiche, ou Affiche.genJava différent. " + report);

        if (astPrintCount >= 2 && genPrintCount < 2) {
            fail("DIAG: AST génère plusieurs prints mais JavaGenerator final non. "
                    + "=> suspect: code final différent / vieux build / mauvais extracteur. " + report);
        }
    }

    // ----------------------------
    // Extraction robuste du Java généré
    // ----------------------------
    private static String extractGeneratedJava(Object gen, Object programme, AnalyseSemantique sem) {

        // 1) si une méthode publique retourne String, on utilise la logique existante
        try {
            return (String) TestTools.invokeBestMethodReturningString(gen, programme, sem);
        } catch (AssertionError ignored) {}
        try {
            return (String) TestTools.invokeBestMethodReturningString(gen, programme);
        } catch (AssertionError ignored) {}

        // 2) sinon, on appelle generate(...) et on extrait un String du résultat (GenerationResult)
        Object res = invokeGenerate(gen, programme);

        return extractStringFromResult(res);
    }

    private static Object invokeGenerate(Object gen, Object programme) {
        try {
            Method m = gen.getClass().getMethod("generate", main.java.parseur.ast.Programme.class);
            return m.invoke(gen, programme);
        } catch (Exception e) {
            throw new AssertionError("DIAG: impossible d'appeler JavaGenerator.generate(Programme). " + e);
        }
    }

    private static String extractStringFromResult(Object res) {
        if (res == null) throw new AssertionError("DIAG: generate(...) a retourné null");
        if (res instanceof String s) return s;

        // A) cherche un getter no-arg qui retourne String
        for (Method m : res.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != String.class) continue;
            String name = m.getName().toLowerCase();
            // préférences: source/code/java
            if (!(name.contains("source") || name.contains("code") || name.contains("java"))) continue;
            try {
                Object v = m.invoke(res);
                if (v instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }

        // B) sinon: n'importe quel no-arg String (même sans "source" dans le nom)
        for (Method m : res.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != String.class) continue;
            try {
                Object v = m.invoke(res);
                if (v instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }

        // C) sinon: champs String
        for (Field f : res.getClass().getDeclaredFields()) {
            if (f.getType() != String.class) continue;
            f.setAccessible(true);
            try {
                Object v = f.get(res);
                if (v instanceof String s && !s.isBlank()) return s;
            } catch (Exception ignored) {}
        }

        // D) dernier recours: toString (mais on te le dit clairement)
        String ts = res.toString();
        if (ts != null && ts.contains("class") == false && ts.length() > 10) {
            return ts;
        }

        throw new AssertionError(
                "DIAG: impossible d'extraire le code Java depuis " + res.getClass().getName() +
                        ". Lance GenerationResultIntrospectionTests pour voir les getters/champs disponibles."
        );
    }

    // ----------------------------
    // Helpers AST (réflexion)
    // ----------------------------
    private static boolean containsScanner(String s) {
        if (s == null) return false;
        return s.contains("Scanner") || s.contains("java.util.Scanner");
    }

    private static int countPrint(String code) {
        if (code == null) return 0;
        return code.split("System\\.out\\.print\\(").length - 1;
    }

    private static String head(String s, int n) {
        if (s == null) return "null";
        s = s.replace("\r", "");
        return s.length() <= n ? s : s.substring(0, n) + "\n...[truncated]...";
    }

    private static int readAfficheArgsSize(Object afficheNode) {
        if (afficheNode == null) return 0;
        try {
            Method m = afficheNode.getClass().getMethod("getExpressions");
            Object list = m.invoke(afficheNode);
            if (list instanceof List<?> l) return l.size();
        } catch (Exception ignored) {}
        return 0;
    }

    private static int countNodes(Object root, String fqcn) {
        return collectNodes(root, fqcn).size();
    }

    private static List<Object> collectNodes(Object root, String targetFqcn) {
        if (root == null) return List.of();

        List<Object> out = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Object> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Object cur = stack.pop();
            if (cur == null) continue;

            int id = System.identityHashCode(cur);
            if (!visited.add(id)) continue;

            if (cur.getClass().getName().equals(targetFqcn)) out.add(cur);

            if (cur instanceof List<?> l) {
                for (Object e : l) stack.push(e);
                continue;
            }

            String pkg = cur.getClass().getPackageName();
            if (!pkg.startsWith("main.java.parseur.ast")) continue;

            for (Field f : cur.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object v = f.get(cur);
                    if (v == null) continue;

                    if (v instanceof List<?> l2) for (Object e : l2) stack.push(e);
                    else stack.push(v);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }
}
