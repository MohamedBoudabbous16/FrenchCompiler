package main.java.ir;

import java.util.Objects;

/** Une expression utilisée comme instruction (ex: appel de fonction). */
public record IrExpressionInstr(IrExpression expression) implements IrInstruction {
    public IrExpressionInstr {
        Objects.requireNonNull(expression, "expression");
    }
}
