package parseur.ast;

import semantic.AnalyseSemantique;

import java.util.List;

import static semantic.TypeSimple.CARACTERE;

public class Fonction implements Noeud {
    private final String nom;
    private final List<String> param;
    private final Bloc corps;

    public Fonction(String nom, List<String> param, Bloc corps) {
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
// parseur/ast/Fonction.java (extrait)
public String genJava(AnalyseSemantique sem) {
    StringBuilder ins = new StringBuilder();

    // 1) Déterminer le type de retour (inféré en sémantique)
    String javaReturnType = switch (sem.typeRetourDe(nom)) {
        case ENTIER    -> "int";
        case BOOLEEN   -> "boolean";
        case TEXTE     -> "String";
        case CARACTERE -> "char";
        case VIDE      -> "void";
        default        -> "Object";
    };

    // 2) Signature de la fonction
    ins.append("public static ").append(javaReturnType)
            .append(" ").append(nom).append("(");

    // paramètres non typés (Object par défaut)
    for (int i = 0; i < param.size(); i++) {
        ins.append("Object ").append(param.get(i));
        if (i < param.size() - 1) ins.append(", ");
    }
    ins.append(") {\n");

    // 3) Déclarations des variables locales (hors paramètres et compteurs de boucle)
    var vars     = sem.variablesDe(nom);
    var loopVars = sem.loopVariablesDe(nom);
    for (var entry : vars.entrySet()) {
        String varName = entry.getKey();
        if (param.contains(varName) || loopVars.contains(varName)) continue;

        String javaType = switch (entry.getValue()) {
            case ENTIER    -> "int";
            case BOOLEEN   -> "boolean";
            case TEXTE     -> "String";
            case CARACTERE -> "char";
            default        -> "Object";
        };
        ins.append("  ").append(javaType).append(" ").append(varName).append(";\n");
    }

    // 4) Corps de la fonction (enlevant les accolades externes du bloc)
    String corpsJava = corps.genJava(sem).trim();
    if (corpsJava.startsWith("{") && corpsJava.endsWith("}")) {
        corpsJava = corpsJava.substring(1, corpsJava.length() - 1).trim();
    }
    for (String line : corpsJava.split("\n")) {
        ins.append("  ").append(line).append("\n");
    }

    ins.append("}\n");
    return ins.toString();
}





}
