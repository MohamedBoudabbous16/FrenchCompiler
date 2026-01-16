package java.ir;

/**
 * Retourne expr; (expr peut être null si la fonction est void).
 */
public record IrRetourne(IrExpression expression) implements IrInstruction {
}
