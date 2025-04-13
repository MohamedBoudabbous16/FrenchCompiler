package parseur.ast;

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
    public String genJava() {
        return nomVar + "=" + expression.genJava() + ";";
    }

}
