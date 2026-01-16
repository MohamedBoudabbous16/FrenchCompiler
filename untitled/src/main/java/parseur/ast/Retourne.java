package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Retourne extends Instruction{
    private final Expression expression;

    public Retourne(Position pos,Expression expression) {
        super(pos);
        this.expression = expression;}

    public Expression getExpression() {return this.expression;}

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "return " + expression.genJava(sem) + ";";
    }
}
