package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class ExpressionBinaire extends Expression {
    private final Expression gauche;
    private final String op;
    private final Expression droite;
    public ExpressionBinaire(Position pos, Expression gauche, String op, Expression droite ){
        super(pos);
        this.gauche = gauche;
        this.op = op;
        this.droite = droite;
    }
    public Expression getGauche() {
        return gauche;
    }

    public String getop() {
        return op;
    }

    public Expression getDroite() {
        return droite;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "(" + gauche.genJava(sem) + " " + op + " " + droite.genJava(sem) + ")";
    }
}
