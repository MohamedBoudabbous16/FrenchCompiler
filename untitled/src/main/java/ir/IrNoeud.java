package java.ir;

/**
 * Racine de tous les nœuds de l’IR.
 * IR = représentation intermédiaire stable, indépendante du parseur.
 */
public sealed interface IrNoeud permits IrProgramme, IrFonction, IrInstruction, IrExpression {
}
