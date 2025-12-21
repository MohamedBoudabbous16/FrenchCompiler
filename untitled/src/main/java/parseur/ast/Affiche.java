package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Affiche extends Instruction {

    private final Expression expression;

    public Affiche(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "System.out.println(" + expression.genJava(sem) + ");";
    }
}
