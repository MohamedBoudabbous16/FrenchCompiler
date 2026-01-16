package java.optimizer;

import java.parseur.ast.*;

final class ConstEval {

    private ConstEval() {}

    static Boolean tryEvalBoolean(Expression e) {
        if (e instanceof Identifiant id) {
            if ("true".equals(id.getNom())) return true;
            if ("false".equals(id.getNom())) return false;
        }
        return null;
    }

    static Integer tryEvalInt(Expression e) {
        if (e instanceof Nombre n) {
            // On tente différentes conventions via réflexion, sinon null.
            Object v = AstReflection.tryCallGetter(n, "getValeur");
            if (v instanceof Integer) return (Integer) v;
            v = AstReflection.tryCallGetter(n, "getValue");
            if (v instanceof Integer) return (Integer) v;

            // fallback : champ "valeur"/"value"
            Object f = AstReflection.tryGetField(n, "valeur");
            if (f instanceof Integer) return (Integer) f;
            f = AstReflection.tryGetField(n, "value");
            if (f instanceof Integer) return (Integer) f;
        }
        return null;
    }

    static String tryEvalString(Expression e) {
        if (e instanceof Texte t) {
            Object v = AstReflection.tryCallGetter(t, "getValeur");
            if (v instanceof String) return (String) v;
            v = AstReflection.tryCallGetter(t, "getValue");
            if (v instanceof String) return (String) v;

            Object f = AstReflection.tryGetField(t, "valeur");
            if (f instanceof String) return (String) f;
            f = AstReflection.tryGetField(t, "value");
            if (f instanceof String) return (String) f;
        }
        return null;
    }

    static ConstValue tryEvalBinary(String op, Expression g, Expression d) {
        Integer gi = tryEvalInt(g);
        Integer di = tryEvalInt(d);
        String gs = tryEvalString(g);
        String ds = tryEvalString(d);
        Boolean gb = tryEvalBoolean(g);
        Boolean db = tryEvalBoolean(d);

        // TEXTE + TEXTE (ou autre déjà converti par ton langage) -> TEXTE
        if ("+".equals(op) && gs != null && ds != null) {
            return ConstValue.text(gs + ds);
        }

        // Arithmétique ENTIER
        if (gi != null && di != null) {
            return switch (op) {
                case "+" -> ConstValue.integer(gi + di);
                case "-" -> ConstValue.integer(gi - di);
                case "*" -> ConstValue.integer(gi * di);
                case "/" -> (di == 0) ? null : ConstValue.integer(gi / di);
                case "%" -> (di == 0) ? null : ConstValue.integer(gi % di);
                case "<" -> ConstValue.bool(gi < di);
                case "<=" -> ConstValue.bool(gi <= di);
                case ">" -> ConstValue.bool(gi > di);
                case ">=" -> ConstValue.bool(gi >= di);
                case "==" -> ConstValue.bool(gi.equals(di));
                case "!=" -> ConstValue.bool(!gi.equals(di));
                default -> null;
            };
        }

        // Logique BOOLEEN
        if (gb != null && db != null) {
            return switch (op) {
                case "&&" -> ConstValue.bool(gb && db);
                case "||" -> ConstValue.bool(gb || db);
                case "==" -> ConstValue.bool(gb.equals(db));
                case "!=" -> ConstValue.bool(!gb.equals(db));
                default -> null;
            };
        }

        // Egalité TEXTE
        if (gs != null && ds != null) {
            return switch (op) {
                case "==" -> ConstValue.bool(gs.equals(ds));
                case "!=" -> ConstValue.bool(!gs.equals(ds));
                default -> null;
            };
        }

        return null;
    }

    static Expression buildConstExpression(ConstValue cv) {
        if (cv == null) return null;

        return switch (cv.kind) {
            case INT -> AstReflection.tryConstruct(Nombre.class, cv.intValue);
            case TEXT -> AstReflection.tryConstruct(Texte.class, cv.textValue);
            case BOOL -> {
                // bool : ton AST encode par Identifiant("true"/"false")
                yield AstReflection.tryConstruct(Identifiant.class, cv.boolValue ? "true" : "false");
            }
        };
    }
}
