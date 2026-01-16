package main.java.ir;

import java.util.Objects;

/**
 * Affectation IR : nomVariable = expression;
 */
public record IrAffectation(String nomVariable, IrExpression expression) implements IrInstruction {

    public IrAffectation {
        nomVariable = (nomVariable == null || nomVariable.isBlank()) ? "<var?>" : nomVariable;
        expression = (expression == null) ? new IrVariable("<expr?>") : expression;
        Objects.requireNonNull(nomVariable, "nomVariable");
        Objects.requireNonNull(expression, "expression");
    }

    /** Alias pratique utilisé par certains générateurs. */
    public String variable() {
        return nomVariable;
    }
}
