package main.java.parseur.ast.controle;

import main.java.parseur.ast.*;
import main.java.semantic.AnalyseSemantique;

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
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();
        sb.append("while (")
                .append(condition.genJava(sem))
                .append(") ")
                .append(corps.genJava(sem));

        return sb.toString();
    }
}
