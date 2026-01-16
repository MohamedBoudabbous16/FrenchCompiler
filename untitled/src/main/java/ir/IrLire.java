package java.ir;

/**
 * Expression builtin: lire()
 * Singleton (pas de champs).
 */
public final class IrLire implements IrExpression {
    public static final IrLire INSTANCE = new IrLire();
    private IrLire() {}
}
