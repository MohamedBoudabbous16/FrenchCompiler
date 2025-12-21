package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Caractere extends Expression {

    private final char valeur;

    public Caractere(char valeur) {
        this.valeur = valeur;
    }

    public char getValeur() {
        return valeur;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        if (valeur == '\\') return "'\\\\'";
        if (valeur == '\'') return "'\\''";
        return "'" + valeur + "'";
    }
}
