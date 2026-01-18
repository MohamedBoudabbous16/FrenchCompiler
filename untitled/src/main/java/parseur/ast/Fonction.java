package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import utils.diag.Position;

import java.util.List;

public class Fonction extends NoeudAst {
    private final String nom;
    private final List<String> param;
    private final Bloc corps;

    public Fonction(Position pos, String nom, List<String> param, Bloc corps) {
        super(pos);
        this.nom = nom;
        this.param = param;
        this.corps = corps;
    }
    public String getNom() {
        return nom;
    }
    public List<String> getParam() {
        return param;
    }
    public Bloc getCorps() {
        return corps;
    }
//    @Override
//    public String genJava() {
//        StringBuilder ins = new StringBuilder();
//        ins.append("public static Object ").append(nom).append("("); // <--- espace ajouté ici
//
//        // Signature des paramètres
//        for (int i = 0; i < param.size(); i++) {
//            ins.append("Object ").append(param.get(i));
//            if (i < param.size() - 1) {
//                ins.append(", ");
//            }
//        }
//
//        ins.append(")");
//
//        // Corps de la fonction
//        ins.append(corps.genJava());
//
//        return ins.toString();
//    }
// main.java.parseur/ast/Fonction.java (extrait)
public String genJava(AnalyseSemantique sem) {
    StringBuilder out = new StringBuilder();

    // 1) Type de retour (inféré)
    String javaReturnType = switch (sem.typeRetourDe(nom)) {
        case ENTIER    -> "int";
        case BOOLEEN   -> "boolean";
        case TEXTE     -> "String";
        case CARACTERE -> "char";
        case VIDE      -> "void";
        default        -> "Object";
    };

    // 2) Signature
    out.append("public static ")
            .append(javaReturnType)
            .append(" ")
            .append(nom)
            .append("(");

    // 2.b) Paramètres typés (fallback Object)
    List<main.java.semantic.TypeSimple> tParams = sem.typesParamsDe(nom);
    if (tParams == null) tParams = java.util.List.of();

    for (int i = 0; i < param.size(); i++) {
        String pName = param.get(i);
        main.java.semantic.TypeSimple t =
                (i < tParams.size() && tParams.get(i) != null)
                        ? tParams.get(i)
                        : main.java.semantic.TypeSimple.INCONNU;

        String javaType = switch (t) {
            case ENTIER    -> "int";
            case BOOLEEN   -> "boolean";
            case TEXTE     -> "String";
            case CARACTERE -> "char";
            default        -> "Object";
        };

        out.append(javaType).append(" ").append(pName);
        if (i < param.size() - 1) out.append(", ");
    }

    out.append(") {\n");

    // 3) Déclarations des variables locales
    // - ordre stable (TreeMap)
    // - exclure paramètres + variables de boucle
    var vars = new java.util.TreeMap<>(sem.variablesDe(nom));
    var loopVars = sem.loopVariablesDe(nom);

    for (var entry : vars.entrySet()) {
        String varName = entry.getKey();

        // paramètres ou compteurs de boucle : déjà gérés ailleurs (param = signature, loop = souvent déclaré dans le for)
        if (param.contains(varName) || loopVars.contains(varName)) continue;

        main.java.semantic.TypeSimple t = entry.getValue();
        if (t == null) t = main.java.semantic.TypeSimple.INCONNU;

        String javaType = switch (t) {
            case ENTIER    -> "int";
            case BOOLEEN   -> "boolean";
            case TEXTE     -> "String";
            case CARACTERE -> "char";
            default        -> "Object";
        };

        out.append("  ").append(javaType).append(" ").append(varName).append(";\n");
    }

    // 4) Corps
    String corpsJava = corps.genJava(sem);
    if (corpsJava == null) corpsJava = "";

    corpsJava = corpsJava.trim();

    // si le bloc renvoie "{ ... }", on enlève juste les accolades externes
    if (corpsJava.startsWith("{") && corpsJava.endsWith("}")) {
        corpsJava = corpsJava.substring(1, corpsJava.length() - 1).trim();
    }

    if (!corpsJava.isEmpty()) {
        // indentation robuste: on indente chaque ligne non vide
        String[] lines = corpsJava.split("\\R", -1);
        for (String line : lines) {
            if (line.isBlank()) {
                out.append("\n");
            } else {
                out.append("  ").append(line.stripTrailing()).append("\n");
            }
        }
    }

    out.append("}\n");
    return out.toString();
}



}
