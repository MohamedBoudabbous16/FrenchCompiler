package parseur.ast;

public class Affiche extends Instruction {

    private final Expression expression;

    public Affiche(Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public String genJava() {
        return "System.out.println(" + expression.genJava() + ");";
    }
}
