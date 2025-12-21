package main.java.ir;

import java.util.List;
import java.util.Objects;

/** affiche(args...) avec option newline. */
public record IrAffiche(List<IrExpression> args, boolean newline) implements IrInstruction {
    public IrAffiche {
        Objects.requireNonNull(args, "args");
    }
}
