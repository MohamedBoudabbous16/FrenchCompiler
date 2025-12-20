package semantic;

import parseur.ast.*;
import parseur.ast.controle.Pour;
import parseur.ast.controle.Si;
import parseur.ast.controle.TantQue;

public class AnalyseSemantique {

    private final TableSymboles ts = new TableSymboles();
    private String fonctionCourante = "??";

    public void verifier(Programme programme) {
        // 1) vérifier toutes les classes / fonctions
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                verifierFonction(f);
            }
        }
    }

    private void verifierFonction(Fonction f) {
        fonctionCourante = f.getNom();

        ts.entrerPortee(); // portée de la fonction

        // Paramètres : (pour l’instant, si tu n’as pas de types sur paramètres,
        // tu peux les considérer ENTIER par défaut ou INCONNU)
        for (String p : f.getParam()) {
            ts.declarer(p, TypeSimple.ENTIER, true); // choix minimal
        }

        verifierBloc(f.getCorps());

        ts.sortirPortee();
    }

    private void verifierBloc(Bloc bloc) {
        ts.entrerPortee(); // scope de bloc

        for (Instruction instr : bloc.getInstructions()) {
            verifierInstruction(instr);
        }

        ts.sortirPortee();
    }

    private void verifierInstruction(Instruction i) {

        if (i instanceof Bloc b) {
            verifierBloc(b);
            return;
        }

        if (i instanceof Affectation a) {
            Symbole s = ts.resoudre(a.getNomVar());
            if (s == null) {
                throw new ErreurSemantique(msg("Variable '" + a.getNomVar() + "' utilisée avant déclaration."));
            }
            TypeSimple tExpr = typerExpression(a.getExpression());
            if (s.getType() != tExpr) {
                throw new ErreurSemantique(msg("Affectation incompatible : " + s.getType() + " = " + tExpr));
            }
            return;
        }

        if (i instanceof Retourne r) {
            // Si tu n’as pas encore le type de retour des fonctions,
            // tu peux juste typer l’expression pour détecter les erreurs internes.
            typerExpression(r.getExpression());
            return;
        }

        if (i instanceof Si s) {
            TypeSimple tCond = typerExpression(s.getCondition());
            if (tCond != TypeSimple.BOOLEEN) {
                throw new ErreurSemantique(msg("Condition de 'si' doit être BOOLEEN, trouvé : " + tCond));
            }
            verifierInstruction(s.getAlorsInstr());
            if (s.getSinonInstr() != null) verifierInstruction(s.getSinonInstr());
            return;
        }

        if (i instanceof TantQue tq) {
            TypeSimple tCond = typerExpression(tq.getCondition());
            if (tCond != TypeSimple.BOOLEEN) {
                throw new ErreurSemantique(msg("Condition de 'tantque' doit être BOOLEEN, trouvé : " + tCond));
            }
            verifierInstruction(tq.getCorps());
            return;
        }

        if (i instanceof Pour p) {
            // Hypothèse actuelle : variable de boucle ENTIER
            // (idéalement : elle doit être déclarée avant, ou le 'pour' la déclare)
            Symbole s = ts.resoudre(p.getNomVar());
            if (s == null) {
                throw new ErreurSemantique(msg("Variable de boucle '" + p.getNomVar() + "' doit être déclarée avant le 'pour'."));
            }
            if (s.getType() != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Variable de boucle '" + p.getNomVar() + "' doit être ENTIER."));
            }

            if (typerExpression(p.getDebut()) != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Début du 'pour' doit être ENTIER."));
            }
            if (typerExpression(p.getFin()) != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Fin du 'pour' doit être ENTIER."));
            }
            if (typerExpression(p.getPas()) != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Pas du 'pour' doit être ENTIER."));
            }

            verifierInstruction(p.getCorps());
            return;
        }

        // Si tu ajoutes Declaration plus tard :
        // if (i instanceof Declaration d) { ... }

        throw new ErreurSemantique(msg("Instruction non gérée par l'analyse sémantique : " + i.getClass().getSimpleName()));
    }

    private TypeSimple typerExpression(Expression e) {

        if (e instanceof Nombre) {
            return TypeSimple.ENTIER;
        }

        if (e instanceof Identifiant id) {
            // Dans ton AST actuel, tu utilises Identifiant("true") / ("false") pour bool :
            if ("true".equals(id.getNom()) || "false".equals(id.getNom())) {
                return TypeSimple.BOOLEEN;
            }

            Symbole s = ts.resoudre(id.getNom());
            if (s == null) {
                throw new ErreurSemantique(msg("Identifiant '" + id.getNom() + "' utilisé avant déclaration."));
            }
            return s.getType();
        }

        if (e instanceof ExpressionBinaire b) {
            TypeSimple g = typerExpression(b.getGauche());
            TypeSimple d = typerExpression(b.getDroite());
            String op = b.getOperateur();

            // arithmétique -> ENTIER
            if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    throw new ErreurSemantique(msg("Opérateur '" + op + "' attend ENTIER,ENTIER."));
                }
                return TypeSimple.ENTIER;
            }

            // comparaisons -> BOOLEEN
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    throw new ErreurSemantique(msg("Comparaison '" + op + "' attend ENTIER,ENTIER."));
                }
                return TypeSimple.BOOLEEN;
            }

            // égalité -> BOOLEEN (on accepte ENTIER/ENTIER ou BOOLEEN/BOOLEEN)
            if (op.equals("==") || op.equals("!=")) {
                if (g != d) {
                    throw new ErreurSemantique(msg("Test '" + op + "' attend deux opérandes du même type."));
                }
                return TypeSimple.BOOLEEN;
            }

            // logique -> BOOLEEN
            if (op.equals("&&") || op.equals("||")) {
                if (g != TypeSimple.BOOLEEN || d != TypeSimple.BOOLEEN) {
                    throw new ErreurSemantique(msg("Opérateur logique '" + op + "' attend BOOLEEN,BOOLEEN."));
                }
                return TypeSimple.BOOLEEN;
            }

            throw new ErreurSemantique(msg("Opérateur binaire inconnu : " + op));
        }

        throw new ErreurSemantique(msg("Expression non gérée : " + e.getClass().getSimpleName()));
    }

    private String msg(String details) {
        return "[Fonction " + fonctionCourante + "] " + details;
    }
}
