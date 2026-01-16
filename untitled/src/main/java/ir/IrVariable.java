package main.java.ir;

import java.util.Objects;

/**
 * Variable IR (nom symbolique).
 */
public record IrVariable(String name) implements IrExpression {

    public IrVariable {
        name = (name == null || name.isBlank()) ? "<var?>" : name;
        Objects.requireNonNull(name, "name");
    }
}
