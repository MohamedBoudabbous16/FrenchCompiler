package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Lire extends Expression {
    @Override
    public String genJava(AnalyseSemantique sem) {
        return "lire()";
    }

}