package main.java.ir;

import java.util.List;

public record IrBloc(List<IrInstruction> instructions) implements IrInstruction {
    public IrBloc {
        if (instructions == null) instructions = List.of();
    }
}
