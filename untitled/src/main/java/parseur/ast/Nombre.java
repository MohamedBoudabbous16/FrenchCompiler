package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Nombre extends Expression  {

    private final int valeur;

    public Nombre (Position pos, int valeur){
        super(pos);
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
