package java.ir;

public record IrConstBool(boolean valeur) implements IrExpression {
    public boolean value() { return valeur; }
}
