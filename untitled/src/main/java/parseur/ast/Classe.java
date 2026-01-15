package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;
import main.java.semantic.TypeSimple;
import utils.diag.Position;

import java.util.List;

/**
 * Représente une classe générée par le compilateur.
 * Elle contient uniquement des fonctions et produit la classe Java correspondante.
 */
public class Classe extends NoeudAst {
    private final String nom;
    private final List<String> meres;
    private final List<String> prives;
    private final List<String> publics;
    private final List<String> finaux;
    private final List<Fonction> fonctions;

    public Classe(Position pos, String nom, List<String> meres, List<String> prives,
                  List<String> publics, List<String> finaux, List<Fonction> fonctions) {
        super(pos);
        this.nom = nom;
        this.meres = meres;
        this.prives = prives;
        this.publics = publics;
        this.finaux = finaux;
        this.fonctions = fonctions;
    }

    public String getNom() {
        return nom;
    }
    public List<String> getMeres()  { return meres;  }
    public List<String> getPrives() { return prives; }
    public List<String> getPublics(){ return publics;}
    public List<String> getFinaux() { return finaux; }
    public List<Fonction> getFonctions() { return fonctions; }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder cls = new StringBuilder();
        cls.append("public class ").append(nom).append(" {\n\n");

        // Déterminer s'il existe une fonction main() dans cette classe
        boolean aUneFonctionMain = false;
        for (Fonction f : fonctions) {
            if ("main".equals(f.getNom())) {
                aUneFonctionMain = true;
                break;
            }
        }

        // Générer la méthode main Java seulement si une fonction main() existe
        if (aUneFonctionMain) {
            cls.append("  public static void main(String[] args) {\n");
            TypeSimple typeMain = sem.typeRetourDe("main");
            if (typeMain == TypeSimple.VIDE) {
                // Si main() est void, on l’appelle sans rien afficher
                cls.append("    main();\n");
            } else {
                // Sinon, on récupère la valeur et on l’affiche
                cls.append("    Object res = main();\n");
                cls.append("    System.out.println(res);\n");
            }
            cls.append("  }\n\n");
        }

        // Générer toutes les fonctions de la classe
        if (!fonctions.isEmpty()) {
            cls.append("  // Méthodes\n");
            for (Fonction f : fonctions) {
                cls.append("  ").append(f.genJava(sem).replaceAll("\n", "\n  ")).append("\n\n");
            }
        }

        cls.append("}\n");
        return cls.toString();
    }
}
