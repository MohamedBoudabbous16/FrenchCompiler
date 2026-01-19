package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class ExpressionInstr extends Instruction {
    private final Expression expression;

    public ExpressionInstr(Position pos, Expression expression) {
        super(pos);
        this.expression = expression;
    }

    public Expression getExpression() { return expression; }

    @Override
    public String genJava(AnalyseSemantique sem) {

        // ✅ Java accepte assignment-statement, mais PAS "(x = 3);"
        if (expression instanceof ExpressionAffectation a) {
            return a.getCible().genJava(sem) + " " + a.getOp() + " " + a.getValeur().genJava(sem) + ";";
        }

        // ✅ Postfix ++/--
        if (expression instanceof ExpressionPostfix p) {
            return p.getExpr().genJava(sem) + p.getOp() + ";";
        }

        // ✅ Prefix ++/-- (dans ton AST : ExpressionUnaire)
        if (expression instanceof ExpressionUnaire u) {
            if ("++".equals(u.getOp()) || "--".equals(u.getOp())) {
                return u.getOp() + u.getExpr().genJava(sem) + ";";
            }
        }

        return expression.genJava(sem) + ";";
    }

}
