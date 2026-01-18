package main.java.codegenerator;

import main.java.parseur.ast.Programme;
import main.java.semantic.AnalyseSemantique;
import utils.diag.DiagnosticCollector;

import java.util.Objects;

public class JavaGenerator {

    public GenerationResult generate(Programme programme) {
        return generate(programme, JavaGeneratorOptions.defaults());
    }

    public GenerationResult generate(Programme programme, JavaGeneratorOptions options) {
        Objects.requireNonNull(programme, "programme");
        Objects.requireNonNull(options, "options");

        AnalyseSemantique sem = options.isRunSemanticAnalysis()
                ? runSemantic(programme)
                : options.getSemanticOrThrow();

        String source = programme.genJava(sem);

        // (A) runtime pour lire()
        boolean needsLireRuntime = options.isForceLireRuntime()
                || RuntimeSupport.programmeUsesLire(programme);

        if (needsLireRuntime) {
            String runtimeChunk = RuntimeSupport.lireIntRuntimeChunk(
                    options.getScannerFieldName(),
                    options.getScannerInitExpr(),
                    options.getLireMethodName()
            );
            source = SourcePatcher.injectBeforeLastBrace(source, runtimeChunk);
            source = ImportManager.ensureImport(source, "java.util.Scanner");
        }

        // (B) runtime pour casts/conversions (asInt/asBool/...)
        boolean needsTypeRuntime =
                source.contains("RuntimeSupport.asInt(") ||
                        source.contains("RuntimeSupport.asBool(") ||
                        source.contains("RuntimeSupport.asString(") ||
                        source.contains("RuntimeSupport.asChar(");

        if (needsTypeRuntime && !source.contains("class RuntimeSupport")) {
            source = SourcePatcher.injectBeforeLastBrace(source, typeRuntimeChunk());
        }

        return new GenerationResult(source, sem);
    }

    private AnalyseSemantique runSemantic(Programme programme) {
        DiagnosticCollector diags = new DiagnosticCollector();
        AnalyseSemantique sem = new AnalyseSemantique(diags);
        sem.verifier(programme);
        return sem;
    }

    // Classe runtime injectée dans ProgrammePrincipal (Java généré)
    private String typeRuntimeChunk() {
        return """
                
                public static final class RuntimeSupport {
                    private RuntimeSupport() {}

                    public static int asInt(Object v) {
                        if (v instanceof Integer i) return i;
                        if (v instanceof Character c) return (int) c;
                        if (v instanceof Boolean b) return b ? 1 : 0;
                        throw new RuntimeException("Valeur non convertible en int: " + v);
                    }

                    public static boolean asBool(Object v) {
                        if (v instanceof Boolean b) return b;
                        if (v instanceof Integer i) return i != 0;
                        throw new RuntimeException("Valeur non convertible en boolean: " + v);
                    }

                    public static String asString(Object v) {
                        if (v == null) return "null";
                        return v.toString();
                    }

                    public static char asChar(Object v) {
                        if (v instanceof Character c) return c;
                        if (v instanceof Integer i) return (char) (int) i;
                        throw new RuntimeException("Valeur non convertible en char: " + v);
                    }
                }
                """;
    }
}
