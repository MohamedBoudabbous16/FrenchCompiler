package main.java.ir;

import java.util.List;
import java.util.Objects;

/** appel f(args...) */
public record IrAppel(String nomFonction, List<IrExpression> args) implements IrExpression {
    public IrAppel {
        Objects.requireNonNull(nomFonction, "nomFonction");
        Objects.requireNonNull(args, "args");
    }
}
