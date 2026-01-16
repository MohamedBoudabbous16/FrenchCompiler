package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Lire extends Expression {

    public Lire(Position pos) {
        super(pos);
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "lire()";
    }
}
