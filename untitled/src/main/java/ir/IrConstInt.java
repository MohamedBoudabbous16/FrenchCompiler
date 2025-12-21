package main.java.ir;

public record IrConstInt(int valeur) implements IrExpression {
    public int value() { return valeur; }
}
