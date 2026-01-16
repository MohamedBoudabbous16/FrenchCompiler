package main.java.ir;

/**
 * Instruction IR (sealed) : on garde des constructs proches de ton AST,
 * mais sous une forme stable et compacte.
 */
public sealed interface IrInstruction extends IrNoeud
        permits IrBloc,
        IrAffectation,
        IrRetourne,
        IrAffiche,
        IrExpressionInstr,
        IrSi,
        IrTantQue,
        IrPour {
}
