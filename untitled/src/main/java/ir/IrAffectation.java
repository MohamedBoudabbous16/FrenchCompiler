package main.java.ir;

import java.util.Objects;

public record IrAffectation(String nomVariable, IrExpression expression) implements IrInstruction {
    public IrAffectation {
        Objects.requireNonNull(nomVariable, "nomVariable");
        Objects.requireNonNull(expression, "expression");
    }

    // ✅ Alias attendu par IrVersJava
    public String variable() { return nomVariable; }
}
