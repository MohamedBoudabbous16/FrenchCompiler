package parseur.ast;

import semantic.AnalyseSemantique;

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
