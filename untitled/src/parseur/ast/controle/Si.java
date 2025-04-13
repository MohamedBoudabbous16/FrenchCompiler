package parseur.ast.controle;
import parseur.ast.*;
public class Si extends Instruction {

    private final Expression condition;
    private final Instruction alorsInstr;
    private final Instruction sinonInstr; // Peut être null

    public Si(Expression condition, Instruction alorsInstr, Instruction sinonInstr) {
        this.condition = condition;
        this.alorsInstr = alorsInstr;
        this.sinonInstr = sinonInstr;
    }

    public Expression getCondition() {
        return condition;
    }

    public Instruction getAlorsInstr() {
        return alorsInstr;
    }

    public Instruction getSinonInstr() {
        return sinonInstr;
    }

    @Override
    public String genJava() {
        StringBuilder sb = new StringBuilder();
        sb.append("if (").append(condition.genJava()).append(") {\n");
        sb.append("  ").append(alorsInstr.genJava()).append("\n");
        sb.append("}");

        if (sinonInstr != null) {
            sb.append(" else {\n");
            sb.append("  ").append(sinonInstr.genJava()).append("\n");
            sb.append("}");
        }

        return sb.toString();
    }

}