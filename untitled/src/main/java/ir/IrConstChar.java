package main.java.ir;

public record IrConstChar(char valeur) implements IrExpression {
    public char value() { return valeur; }

    public IrConstChar {
        // if (valeur == '\0') throw new IllegalArgumentException("caractère nul interdit");
    }//protéger contre le caractère “nul” '\0' (si utilisé comme valeur de récupération) :

}
