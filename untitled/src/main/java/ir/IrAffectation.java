package main.java.ir;

import java.util.Objects;

/** x = expr; */
public record IrAffectation(String nomVariable, IrExpression expression) implements IrInstruction {
    public IrAffectation {
        Objects.requireNonNull(nomVariable, "nomVariable");
        Objects.requireNonNull(expression, "expression");
    }
}
