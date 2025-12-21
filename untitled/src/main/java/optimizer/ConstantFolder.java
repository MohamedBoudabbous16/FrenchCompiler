package main.java.optimizer;

import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;

import java.util.List;

/**
 * Passe : pliage de constantes (constant folding) sur les expressions.
 * - Optimise principalement les listes d'expressions (affiche, appels de fonctions)
 * - Essaie aussi de modifier certains champs via réflexion si possible.
 */
class ConstantFolder {

    void optimize(Programme programme) {
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                optimizeInstruction(f.getCorps());
            }
        }
    }

    private void optimizeInstruction(Instruction i) {
        if (i == null) return;

        if (i instanceof Bloc b) {
            for (Instruction sub : b.getInstructions()) optimizeInstruction(sub);
            return;
        }

        if (i instanceof Affiche a) {
            List<Expression> exprs = a.getExpressions();
            if (exprs != null) {
                for (int k = 0; k < exprs.size(); k++) {
                    exprs.set(k, fold(exprs.get(k)));
                }
            }
            return;
        }

        if (i instanceof Affectation a) {
            // pas forcément modifiable (champ final), on tente via réflexion
            Expression folded = fold(a.getExpression());
            AstReflection.trySetField(a, "expression", folded);
            AstReflection.trySetField(a, "expr", folded); // au cas où
            return;
        }

        if (i instanceof Retourne r) {
            Expression folded = fold(r.getExpression());
            AstReflection.trySetField(r, "expression", folded);
            AstReflection.trySetField(r, "expr", folded);
            return;
        }

        if (i instanceof AppelFonctionInstr afi) {
            // optimiser les args à l'intérieur de l'appel
            Expression folded = fold(afi.getAppel());
            AstReflection.trySetField(afi, "appel", folded);
            AstReflection.trySetField(afi, "expression", folded);
            return;
        }

        if (i instanceof Si s) {
            AstReflection.trySetField(s, "condition", fold(s.getCondition()));
            optimizeInstruction(s.getAlorsInstr());
            if (s.getSinonInstr() != null) optimizeInstruction(s.getSinonInstr());
            return;
        }

        if (i instanceof TantQue tq) {
            AstReflection.trySetField(tq, "condition", fold(tq.getCondition()));
            optimizeInstruction(tq.getCorps());
            return;
        }

        if (i instanceof Pour p) {
            AstReflection.trySetField(p, "debut", fold(p.getDebut()));
            AstReflection.trySetField(p, "fin", fold(p.getFin()));
            AstReflection.trySetField(p, "pas", fold(p.getPas()));
            optimizeInstruction(p.getCorps());
        }
    }

    private Expression fold(Expression e) {
        if (e == null) return null;

        if (e instanceof ExpressionBinaire b) {
            Expression g2 = fold(b.getGauche());
            Expression d2 = fold(b.getDroite());

            // essayer de mettre à jour les enfants si possible
            AstReflection.trySetField(b, "gauche", g2);
            AstReflection.trySetField(b, "droite", d2);

            // tenter evaluation constante
            ConstValue cv = ConstEval.tryEvalBinary(b.getop(), g2, d2);
            if (cv != null) {
                Expression rebuilt = ConstEval.buildConstExpression(cv);
                if (rebuilt != null) return rebuilt;
            }
            return e;
        }

        if (e instanceof AppelFonction af) {
            List<Expression> args = af.getArgs();
            if (args != null) {
                for (int i = 0; i < args.size(); i++) {
                    args.set(i, fold(args.get(i)));
                }
            }
            return e;
        }

        // les littéraux / identifiants / lire() : rien à plier
        return e;
    }
}
