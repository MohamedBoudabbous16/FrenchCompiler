package parseur.ast;

import java.util.List;

public class Classe implements Noeud{

    private String nom;
    private final List<String> meres;
    private final List<String> prives;
    private final List<String> publics;
    private final List<String> finaux;
    private final List<Fonction> fonctions;

    public Classe(String nom, List<String> meres, List<String> prives, List<String> publics, List<String> finaux,List<Fonction> fonctions) {
        this.nom = nom;
        this.meres = meres;
        this.prives = prives;
        this.publics = publics;
        this.finaux = finaux;
        this.fonctions = fonctions;
    }
    public List<String> getMeres() {
        return meres;
    }


    public String getNom() {
        return nom;
    }

    public List<String> getPrives() {
        return prives;
    }

    public List<String> getPublics() {
        return publics;
    }

    public List<String> getFinaux() {
        return finaux;
    }

    public List<Fonction> getFonctions() {
        return fonctions;
    }
    @Override
    public String genJava() {
        StringBuilder cls = new StringBuilder();
        cls.append("public class ").append(nom);

        if (!meres.isEmpty()) {
            String classePrincipale = meres.getFirst();
            List<String> interfaces = meres.subList(1, meres.size());

            cls.append(" extends ").append(classePrincipale);
            if (!interfaces.isEmpty()) {
                cls.append(" implements ").append(String.join(", ", interfaces));
            }
        }

        cls.append(" {\n\n"); // <-- C'était manquant !

        if (!finaux.isEmpty()) {
            cls.append("  // Variables finales\n");
            for (String var : finaux) {
                cls.append("  public static final Object ").append(var).append(" = null;\n");
            }
            cls.append("\n");
        }

        if (!publics.isEmpty()) {
            cls.append("  // Variables publiques\n");
            for (String var : publics) {
                cls.append("  public Object ").append(var).append(";\n");
            }
            cls.append("\n");
        }

        if (!prives.isEmpty()) {
            cls.append("  // Variables privées\n");
            for (String var : prives) {
                cls.append("  private Object ").append(var).append(";\n");
            }
            cls.append("\n");
        }

        if (!fonctions.isEmpty()) {
            cls.append("  // Méthodes\n");
            for (Fonction f : fonctions) {
                cls.append("  ").append(f.genJava().replaceAll("\n", "\n  ")).append("\n\n");
            }
        }

        cls.append("}\n");
        return cls.toString();
    }
}