package main.java.codegenerator;

import main.java.semantic.AnalyseSemantique;

public class GenerationResult {
    private final String javaSource;
    private final AnalyseSemantique semantic;

    public GenerationResult(String javaSource, AnalyseSemantique semantic) {
        this.javaSource = javaSource;
        this.semantic = semantic;
    }

    public String getJavaSource() { return javaSource; }
    public AnalyseSemantique getSemantic() { return semantic; }
    public String getSource() { return javaSource; }



    @Override
    public String toString() { return javaSource; }
}
