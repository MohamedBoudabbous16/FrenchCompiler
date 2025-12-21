package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

import java.util.List;


import java.util.List;

public class Affiche extends Instruction {
    private final List<Expression> expressions;
    private final boolean newline;

    public Affiche(List<Expression> expressions, boolean newline) {
        this.expressions = expressions;
        this.newline = newline;
    }

    public List<Expression> getExpressions() { return expressions; }
    public boolean isNewline() { return newline; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            sb.append("System.out.print(")
                    .append(expressions.get(i).genJava(sem))
                    .append(");");
            if (i < expressions.size() - 1) {
                sb.append("\n");
            }
        }
        if (newline) {
            sb.append("\nSystem.out.println();");
        }
        return sb.toString();
    }
}
