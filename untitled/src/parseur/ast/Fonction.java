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
    @Override
    public String genJava() {
        StringBuilder ins = new StringBuilder();
        ins.append("public static Object ").append(nom).append("("); // <--- espace ajouté ici

        // Signature des paramètres
        for (int i = 0; i < param.size(); i++) {
            ins.append("Object ").append(param.get(i));
            if (i < param.size() - 1) {
                ins.append(", ");
            }
        }

        ins.append(")");

        // Corps de la fonction
        ins.append(corps.genJava());

        return ins.toString();
    }

}
