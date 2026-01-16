package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Texte extends Expression {

    private final String valeur;

    public Texte(Position pos, String valeur) {
        super(pos);
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
