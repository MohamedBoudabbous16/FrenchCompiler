package main.java.parseur.ast.controle;

import main.java.parseur.ast.Expression;
import main.java.parseur.ast.Instruction;
import main.java.semantic.AnalyseSemantique;
import main.java.semantic.TypeSimple;
import utils.diag.Position;

public class Si extends Instruction {

    private final Expression condition;
    private final Instruction alorsInstr;
    private final Instruction sinonInstr; // Peut être null

    public Si(Position pos, Expression condition, Instruction alorsInstr, Instruction sinonInstr) {
        super(pos);
        this.condition = condition;
        this.alorsInstr = alorsInstr;
        this.sinonInstr = sinonInstr;
    }

    public Expression getCondition() { return condition; }
    public Instruction getAlorsInstr() { return alorsInstr; }
    public Instruction getSinonInstr() { return sinonInstr; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();

        String condJava = condition.genJava(sem);
        if (sem.typeDe(condition) == TypeSimple.INCONNU) {
            condJava = "RuntimeSupport.asBool(" + condJava + ")";
        }

        sb.append("if (").append(condJava).append(") {\n");

        String alors = alorsInstr.genJava(sem);
        if (alors != null && !alors.isBlank()) {
            for (String line : alors.split("\n")) {
                sb.append("  ").append(line).append("\n");
            }
        }

        sb.append("}");

        if (sinonInstr != null) {
            sb.append(" else {\n");
            String sinon = sinonInstr.genJava(sem);
            if (sinon != null && !sinon.isBlank()) {
                for (String line : sinon.split("\n")) {
                    sb.append("  ").append(line).append("\n");
                }
            }
            sb.append("}");
        }

        return sb.toString();
    }
}
