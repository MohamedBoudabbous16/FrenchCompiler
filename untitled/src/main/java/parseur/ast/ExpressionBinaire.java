package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import main.java.semantic.TypeSimple;
import utils.diag.Position;

import java.util.Objects;

public class ExpressionBinaire extends Expression {

    private final Expression gauche;
    private final Expression droite;
    private final String op;

    public ExpressionBinaire(Position pos, Expression gauche, String op, Expression droite) {
        super(pos);
        this.gauche = gauche;
        this.droite = droite;
        this.op = op;
    }

    public Expression getGauche() { return gauche; }
    public Expression getDroite() { return droite; }
    public String getop() { return op; }

    private static boolean isArithNoPlus(String op) {
        return "-".equals(op) || "*".equals(op) || "/".equals(op) || "%".equals(op);
    }

    private static boolean isCompare(String op) {
        return "<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op);
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        Objects.requireNonNull(sem, "sem");

        TypeSimple tg = sem.typeDe(gauche);
        TypeSimple td = sem.typeDe(droite);
        TypeSimple tr = sem.typeDe(this);

        String jg = gauche.genJava(sem);
        String jd = droite.genJava(sem);

        java.util.function.Function<String, String> asInt  = s -> "RuntimeSupport.asInt(" + s + ")";
        java.util.function.Function<String, String> asBool = s -> "RuntimeSupport.asBool(" + s + ")";
        java.util.function.Function<String, String> asStr  = s -> "RuntimeSupport.asString(" + s + ")";

        // LOGIQUE
        if ("&&".equals(op) || "||".equals(op)) {
            if (tg == TypeSimple.INCONNU) jg = asBool.apply(jg);
            if (td == TypeSimple.INCONNU) jd = asBool.apply(jd);
            return "(" + jg + " " + op + " " + jd + ")";
        }

        // ARITH + COMPARE : besoin int
        if (isArithNoPlus(op) || isCompare(op)) {
            if (tg == TypeSimple.INCONNU) jg = asInt.apply(jg);
            if (td == TypeSimple.INCONNU) jd = asInt.apply(jd);
            return "(" + jg + " " + op + " " + jd + ")";
        }

        // EGALITÉ
        if ("==".equals(op) || "!=".equals(op)) {
            boolean useObjectsEquals =
                    tg == TypeSimple.TEXTE || td == TypeSimple.TEXTE ||
                            tg == TypeSimple.INCONNU || td == TypeSimple.INCONNU;

            if (useObjectsEquals) {
                String eq = "java.util.Objects.equals(" + jg + ", " + jd + ")";
                return "==".equals(op) ? eq : "!" + eq;
            }

            // types primitifs mêmes -> == / !=
            if (tg == td && (tg == TypeSimple.ENTIER || tg == TypeSimple.BOOLEEN || tg == TypeSimple.CARACTERE)) {
                return "(" + jg + " " + op + " " + jd + ")";
            }

            // fallback safe
            String eq = "java.util.Objects.equals(" + jg + ", " + jd + ")";
            return "==".equals(op) ? eq : "!" + eq;
        }

        // PLUS : concat / addition / fallback dynamique
        if ("+".equals(op)) {
            if (tr == TypeSimple.TEXTE || tg == TypeSimple.TEXTE) {
                return "(" + asStr.apply(jg) + " + " + asStr.apply(jd) + ")";
            }
            if (tr == TypeSimple.ENTIER) {
                if (tg == TypeSimple.INCONNU) jg = asInt.apply(jg);
                if (td == TypeSimple.INCONNU) jd = asInt.apply(jd);
                return "(" + jg + " + " + jd + ")";
            }
            // ⚠️ nécessite RuntimeSupport.add(Object,Object)
            return "RuntimeSupport.add(" + jg + ", " + jd + ")";
        }

        // fallback : ne pas casser la compilation Java
        return "(" + jg + " " + op + " " + jd + ")";
    }
}
