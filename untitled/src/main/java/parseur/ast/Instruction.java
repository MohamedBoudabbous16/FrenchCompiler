package java.parseur.ast;

import utils.diag.Position;

public abstract class Instruction extends NoeudAst {
    protected Instruction(Position pos) { super(pos); }
}