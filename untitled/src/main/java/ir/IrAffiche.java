package java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Affiche IR.
 *
 * Garanties utiles côté génération Java:
 * - multi-args => un System.out.print(...) par argument
 * - newline => ajoute un System.out.println() final
 */
public record IrAffiche(List<IrExpression> args, boolean newline) implements IrInstruction {

    public IrAffiche {
        args = (args == null) ? List.of() : List.copyOf(args);
        Objects.requireNonNull(args, "args");
    }
}
