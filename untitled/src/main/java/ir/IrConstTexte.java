package main.java.ir;

import java.util.Objects;

public record IrConstTexte(String valeur) implements IrExpression {
    public IrConstTexte {
        Objects.requireNonNull(valeur, "valeur");
    }
}
