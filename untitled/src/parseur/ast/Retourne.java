package parseur.ast;

import semantic.AnalyseSemantique;

public class Retourne extends Instruction{
    private final Expression expression;

    public Retourne(Expression expression) {this.expression = expression;}

    public Expression getExpression() {return this.expression;}

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "return " + expression.genJava(sem) + ";";
    }
}
