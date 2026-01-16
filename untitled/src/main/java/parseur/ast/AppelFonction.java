package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

import java.util.List;

public class AppelFonction extends Expression {

    private final String nom;
    private final List<Expression> args;

    public AppelFonction(Position pos, String nom, List<Expression> args) {
        super(pos);
        this.nom = nom;
        this.args = args;
    }

    public String getNom() {
        return nom;
    }

    public List<Expression> getArgs() {
        return args;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();
        sb.append(nom).append("(");

        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(args.get(i).genJava(sem));
        }

        sb.append(")");
        return sb.toString();
    }
}
