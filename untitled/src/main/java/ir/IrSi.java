package main.java.ir;

import java.util.Objects;

/**
 * Si IR : if (condition) alorsInstr else sinonInstr.
 * sinonInstr peut être null.
 */
public record IrSi(IrExpression condition, IrInstruction alorsInstr, IrInstruction sinonInstr)
        implements IrInstruction {

    public IrSi {
        condition = (condition == null) ? new IrConstBool(false) : condition;
        alorsInstr = (alorsInstr == null) ? new IrBloc(java.util.List.of()) : alorsInstr;
        // sinonInstr peut être null
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(alorsInstr, "alorsInstr");
    }
}
