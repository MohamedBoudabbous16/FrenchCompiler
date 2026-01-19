package main.java.codegenerator;

import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;

public final class RuntimeSupport {

    private RuntimeSupport() {}

    /** Solution robuste : détecte lire() dans l'AST, pas dans le code Java généré. */
    public static boolean programmeUsesLire(Programme programme) {
        if (programme == null) return false;

        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                if (blocUsesLire(f.getCorps())) return true;
            }
        }
        return false;
    }

    private static boolean blocUsesLire(Bloc bloc) {
        for (Instruction i : bloc.getInstructions()) {
            if (instrUsesLire(i)) return true;
        }
        return false;
    }

    private static boolean instrUsesLire(Instruction i) {

        // ✅ NEW : expression statement (x++; a=b=3; x=lire(); etc.)
        if (i instanceof ExpressionInstr ei) {
            return exprUsesLire(ei.getExpression());
        }

        if (i instanceof Bloc b) {
            return blocUsesLire(b);
        }
        if (i instanceof Affiche a) {
            for (Expression e : a.getExpressions()) {
                if (exprUsesLire(e)) return true;
            }
            return false;
        }
        if (i instanceof Affectation a) {
            return exprUsesLire(a.getExpression());
        }
        if (i instanceof Retourne r) {
            return exprUsesLire(r.getExpression());
        }
        if (i instanceof AppelFonctionInstr afi) {
            return exprUsesLire(afi.getAppel());
        }
        if (i instanceof Si s) {
            if (exprUsesLire(s.getCondition())) return true;
            if (instrUsesLire(s.getAlorsInstr())) return true;
            return s.getSinonInstr() != null && instrUsesLire(s.getSinonInstr());
        }
        if (i instanceof TantQue tq) {
            return exprUsesLire(tq.getCondition()) || instrUsesLire(tq.getCorps());
        }
        if (i instanceof Pour p) {
            return exprUsesLire(p.getDebut())
                    || exprUsesLire(p.getFin())
                    || exprUsesLire(p.getPas())
                    || instrUsesLire(p.getCorps());
        }

        return false;
    }

    private static boolean exprUsesLire(Expression e) {
        if (e == null) return false;

        if (e instanceof Lire) return true;

        // ✅ NEW : tes nouvelles expressions
        if (e instanceof ExpressionAffectation a) {
            return exprUsesLire(a.getCible()) || exprUsesLire(a.getValeur());
        }
        if (e instanceof ExpressionUnaire u) {
            return exprUsesLire(u.getExpr());
        }
        if (e instanceof ExpressionPostfix p) {
            return exprUsesLire(p.getExpr());
        }

        // existant
        if (e instanceof ExpressionBinaire b) {
            return exprUsesLire(b.getGauche()) || exprUsesLire(b.getDroite());
        }

        if (e instanceof AppelFonction a) {
            for (Expression arg : a.getArgs()) {
                if (exprUsesLire(arg)) return true;
            }
            return false;
        }

        // Nombre / Texte / Caractere / Identifiant : pas de sous-expressions
        return false;
    }

    public static String lireIntRuntimeChunk(String scannerFieldName,
                                             String scannerInitExpr,
                                             String lireMethodName) {

        return
                "\n  // ===== Runtime built-in =====\n" +
                        "  private static final java.util.Scanner " + scannerFieldName + " = " + scannerInitExpr + ";\n\n" +
                        "  /** Built-in: lire() -> ENTIER */\n" +
                        "  public static int " + lireMethodName + "() {\n" +
                        "    return " + scannerFieldName + ".nextInt();\n" +
                        "  }\n";
    }
}
