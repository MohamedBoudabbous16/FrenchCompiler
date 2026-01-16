package java.parseur.ast.controle;

import java.parseur.ast.*;
import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class TantQue extends Instruction {

    private final Expression condition;
    private final Instruction corps;

    public TantQue(Position pos, Expression condition, Instruction corps) {
        super(pos);
        this.condition = condition;
        this.corps = corps;
    }

    public Expression getCondition() {
        return condition;
    }

    public Instruction getCorps() {
        return corps;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();
        sb.append("while (")
                .append(condition.genJava(sem))
                .append(") ")
                .append(corps.genJava(sem));

        return sb.toString();
    }
}
