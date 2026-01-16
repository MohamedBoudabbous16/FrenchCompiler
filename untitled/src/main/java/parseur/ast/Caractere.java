package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Caractere extends Expression {

    private final char valeur;

    public Caractere(Position pos, char valeur) {
        super(pos);
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
