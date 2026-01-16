package main.java.ir;

public record IrConstInt(int valeur) implements IrExpression {
    public IrConstInt {
        if (valeur < 0) throw new IllegalArgumentException("valeur doit être >= 0");
    }
//pour interdire des valuers negatives
    public int valeur() { return valeur; }
}
