package parseur.ast;

import java.util.List;

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
    public String genJava(semantic.AnalyseSemantique sem) {
        StringBuilder ins = new StringBuilder();

        // Signature (tu gardes Object pour l’instant)
        String javaReturnType = switch (sem.typeRetourDe(nom)) {
            case ENTIER -> "int";
            case BOOLEEN -> "boolean";
            case TEXTE -> "String";
            case CARACTERE -> "char";
            case VIDE -> "void";
            default -> "Object";
        };
        ins.append("public static ").append(javaReturnType).append(" ").append(nom).append("(");
        for (int i = 0; i < param.size(); i++) {
            ins.append("Object ").append(param.get(i));
            if (i < param.size() - 1) ins.append(", ");
        }
        ins.append(") {\n");

        // 1) Déclarations Java des variables inférées (sauf paramètres)
        var vars = sem.variablesDe(nom);
        var loopVars = sem.loopVariablesDe(nom);
        for (var entry : vars.entrySet()) {
            String varName = entry.getKey();
            // Ne pas redéclarer les paramètres
            if (param.contains(varName) || loopVars.contains(varName)) continue;

            String javaType = switch (entry.getValue()) {
                case ENTIER -> "int";
                case BOOLEEN -> "boolean";
                case TEXTE -> "String";
                case CARACTERE -> "char";
                default -> "Object";
            };


            ins.append("  ").append(javaType).append(" ").append(varName).append(";\n");
        }

        // 2) Corps (ton Bloc.genJava() retourne déjà un bloc avec { ... })
        // On enlève les accolades externes pour éviter "{\n{...}\n}\n"
        String corpsJava = corps.genJava(sem).trim();
        if (corpsJava.startsWith("{") && corpsJava.endsWith("}")) {
            corpsJava = corpsJava.substring(1, corpsJava.length() - 1).trim();
        }

        if (!corpsJava.isEmpty()) {
            for (String line : corpsJava.split("\n")) {
                ins.append("  ").append(line).append("\n");
            }
        }

        ins.append("}\n");
        return ins.toString();
    }




}
