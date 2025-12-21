package main.java.ir;

/** Marqueur commun à tous les nœuds IR. */
public sealed interface IrNoeud
        permits IrProgramme, IrFonction, IrInstruction, IrExpression {
}
