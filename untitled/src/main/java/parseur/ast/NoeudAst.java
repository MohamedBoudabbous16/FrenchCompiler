package java.parseur.ast;



import utils.diag.Position;

public abstract class NoeudAst implements Noeud, AvecPosition {
    private final Position position;

    protected NoeudAst(Position position) {
        this.position = position;
    }

    @Override
    public Position getPosition() {
        return position;
    }
}
