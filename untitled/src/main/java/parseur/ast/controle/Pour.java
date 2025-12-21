package main.java.parseur.ast.controle;

import main.java.parseur.ast.*;

/**
 * pour i = [0; 10], +=1	Incrémentation (i croît)
 * pour i = [10; 0], -=1	Décrémentation (i décroît)
 * pour i = [a; b], +=n	Incrémentation personnalisée
 * pour i = [a; b], -=n	Décrémentation personnalisée
 */

public class Pour extends Instruction {

    private final String nomVar;
    private final Expression debut;
    private final Expression fin;
    private final String operateur; // "+=" ou "-=" "*=", "/="
    private final Expression pas;
    private final Instruction corps;

    public Pour(String nomVar, Expression debut, Expression fin, String operateur, Expression pas, Instruction corps) {
        this.nomVar = nomVar;
        this.debut = debut;
        this.fin = fin;
        this.operateur = operateur;
        this.pas = pas;
        this.corps = corps;
    }

    public String getNomVar() {
        return nomVar;
    }

    public Expression getDebut() {
        return debut;
    }

    public Expression getFin() {
        return fin;
    }

    public String getOperateur() {
        return operateur;
    }

    public Expression getPas() {
        return pas;
    }

    public Instruction getCorps() {
        return corps;
    }

    private String conditionJava() {
        String finStr = fin.genJava();
        return switch (operateur) {
            case "+=" -> nomVar + " <= " + finStr;
            case "-=" -> nomVar + " >= " + finStr;
            case "*=" -> nomVar + " <= " + finStr; // par défaut croissance
            case "/=" -> nomVar + " >= " + finStr; // par défaut décroissance
            default -> throw new RuntimeException("Opérateur de boucle inconnu : " + operateur);
        };
    }


    private String miseAJourJava() {
        return nomVar + " " + operateur + " " + pas.genJava();
    }

    @Override
    public String genJava() {
        return "for (int " + nomVar + " = " + debut.genJava() + "; " +
                conditionJava() + "; " +
                miseAJourJava() + ") " +
                corps.genJava();
    }

}