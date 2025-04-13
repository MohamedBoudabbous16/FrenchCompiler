package parseur.ast.controle;

import parseur.ast.*;

public class TantQue extends Instruction {

    private final Expression condition;
    private final Instruction corps;

    public TantQue(Expression condition, Instruction corps) {
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
    public String genJava() {
        StringBuilder sb = new StringBuilder();
        sb.append("while (")
                .append(condition.genJava())
                .append(") ")
                .append(corps.genJava());

        return sb.toString();
    }
}
