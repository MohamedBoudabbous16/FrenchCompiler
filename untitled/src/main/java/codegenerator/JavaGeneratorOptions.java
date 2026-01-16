package main.java.codegenerator;

import main.java.semantic.AnalyseSemantique;

public class JavaGeneratorOptions {

    // --- runtime lire() ---
    private boolean forceLireRuntime = false;
    private String scannerFieldName = "__scanner";
    private String scannerInitExpr = "new Scanner(System.in)";
    private String lireMethodName = "lire";

    // --- sémantique ---
    private boolean runSemanticAnalysis = true;
    private AnalyseSemantique semantic = null;

    public static JavaGeneratorOptions defaults() {
        return new JavaGeneratorOptions();
    }

    public boolean isForceLireRuntime() { return forceLireRuntime; }
    public String getScannerFieldName() { return scannerFieldName; }
    public String getScannerInitExpr() { return scannerInitExpr; }
    public String getLireMethodName() { return lireMethodName; }

    public boolean isRunSemanticAnalysis() { return runSemanticAnalysis; }

    public AnalyseSemantique getSemanticOrThrow() {
        if (semantic == null) {
            throw new IllegalStateException(
                    "Options.runSemanticAnalysis=false mais aucune AnalyseSemantique n'a été fournie (options.semantic)."
            );
        }
        return semantic;
    }

    // --- Builder style ---
    public JavaGeneratorOptions forceLireRuntime(boolean v) {
        this.forceLireRuntime = v;
        return this;
    }

    public JavaGeneratorOptions scannerFieldName(String v) {
        this.scannerFieldName = v;
        return this;
    }

    public JavaGeneratorOptions scannerInitExpr(String v) {
        this.scannerInitExpr = v;
        return this;
    }

    public JavaGeneratorOptions lireMethodName(String v) {
        this.lireMethodName = v;
        return this;
    }

    public JavaGeneratorOptions runSemanticAnalysis(boolean v) {
        this.runSemanticAnalysis = v;
        return this;
    }

    public JavaGeneratorOptions semantic(AnalyseSemantique sem) {
        this.semantic = sem;
        return this;
    }
}
