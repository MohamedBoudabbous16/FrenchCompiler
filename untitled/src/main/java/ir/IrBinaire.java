package java.ir;

import java.util.Objects;

/**
 * Expression binaire IR : gauche op droite
 */
public record IrBinaire(IrExpression gauche, String op, IrExpression droite) implements IrExpression {

    public IrBinaire {
        gauche = (gauche == null) ? new IrVariable("<g?>") : gauche;
        droite = (droite == null) ? new IrVariable("<d?>") : droite;
        op = (op == null || op.isBlank()) ? "?" : op;
        Objects.requireNonNull(gauche, "gauche");
        Objects.requireNonNull(op, "op");
        Objects.requireNonNull(droite, "droite");
    }
}
