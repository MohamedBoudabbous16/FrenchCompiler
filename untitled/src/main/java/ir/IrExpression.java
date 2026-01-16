package main.java.ir;

/**
 * Expression IR (sealed) : littéraux, variables, binaire, appel, lire().
 */
public sealed interface IrExpression extends IrNoeud
        permits IrConstInt, IrConstTexte, IrConstChar, IrConstBool,
        IrVariable, IrBinaire, IrAppel, IrLire {
}
