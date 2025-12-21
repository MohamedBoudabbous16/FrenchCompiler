package main.java.ir;

import java.util.Objects;

/** exprG op exprD */
public record IrBinaire(IrExpression gauche, String operateur, IrExpression droite) implements IrExpression {
    public IrBinaire {
        Objects.requireNonNull(gauche, "gauche");
        Objects.requireNonNull(operateur, "operateur");
        Objects.requireNonNull(droite, "droite");
    }
    public String op() { return operateur; }
}
