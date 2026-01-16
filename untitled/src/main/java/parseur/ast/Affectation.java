package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Affectation extends Instruction{
// une instruction d'affectation comme x = expression
    private final String nomVar;
    private final Expression expression;;
    public Affectation(Position pos, String nomVar, Expression expression) {
        super(pos);
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
