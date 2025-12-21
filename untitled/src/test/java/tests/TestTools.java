package test.java.tests;

import org.junit.jupiter.api.Assertions;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;


/**
 * Outils robustes par réflexion (parser + generator) + compilation Java.
 */
public final class TestTools {

    private TestTools() {}

    // ---------- Reflection helpers ----------
    public static Class<?> mustClass(String fqcn) {
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException e) {
            Assertions.fail("Classe introuvable: " + fqcn + " (vérifie tes packages/noms après refactoring)");
            return null;
        }
    }


        /**
         * Invoque "la meilleure" méthode publique dont les paramètres matchent args,
         * peu importe le type de retour.
         * Stratégie :
         * - prend les méthodes publiques
         * - filtre celles dont nb params == args.length et types assignables
         * - préfère une méthode nommée generate / gen / compile si dispo
         * - sinon prend la première candidate
         */
        public static Object invokeBestPublicMethod(Object target, Object... args) {
            Objects.requireNonNull(target, "target");

            Method best = findBestPublicMethod(target.getClass(), args);
            Assertions.assertNotNull(best,
                    "Aucune méthode publique compatible args=" + Arrays.toString(args)
                            + " dans " + target.getClass().getName());

            try {
                best.setAccessible(true);
                return best.invoke(target, args);
            } catch (Exception e) {
                throw new RuntimeException("Erreur invocation " + best.getName() + " : " + e.getMessage(), e);
            }
        }

        private static Method findBestPublicMethod(Class<?> clazz, Object[] args) {
            List<Method> candidates = new ArrayList<>();
            for (Method m : clazz.getMethods()) { // public incl. héritées
                if (!Modifier.isPublic(m.getModifiers())) continue;
                if (m.getParameterCount() != args.length) continue;
                if (!paramsMatch(m.getParameterTypes(), args)) continue;
                candidates.add(m);
            }
            if (candidates.isEmpty()) return null;

            // Score : préférer des noms attendus
            candidates.sort(Comparator.comparingInt((Method m) -> scoreMethodName(m.getName())).reversed());
            return candidates.get(0);
        }

        private static boolean paramsMatch(Class<?>[] paramTypes, Object[] args) {
            for (int i = 0; i < paramTypes.length; i++) {
                Object a = args[i];
                if (a == null) return false;
                Class<?> argType = a.getClass();

                // gérer primitives vs wrappers
                Class<?> p = wrapPrimitive(paramTypes[i]);
                if (!p.isAssignableFrom(argType)) return false;
            }
            return true;
        }

        private static int scoreMethodName(String name) {
            String n = name.toLowerCase(Locale.ROOT);
            if (n.equals("generate")) return 100;
            if (n.equals("genjava")) return 95;
            if (n.equals("compile")) return 90;
            if (n.startsWith("generate")) return 80;
            if (n.startsWith("gen")) return 70;
            return 10;
        }

        private static Class<?> wrapPrimitive(Class<?> c) {
            if (!c.isPrimitive()) return c;
            if (c == int.class) return Integer.class;
            if (c == boolean.class) return Boolean.class;
            if (c == long.class) return Long.class;
            if (c == double.class) return Double.class;
            if (c == float.class) return Float.class;
            if (c == char.class) return Character.class;
            if (c == byte.class) return Byte.class;
            if (c == short.class) return Short.class;
            return c;
        }

        /**
         * Extrait un code Java (String) depuis un résultat de génération.
         * - si result est String => retourne tel quel
         * - sinon, tente getSource(), source(), getJavaCode(), getCode()...
         * - sinon, prend la seule méthode publique no-arg retournant String
         */
        public static String extractJavaSource(Object result) {
            Assertions.assertNotNull(result, "Résultat null");

            if (result instanceof String s) {
                return s;
            }

            // Essayer getters standards
            String[] preferred = {
                    "getSource", "source",
                    "getJavaCode", "javaCode",
                    "getCode", "code",
                    "getText", "text",
                    "toString" // dernier recours (mais souvent moche)
            };

            for (String methodName : preferred) {
                String s = tryNoArgStringMethod(result, methodName);
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }

            // Sinon: si une seule méthode publique no-arg retourne String, on la prend
            List<Method> stringNoArg = new ArrayList<>();
            for (Method m : result.getClass().getMethods()) {
                if (!Modifier.isPublic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != String.class) continue;
                stringNoArg.add(m);
            }

            if (stringNoArg.size() == 1) {
                try {
                    return (String) stringNoArg.get(0).invoke(result);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            Assertions.fail("Impossible d'extraire le code Java depuis " + result.getClass().getName()
                    + " (pas String et pas de getter String clair).");
            return null; // unreachable
        }

        private static String tryNoArgStringMethod(Object obj, String methodName) {
            try {
                Method m = obj.getClass().getMethod(methodName);
                if (m.getReturnType() != String.class) return null;
                return (String) m.invoke(obj);
            } catch (NoSuchMethodException e) {
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }












    public static Object newInstance(Class<?> cls, Object... args) {
        try {
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                if (c.getParameterCount() != args.length) continue;
                if (!compatible(c.getParameterTypes(), args)) continue;
                c.setAccessible(true);
                return c.newInstance(args);
            }
            Assertions.fail("Aucun constructeur compatible pour " + cls.getName() + " avec args=" + Arrays.toString(args));
            return null;
        } catch (InvocationTargetException ite) {
            throw new RuntimeException(ite.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object invokeAnyMethod(Object target, String[] methodNames, Class<?>[] paramTypes, Object... args) {
        Class<?> cls = target.getClass();
        for (String name : methodNames) {
            try {
                Method m = cls.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m.invoke(target, args);
            } catch (NoSuchMethodException ignored) {
            } catch (InvocationTargetException ite) {
                throw new RuntimeException(ite.getTargetException());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        Assertions.fail("Aucune méthode trouvée parmi " + Arrays.toString(methodNames) + " dans " + cls.getName());
        return null;
    }

    public static Object invokeBestMethodReturningString(Object target, Object... args) {
        try {
            for (Method m : target.getClass().getMethods()) {
                if (m.getReturnType() != String.class) continue;
                if (m.getParameterCount() != args.length) continue;
                if (!compatible(m.getParameterTypes(), args)) continue;
                m.setAccessible(true);
                return m.invoke(target, args);
            }
        } catch (InvocationTargetException ite) {
            throw new RuntimeException(ite.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assertions.fail("Aucune méthode publique retournant String ne matche args=" + Arrays.toString(args)
                + " dans " + target.getClass().getName());
        return null;
    }

    private static boolean compatible(Class<?>[] pts, Object[] args) {
        for (int i = 0; i < pts.length; i++) {
            if (args[i] == null) continue;
            Class<?> need = wrap(pts[i]);
            Class<?> got = wrap(args[i].getClass());
            if (!need.isAssignableFrom(got)) return false;
        }
        return true;
    }

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == boolean.class) return Boolean.class;
        if (c == char.class) return Character.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        return c;
    }

    // ---------- Parser factory ----------
    /**
     * Essaie de parser un Programme depuis une string source.
     * Stratégies tentées (selon ton code):
     * - new Lexeur(String) + new AnaSynt(Lexeur) + anaSynt.parse()/analyser()/programme()...
     * - AnaSynt.parse(String) statique / analyser(String) / etc.
     */
    public static Object parseProgramme(String source) {
        Class<?> anaCls = mustClass("main.java.parseur.AnaSynt");
        Class<?> progCls = mustClass("main.java.parseur.ast.Programme");

        // 1) Essayer une méthode statique AnaSynt.xxx(String)->Programme
        for (Method m : anaCls.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!progCls.isAssignableFrom(m.getReturnType())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!wrap(m.getParameterTypes()[0]).isAssignableFrom(String.class)) continue;
            try {
                return m.invoke(null, source);
            } catch (InvocationTargetException ite) {
                throw new RuntimeException(ite.getTargetException());
            } catch (Exception ignored) {}
        }

        // 2) Essayer Lexeur + AnaSynt
        Class<?> lexCls = mustClass("main.java.lexeur.Lexeur");
        Object lex = null;

        // constructor Lexeur(String)
        try {
            lex = newInstance(lexCls, source);
        } catch (AssertionError ignored) {
            // essayer Lexeur(Reader)
            lex = newInstance(lexCls, new StringReader(source));
        }

        // AnaSynt(Lexeur)
        Object ana = newInstance(anaCls, lex);

        // Méthodes candidates pour obtenir un Programme
        String[] candidates = {"analyser", "parse", "programme", "analyse", "run"};
        for (String name : candidates) {
            try {
                Method m = anaCls.getMethod(name);
                if (progCls.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return m.invoke(ana);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (InvocationTargetException ite) {
                throw new RuntimeException(ite.getTargetException());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Assertions.fail("Impossible de parser un Programme depuis AnaSynt/Lexeur. "
                + "Ajoute une méthode publique parse()/analyser() qui retourne Programme, ou adapte TestTools.parseProgramme().");
        return null;
    }

    // ---------- Java compilation helper ----------
    public static void assertCompiles(String className, String javaCode) {
        try {
            Path tmp = Files.createTempDirectory("cgtest");
            Path srcFile = tmp.resolve(className + ".java");
            Files.writeString(srcFile, javaCode, StandardCharsets.UTF_8);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            Assertions.assertNotNull(compiler, "JavaCompiler null (tu utilises un JRE au lieu d'un JDK ?)");

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(List.of(srcFile.toFile()));
            List<String> options = List.of("-encoding", "UTF-8", "-d", tmp.toString());

            Boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();
            fm.close();

            if (!Boolean.TRUE.equals(ok)) {
                StringBuilder sb = new StringBuilder("Compilation Java échouée:\n");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    sb.append(d.toString()).append("\n");
                }
                sb.append("\n--- Code ---\n").append(javaCode);
                Assertions.fail(sb.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
