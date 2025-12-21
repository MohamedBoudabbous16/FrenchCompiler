package main.java.ir;

import java.util.Objects;

/** if (cond) alorsInstr else sinonInstr (sinonInstr peut être null). */
public record IrSi(IrExpression condition, IrInstruction alorsInstr, IrInstruction sinonInstr) implements IrInstruction {
    public IrSi {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(alorsInstr, "alorsInstr");
    }
}
