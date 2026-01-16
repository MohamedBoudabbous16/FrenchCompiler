package java.codegenerator;

import java.parseur.ast.*;
import java.parseur.ast.controle.Pour;
import java.parseur.ast.controle.Si;
import java.parseur.ast.controle.TantQue;

import java.util.List;

public final class AstInspector {

    private AstInspector() {}

    public static boolean usesLire(Programme programme) {
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                if (usesLire(f.getCorps())) return true;
            }
        }
        return false;
    }

    private static boolean usesLire(Instruction instr) {
        if (instr == null) return false;

        if (instr instanceof Bloc b) {
            for (Instruction i : b.getInstructions()) {
                if (usesLire(i)) return true;
            }
            return false;
        }

        if (instr instanceof Affiche a) {
            for (Expression e : a.getExpressions()) {
                if (usesLire(e)) return true;
            }
            return false;
        }

        if (instr instanceof Affectation a) {
            return usesLire(a.getExpression());
        }

        if (instr instanceof Retourne r) {
            return usesLire(r.getExpression());
        }

        if (instr instanceof AppelFonctionInstr afi) {
            return usesLire(afi.getAppel());
        }

        if (instr instanceof Si s) {
            return usesLire(s.getCondition())
                    || usesLire(s.getAlorsInstr())
                    || usesLire(s.getSinonInstr());
        }

        if (instr instanceof TantQue tq) {
            return usesLire(tq.getCondition()) || usesLire(tq.getCorps());
        }

        if (instr instanceof Pour p) {
            return usesLire(p.getDebut())
                    || usesLire(p.getFin())
                    || usesLire(p.getPas())
                    || usesLire(p.getCorps());
        }

        return false;
    }

    private static boolean usesLire(Expression e) {
        if (e == null) return false;

        if (e instanceof Lire) return true;

        if (e instanceof ExpressionBinaire b) {
            return usesLire(b.getGauche()) || usesLire(b.getDroite());
        }

        if (e instanceof AppelFonction a) {
            List<Expression> args = a.getArgs();
            for (Expression arg : args) {
                if (usesLire(arg)) return true;
            }
            return false;
        }

        // Nombre, Texte, Caractere, Identifiant, etc.
        return false;
    }
}
