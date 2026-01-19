package main.java.parseur.ast.controle;

import main.java.parseur.ast.*;
import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

/**
 * pour i = [0; 10], +=1   Incrémentation (i croît)
 * pour i = [10; 0], -=1   Décrémentation (i décroît)
 * pour i = [a; b], +=n    Incrémentation personnalisée
 * pour i = [a; b], -=n    Décrémentation personnalisée
 *
 * Extensions (si tu autorises) :
 *  - "*=" : on suppose que i "croît" si pas > 1, sinon ça diminue (mais c'est flou)
 *  - "/=" : souvent décroissance si pas > 1
 *  - "%=" : pas monotone -> condition par défaut (i != fin) (attention boucle infinie possible)
 *
 * Recommandation: en vrai langage, on limite le "pour" à += / -=.
 */
public class Pour extends Instruction {

    private final String nomVar;
    private final Expression debut;
    private final Expression fin;
    private final String operateur; // "+=" "-=" "*=" "/=" "%="
    private final Expression pas;
    private final Instruction corps;

    public Pour(Position pos, String nomVar, Expression debut, Expression fin, String operateur, Expression pas, Instruction corps) {
        super(pos);
        this.nomVar = nomVar;
        this.debut = debut;
        this.fin = fin;
        this.operateur = operateur;
        this.pas = pas;
        this.corps = corps;
    }

    public String getNomVar() { return nomVar; }
    public Expression getDebut() { return debut; }
    public Expression getFin() { return fin; }
    public String getOperateur() { return operateur; }
    public Expression getPas() { return pas; }
    public Instruction getCorps() { return corps; }

    private String conditionJava(AnalyseSemantique sem) {
        String finStr = fin.genJava(sem);

        return switch (operateur) {
            // cas standard
            case "+=" -> nomVar + " <= " + finStr;
            case "-=" -> nomVar + " >= " + finStr;

            // choix "raisonnables" mais imparfaits :
            // *= : si le pas est > 1, i augmente donc <= fin.
            // (si pas entre 0 et 1 ça n'existe pas en int)
            case "*=" -> nomVar + " <= " + finStr;

            // /= : en int, i diminue si pas > 1 donc >= fin
            case "/=" -> nomVar + " >= " + finStr;

            // %= : pas monotone => on fait une boucle jusqu'à atteindre exactement fin
            // ⚠ risque de boucle infinie si jamais i n'atteint jamais fin
            case "%=" -> nomVar + " != " + finStr;

            default -> throw new RuntimeException("Opérateur de boucle inconnu : " + operateur);
        };
    }

    private String miseAJourJava(AnalyseSemantique sem) {
        return nomVar + " " + operateur + " " + pas.genJava(sem);
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return "for (int " + nomVar + " = " + debut.genJava(sem) + "; " +
                conditionJava(sem) + "; " +
                miseAJourJava(sem) + ") " +
                corps.genJava(sem);
    }
}
