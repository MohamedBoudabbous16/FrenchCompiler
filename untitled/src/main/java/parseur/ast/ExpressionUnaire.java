package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class ExpressionUnaire extends Expression {
    //private final Position position;
    private final String op;      // "!"  "-"  "+"  "++"  "--"
    private final Expression expr;

    public ExpressionUnaire(Position position, String op, Expression expr) {
       super(position);
        this.op = op;
        this.expr = expr;
    }

    public String getOp() { return op; }
    public Expression getExpr() { return expr; }

    @Override
    public Position getPosition() { return super.getPosition(); }

    @Override
    public String genJava(AnalyseSemantique sem) {
        // Java direct (types/casts runtime gérés ailleurs si besoin)
        return "(" + op + expr.genJava(sem) + ")";
    }
}
