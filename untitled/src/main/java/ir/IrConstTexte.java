package main.java.ir;

import java.util.Objects;

public record IrConstTexte(String valeur) implements IrExpression {
    public IrConstTexte {
        Objects.requireNonNull(valeur, "valeur");
        if (valeur.isEmpty()) throw new IllegalArgumentException("valeur ne doit pas être vide");
    }

    public String value() { return valeur; }
}
