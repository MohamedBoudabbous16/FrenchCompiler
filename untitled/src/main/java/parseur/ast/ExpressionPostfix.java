package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class ExpressionPostfix extends Expression {

    private final Expression expr;
    private final String op; // "++" ou "--"

    public ExpressionPostfix(Position position, Expression expr, String op) {
        super(position);          // ✅ obligatoire
        this.expr = expr;
        this.op = op;
    }

    public Expression getExpr() { return expr; }
    public String getOp() { return op; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "(" + expr.genJava(sem) + op + ")";
    }
}
