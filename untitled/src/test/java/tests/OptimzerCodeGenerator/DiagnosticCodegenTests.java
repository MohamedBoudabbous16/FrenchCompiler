package test.java.tests.OptimzerCodeGenerator;

import main.java.semantic.AnalyseSemantique;
import org.junit.jupiter.api.Test;
import test.java.tests.TestTools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DiagnosticCodegenTests {

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

        // 1) Diagnostiquer l’AST: présence de Lire ?
        int nbLire = countNodes(programme, "main.java.parseur.ast.Lire");
        int nbAffiche = countNodes(programme, "main.java.parseur.ast.Affiche");

        // 2) Génération directe via l’AST
        AnalyseSemantique sem = new AnalyseSemantique();
        sem.verifier((main.java.parseur.ast.Programme) programme);
        String astJava = ((main.java.parseur.ast.Programme) programme).genJava(sem);

        // 3) Génération via JavaGenerator (même logique que tes tests)
        Object gen = TestTools.newInstance(TestTools.mustClass("main.java.codegenerator.JavaGenerator"));
        String genJava = invokeStringOrExtractFromGenerationResult(gen, programme, sem);

        // Résumé super lisible si ça fail
        String report =
                "\n===== DIAG LIRE =====\n" +
                        "AST: nb Lire = " + nbLire + ", nb Affiche = " + nbAffiche + "\n" +
                        "AST gen contains 'Scanner'? " + containsScanner(astJava) + "\n" +
                        "GEN gen contains 'Scanner'? " + containsScanner(genJava) + "\n" +
                        "AST gen first 400 chars:\n" + head(astJava, 400) + "\n" +
                        "GEN gen first 400 chars:\n" + head(genJava, 400) + "\n";

        // A) Si le parseur ne crée même pas Lire -> bug AnaSynt/analyserPrimaire ou Lexeur(TypeJeton.Lire)
        assertTrue(nbLire > 0, "DIAG: aucun noeud Lire trouvé dans l’AST. " + report);

        // B) Si AST gen contient déjà Scanner mais GEN non -> tu n’utilises pas le bon code final (mauvaise méthode / vieux .class)
        if (containsScanner(astJava) && !containsScanner(genJava)) {
            fail("DIAG: Programme.genJava(sem) contient Scanner mais le code final renvoyé par JavaGenerator NON. "
                    + "Ça pointe vers: mauvaise méthode invoquée, vieux bytecode, ou écrasement du source après gen. "
                    + report);
        }

        // C) Si ni AST gen ni GEN ne contiennent Scanner -> ton Lire.genJava ne met pas Scanner ET ton runtime n’est pas injecté
        assertTrue(containsScanner(genJava),
                "DIAG: le code final ne contient pas Scanner. Donc soit Lire.genJava ne l’émet pas, "
                        + "soit le runtime n’est pas injecté (programmeUsesLire() faux). " + report);
    }

    @Test
    void diag_affiche_multi_args_ast_vs_gen() {
        Object programme = TestTools.parseProgramme(SRC_AFFICHE_MULTI);
        assertNotNull(programme);

        AnalyseSemantique sem = new AnalyseSemantique();
        sem.verifier((main.java.parseur.ast.Programme) programme);

        // 1) Diagnostiquer AST: Affiche doit exister ET avoir plusieurs args
        List<Object> afficheNodes = collectNodes(programme, "main.java.parseur.ast.Affiche");
        int nbAffiche = afficheNodes.size();
        int maxArgs = 0;
        for (Object a : afficheNodes) {
            maxArgs = Math.max(maxArgs, readAfficheArgsSize(a));
        }

        // 2) Génération directe AST
        String astJava = ((main.java.parseur.ast.Programme) programme).genJava(sem);
        int astPrintCount = countPrint(astJava);

        // 3) Génération via JavaGenerator
        Object gen = TestTools.newInstance(TestTools.mustClass("main.java.codegenerator.JavaGenerator"));
        String genJava = invokeStringOrExtractFromGenerationResult(gen, programme, sem);
        int genPrintCount = countPrint(genJava);

        String report =
                "\n===== DIAG AFFICHE MULTI =====\n" +
                        "AST: nb Affiche = " + nbAffiche + ", max args dans un Affiche = " + maxArgs + "\n" +
                        "AST gen print count = " + astPrintCount + "\n" +
                        "GEN gen print count = " + genPrintCount + "\n" +
                        "AST gen first 400 chars:\n" + head(astJava, 400) + "\n" +
                        "GEN gen first 400 chars:\n" + head(genJava, 400) + "\n";

        // A) Si aucun Affiche -> parseur ne construit pas le bon nœud
        assertTrue(nbAffiche > 0, "DIAG: aucun noeud Affiche trouvé dans l’AST. " + report);

        // B) Si Affiche existe mais args=1 -> ton parser ne remplit pas la liste d’args (analyse virgules / Texte / Identifiant)
        assertTrue(maxArgs >= 2,
                "DIAG: Affiche présent mais pas multi-arguments (maxArgs=" + maxArgs + "). "
                        + "Donc le parser ne lit pas la liste complète. " + report);

        // C) Si AST gen a >=2 print mais GEN non -> problème d’utilisation du code final (méthode appelée / vieux build)
        if (astPrintCount >= 2 && genPrintCount < 2) {
            fail("DIAG: Programme.genJava(sem) génère bien plusieurs System.out.print(...) mais JavaGenerator renvoie un code qui ne les contient pas. "
                    + "Ça pointe vers: méthode différente, transformation après génération, ou vieux bytecode. " + report);
        }

        // D) Si GEN <2 print -> réellement mauvais
        assertTrue(genPrintCount >= 2, "DIAG: pas assez de System.out.print(...) dans le code final. " + report);
    }

    // ---------------------------
    // Helpers diagnostics
    // ---------------------------

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

    private static String invokeStringOrExtractFromGenerationResult(Object gen, Object programme, AnalyseSemantique sem) {
        // Essaie la même logique que tes tests : méthode String
        try {
            return (String) TestTools.invokeBestMethodReturningString(gen, programme, sem);
        } catch (AssertionError ignored) {
            // peut-être (gen, programme)
        }

        try {
            return (String) TestTools.invokeBestMethodReturningString(gen, programme);
        } catch (AssertionError ignored) {
            // sinon on tente GenerationResult => getSource()
        }

        // Fallback : appeler generate(...) puis getSource()
        try {
            Method m = gen.getClass().getMethod("generate", main.java.parseur.ast.Programme.class);
            Object res = m.invoke(gen, programme);
            // GenerationResult.getSource()
            Method getSource = res.getClass().getMethod("getSource");
            Object s = getSource.invoke(res);
            return (String) s;
        } catch (Exception e) {
            throw new AssertionError("DIAG: impossible d'obtenir le code Java depuis JavaGenerator. " + e);
        }
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

            if (cur.getClass().getName().equals(targetFqcn)) {
                out.add(cur);
            }

            // List -> push elements
            if (cur instanceof List<?> l) {
                for (Object e : l) stack.push(e);
                continue;
            }

            // Traverser les champs des classes AST (réflexion)
            String pkg = cur.getClass().getPackageName();
            if (!pkg.startsWith("main.java.parseur.ast")) continue;

            for (Field f : cur.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object v = f.get(cur);
                    if (v == null) continue;

                    if (v instanceof List<?> l2) {
                        for (Object e : l2) stack.push(e);
                    } else {
                        stack.push(v);
                    }
                } catch (Exception ignored) {}
            }
        }
        return out;
    }
}
