package main.java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Appel de fonction IR : nom(args...)
 */
public record IrAppel(String nom, List<IrExpression> args) implements IrExpression {

    public IrAppel {
        nom = (nom == null || nom.isBlank()) ? "<f?>" : nom;
        args = (args == null) ? List.of() : List.copyOf(args);
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(args, "args");
    }
}
