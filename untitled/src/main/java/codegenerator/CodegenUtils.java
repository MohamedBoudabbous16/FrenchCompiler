package main.java.codegenerator;

import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;
import main.java.semantic.TypeSimple;

public final class CodegenUtils {
    private CodegenUtils() {}

    public static String indent(String code, String prefix) {
        if (code == null || code.isEmpty()) return "";
        String[] lines = code.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            sb.append(prefix).append(lines[i]);
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    public static boolean existeFonction(Programme p, String nom) {
        for (Classe c : p.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                if (f.getNom().equals(nom)) return true;
            }
        }
        return false;
    }

    /** Détecte si le programme contient l'expression Lire() quelque part. */
    public static boolean contientLire(Programme p) {
        for (Classe c : p.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                if (contientLireInstr(f.getCorps())) return true;
            }
        }
        return false;
    }

    private static boolean contientLireInstr(Instruction i) {
        if (i instanceof Bloc b) {
            for (Instruction x : b.getInstructions()) {
                if (contientLireInstr(x)) return true;
            }
            return false;
        }
        if (i instanceof Affiche a) {
            for (Expression e : a.getExpressions()) {
                if (contientLireExpr(e)) return true;
            }
            return false;
        }
        if (i instanceof Affectation a) return contientLireExpr(a.getExpression());
        if (i instanceof Retourne r) return contientLireExpr(r.getExpression());
        if (i instanceof AppelFonctionInstr afi) return contientLireExpr(afi.getAppel());

        // Contrôle
        if (i instanceof Si s) {
            return contientLireExpr(s.getCondition())
                    || contientLireInstr(s.getAlorsInstr())
                    || (s.getSinonInstr() != null && contientLireInstr(s.getSinonInstr()));
        }
        if (i instanceof TantQue tq) {
            return contientLireExpr(tq.getCondition()) || contientLireInstr(tq.getCorps());
        }
        if (i instanceof Pour p) {
            return contientLireExpr(p.getDebut())
                    || contientLireExpr(p.getFin())
                    || contientLireExpr(p.getPas())
                    || contientLireInstr(p.getCorps());
        }

        return false;
    }

    private static boolean contientLireExpr(Expression e) {
        if (e == null) return false;
        if (e instanceof Lire) return true;
        if (e instanceof ExpressionBinaire b) {
            return contientLireExpr(b.getGauche()) || contientLireExpr(b.getDroite());
        }
        if (e instanceof AppelFonction a) {
            for (Expression arg : a.getArgs()) {
                if (contientLireExpr(arg)) return true;
            }
        }
        return false;
    }

    public static String mapTypeRetour(TypeSimple t) {
        return switch (t) {
            case VIDE -> "void";
            case ENTIER -> "int";
            case BOOLEEN -> "boolean";
            case TEXTE -> "String";
            case CARACTERE -> "char";
            default -> "Object";
        };
    }

    public static String mapTypeVar(TypeSimple t) {
        // Variables locales : on reste simple
        return switch (t) {
            case ENTIER -> "int";
            case BOOLEEN -> "boolean";
            case TEXTE -> "String";
            case CARACTERE -> "char";
            default -> "Object";
        };
    }
}
