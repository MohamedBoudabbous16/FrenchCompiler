package java.ir;

import java.util.Objects;

/**
 * TantQue IR : while (condition) corps
 */
public record IrTantQue(IrExpression condition, IrInstruction corps) implements IrInstruction {

    public IrTantQue {
        condition = (condition == null) ? new IrConstBool(false) : condition;
        corps = (corps == null) ? new IrBloc(java.util.List.of()) : corps;
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(corps, "corps");
    }
}
