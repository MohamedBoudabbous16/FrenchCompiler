package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Affectation extends Instruction{
// une instruction d'affectation comme x = expression
    private final String nomVar;
    private final Expression expression;;
    public Affectation(String nomVar, Expression expression) {
        this.nomVar = nomVar;
        this.expression = expression;
    }
    public String getNomVar() {
        return nomVar;
    }
    public Expression getExpression() {
        return expression;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return nomVar + "=" + expression.genJava(sem) + ";";
    }

}
