package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class ExpressionAffectation extends Expression {

    private final Expression cible;
    private final String op;
    private final Expression valeur;

    public ExpressionAffectation(Position position, Expression cible, String op, Expression valeur) {
        super(position);
        this.cible = cible;
        this.op = op;
        this.valeur = valeur;
    }

    public Expression getCible() { return cible; }
    public String getOp() { return op; }
    public Expression getValeur() { return valeur; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "(" + cible.genJava(sem) + " " + op + " " + valeur.genJava(sem) + ")";
    }
}
