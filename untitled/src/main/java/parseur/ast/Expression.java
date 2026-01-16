package java.parseur.ast;

import utils.diag.Position;

public abstract class Expression extends NoeudAst {
    protected Expression(Position pos) { super(pos); }
}