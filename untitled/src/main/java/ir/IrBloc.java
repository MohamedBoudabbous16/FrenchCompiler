package java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Bloc IR : liste d'instructions.
 */
public record IrBloc(List<IrInstruction> instructions) implements IrInstruction {

    public IrBloc {
        instructions = (instructions == null) ? List.of() : List.copyOf(instructions);
        Objects.requireNonNull(instructions, "instructions");
    }
}
