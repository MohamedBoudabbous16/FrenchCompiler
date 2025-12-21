package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Lire extends Expression {
    @Override
    public String genJava(AnalyseSemantique sem) {
        // lit un int sur l'entrée standard ; adapter selon vos besoins
        return "(new java.util.Scanner(System.in)).nextInt()";
    }
}