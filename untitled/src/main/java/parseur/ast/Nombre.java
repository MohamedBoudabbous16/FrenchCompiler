package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Nombre extends Expression  {

    private final int valeur;

    public Nombre (int valeur){
        this.valeur = valeur;
    }

    public int getValeur() {
        return valeur;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return Integer.toString(valeur);
    }

}
