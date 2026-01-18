package main.java.parseur.ast.controle;

import main.java.parseur.ast.Expression;
import main.java.parseur.ast.Instruction;
import main.java.semantic.AnalyseSemantique;
import main.java.semantic.TypeSimple;
import utils.diag.Position;

public class TantQue extends Instruction {

    private final Expression condition;
    private final Instruction corps;

    public TantQue(Position pos, Expression condition, Instruction corps) {
        super(pos);
        this.condition = condition;
        this.corps = corps;
    }

    public Expression getCondition() { return condition; }
    public Instruction getCorps() { return corps; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();

        String condJava = condition.genJava(sem);
        if (sem.typeDe(condition) == TypeSimple.INCONNU) {
            condJava = "RuntimeSupport.asBool(" + condJava + ")";
        }

        sb.append("while (").append(condJava).append(") ");

        // On laisse la responsabilité au corps : s'il génère déjà "{...}" c'est parfait.
        // Sinon on l'encapsule en bloc.
        String corpsJava = corps.genJava(sem);
        String trimmed = (corpsJava == null) ? "" : corpsJava.trim();

        if (trimmed.startsWith("{")) {
            sb.append(corpsJava);
        } else {
            sb.append("{\n");
            if (!trimmed.isBlank()) {
                for (String line : corpsJava.split("\n")) {
                    sb.append("  ").append(line).append("\n");
                }
            }
            sb.append("}");
        }

        return sb.toString();
    }
}
