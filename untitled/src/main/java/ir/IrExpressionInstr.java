package main.java.ir;

import java.util.Objects;

/**
 * Une expression utilisée comme instruction (ex: appel de fonction "f(...);").
 */
public record IrExpressionInstr(IrExpression expression) implements IrInstruction {

    public IrExpressionInstr {
        expression = (expression == null) ? new IrVariable("<expr?>") : expression;
        Objects.requireNonNull(expression, "expression");
    }
}
