package main.java.ir;

import java.util.Objects;

/** while (cond) corps */
public record IrTantQue(IrExpression condition, IrInstruction corps) implements IrInstruction {
    public IrTantQue {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(corps, "corps");
    }
}
