package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Affectation extends Instruction {
    private final String nomVar;
    private final String operateur; // "=", "+=", "-=", "*=", "/=", "%="
    private final Expression expression;

    // Ancien constructeur conservé (compat)
    public Affectation(Position pos, String nomVar, Expression expression) {
        this(pos, nomVar, "=", expression);
    }

    public Affectation(Position pos, String nomVar, String operateur, Expression expression) {
        super(pos);
        this.nomVar = nomVar;
        this.operateur = operateur;
        this.expression = expression;
    }

    public String getNomVar() { return nomVar; }
    public String getOperateur() { return operateur; }
    public Expression getExpression() { return expression; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return nomVar + " " + operateur + " " + expression.genJava(sem) + ";";
    }
}
