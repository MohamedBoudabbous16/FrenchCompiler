package main.java.lexeur;


/**
 * La classe Jeton représente une unité lexicale détectée dans le code source.
 * Un jeton est un élément atomique reconnu par le main.java.lexeur (analyseur lexical).
 *
 * Par exemple :
 * - Le mot-clé "si" devient un jeton de type TypeJeton.Si
 * - Le symbole "+" devient un jeton de type TypeJeton.Plus
 * - Le nombre "42" devient un jeton de type TypeJeton.Nombre
 *
 * Chaque jeton contient :
 * - son type (TypeJeton) qui décrit sa catégorie (mot-clé, opérateur, identifiant, etc.)
 * - sa valeur brute (chaîne de caractères lue dans le texte source)
 * - sa position dans le code (numéro de ligne et de colonne)
 */
public class Jeton {

    // Le type du jeton (ex: Si, Identifiant, Plus, Nombre...)
    private final TypeJeton type;

    // La valeur textuelle du lexème reconnu (ex: "si", "x", "42", "+")
    private final String valeur;

    // La position du jeton dans le fichier source
    private final int ligne;     // numéro de ligne
    private final int colonne;   // position dans la ligne

    // Constructeur
    public Jeton(TypeJeton type, String valeur, int ligne, int colonne) {
        this.type = type;
        this.valeur = valeur;
        this.ligne = ligne;
        this.colonne = colonne;
    }

    //#########################################################################
    // Getters
    public TypeJeton getType() {
        return type;
    }

    public String getValeur() {
        return valeur;
    }

    public int getLigne() {
        return ligne;
    }

    public int getColonne() {
        return colonne;
    }
    //#########################################################################

    //je mes mes VERIFICATEURS icci:
    public boolean estMotCle() {
        return switch (type) {
            case Si, Sinon, TantQue, Pour, Retourne, Fonction, Classe -> true;
            default -> false;
        };
    }
    public boolean estOperateurArithmetique() {
        return switch (type) {
            case Plus, Moins, Mult, Div, Modulo, Incr, Decr -> true;
            default -> false;
        };
    }
    public boolean estOperateurLogique() {
        return type == TypeJeton.Et || type == TypeJeton.Ou || type == TypeJeton.Non;
    }
    public boolean estOperateurComparaison() {
        return switch (type) {
            case Affecte, Egal, PasEgal, Inf, Superieur, InfEgal, SupEgal -> true;
            default -> false;
        };
    }
    public boolean estLitteral() {
        return switch (type) {
            case Nombre, TexteLitteral, CaractereLitteral, Vrai, Faux -> true;
            default -> false;
        };
    }
    public boolean estIdentifiant() {
        return type == TypeJeton.Identifiant;
    }
    //#########################################################################
//#########################################################################
// Méthodes utilitaires redéfinies (affichage, égalité, hachage)


    public String enChaine() {
        StringBuilder sb = new StringBuilder();
        sb.append("###########################################\n");
        sb.append(String.format("│ Type    : %-27s │\n", type));
        sb.append(String.format("│ Valeur  : %-27s │\n", valeur));
        sb.append(String.format("│ Position: ligne %-3d, colonne %-5d │\n", ligne, colonne));
        sb.append("###########################################\n");;
        return sb.toString();
    }
    @Override
    //a verifier si elle doit etre la
    public String toString() {
        return enChaine();
    }



    public boolean estEgalA(Jeton autre) {
        if (autre == null) return false;

        boolean memeValeur = (this.valeur == null && autre.valeur == null)
                || (this.valeur != null && this.valeur.equals(autre.valeur));

        return this.type == autre.type &&
                this.ligne == autre.ligne &&
                this.colonne == autre.colonne &&
                memeValeur;
    }
    //je souhaite eviter l'utilisation de
    // la classe Objects pour le moement


    @Override
    public int hashCode() {
        int res = 17; // nb premier comme valeur arbitraire

        // Hachage du type
        res = 31 * res + (type == null ? 0 : type.hashCode());

        // Hachage de la valeur
        res = 31 * res + (valeur == null ? 0 : valeur.hashCode());

        // Hachage des coordonnées
        res = 31 * res + ligne;
        res = 31 * res + colonne;

        return res;
    }
}

