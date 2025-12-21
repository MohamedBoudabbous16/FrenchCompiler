package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Texte extends Expression {

    private final String valeur;

    public Texte(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "\"" + valeur.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
