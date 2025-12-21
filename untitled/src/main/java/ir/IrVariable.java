package main.java.ir;

import java.util.Objects;

public record IrVariable(String nom) implements IrExpression {
    public IrVariable {
        Objects.requireNonNull(nom, "nom");
    }
    public String name() { return nom; }
}
