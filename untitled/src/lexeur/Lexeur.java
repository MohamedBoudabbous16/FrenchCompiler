package lexeur;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;


//La classe Lexeur va devoir créer et retourner des objets
// de type Jeton à chaque fois qu’elle détecte un mot,un nombre, un symbole,..

public class Lexeur {
    //pour le moement pas de getters ni de setters
    private final String texte;
    private int position;
    private int ligne;
    private int colonne;

    private static final Map<String, TypeJeton> MOTS_CLES = new HashMap<>();

    static {
        MOTS_CLES.put("si", TypeJeton.Si);
        MOTS_CLES.put("sinon", TypeJeton.Sinon);
        MOTS_CLES.put("tantque", TypeJeton.TantQue);
        MOTS_CLES.put("retourne", TypeJeton.Retourne);
        MOTS_CLES.put("pour", TypeJeton.Pour);
        MOTS_CLES.put("fonction", TypeJeton.Fonction);
        MOTS_CLES.put("classe", TypeJeton.Classe);
        MOTS_CLES.put("vrai", TypeJeton.Vrai);
        MOTS_CLES.put("faux", TypeJeton.Faux);
    }
    private static final Map<String, TypeJeton> SYMBOLES = new HashMap<>();

    static {
        SYMBOLES.put("+", TypeJeton.Plus);
        SYMBOLES.put("-", TypeJeton.Moins);
        SYMBOLES.put("*", TypeJeton.Mult);
        SYMBOLES.put("/", TypeJeton.Div);
        SYMBOLES.put("%", TypeJeton.Modulo);
        SYMBOLES.put("=", TypeJeton.Affecte);
        SYMBOLES.put("==", TypeJeton.Egal);
        SYMBOLES.put("!=", TypeJeton.PasEgal);
        SYMBOLES.put("<", TypeJeton.Inf);
        SYMBOLES.put("<=", TypeJeton.InfEgal);
        SYMBOLES.put(">", TypeJeton.Superieur);
        SYMBOLES.put(">=", TypeJeton.SupEgal);
        SYMBOLES.put("&&", TypeJeton.Et);
        SYMBOLES.put("||", TypeJeton.Ou);
        SYMBOLES.put("!", TypeJeton.Non);
        SYMBOLES.put("(", TypeJeton.ParOuvr);
        SYMBOLES.put(")", TypeJeton.ParFerm);
        SYMBOLES.put("{", TypeJeton.AccoladeOuvr);
        SYMBOLES.put("}", TypeJeton.AccoFerma);
        SYMBOLES.put("[", TypeJeton.CrochetOuvrant);
        SYMBOLES.put("]", TypeJeton.CrochetFermant);
        SYMBOLES.put(";", TypeJeton.PointVirgule);
        SYMBOLES.put(",", TypeJeton.Virgule);
        SYMBOLES.put(":", TypeJeton.DeuxPoints);
        SYMBOLES.put(".", TypeJeton.Point);
    }



    public Lexeur(String texte) {
        this.texte = texte;
        this.position = 0;
        this.ligne = 1;
        this.colonne = 1;
    }

    public List<Jeton> analyser() {
    /**
     * Analyse le texte source et génère une liste de jetons.
     * Cette méthode constitue le point d'entrée principal de l'analyse lexicale.
     * Elle parcourt le texte caractère par caractère et identifie les lexèmes
     * pour construire des objets Jeton correspondants aux mots-clés, identifiants,
     * opérateurs, symboles, nombres, etc.
     *
     * @return une liste de jetons représentant le code source analysé
     */
        List<Jeton> jetons = new ArrayList<>();

        while (!estTermine()) {
            char caractere = caractereActuel();

            if (estEspace(caractere)) {
                avancer(caractere);
                continue;
            }

            Jeton jeton = lireJeton();

            if (jeton != null) {
                jetons.add(jeton);
            }
        }

        jetons.add(new Jeton(TypeJeton.FinFichier, "", ligne, colonne));
        return jetons;
    }

    private Jeton lireJeton() {
        /**
         * Lit le caractère courant et retourne le jeton correspondant.
         * Cette méthode détermine le type d’élément à lire (mot, nombre, symbole, etc.)
         * en fonction du caractère courant, et délègue le traitement à la méthode appropriée.
         * Elle ignore les espaces et les caractères non significatifs.
         *
         * @return un objet Jeton correspondant à l’élément reconnu, ou null si ignoré
         */
        char c = caractereActuel();
        if (estEspace(c)) {// j'ignore les espaces
            avancer(c);
            return null;
        }else if (Character.isLetter(c)) {
            return lireMotOuIdentifiant();
        } else if (Character.isDigit(c)) {
            return lireNombre();
        } else {
            return lireSymbole();
        }

    }

    private Jeton lireMotOuIdentifiant() {
        /**Lire une suite de lettres et/ou de chiffres pour
         * construire un mot complet, et déterminer si c’est un mot-clé
         * (si, retourne, pour, etc.) ou un identifiant (comme compteur,
         * somme, x…).
         *@return un objet Jeton
         */
        int colonneDepart = colonne;
        StringBuilder mot = new StringBuilder();
        //je dois verifier la condition while icci  caractereActuel() == '_'
        while (!estTermine() && (Character.isLetterOrDigit(caractereActuel()) || caractereActuel() == '_')) {
            mot.append(caractereActuel());
            avancer(caractereActuel());
        }
        String valeurLue = mot.toString();
        //si valeurLue est un mot-clé (présent dans MOTS_CLES), prends son TypeJeton.
        //Sinon, considère que c’est un Identifiant
        TypeJeton type = MOTS_CLES.getOrDefault(valeurLue, TypeJeton.Identifiant);
        return new Jeton(type, valeurLue, ligne, colonneDepart);


    }

    private Jeton lireNombre() {
    /** Lire une suite de chiffres pour construire un nombre entier.
     * Exemple : "42", "100", "7"
     * @return un objet Jeton de type Nombre
     */
        int colonneDepart = colonne;
        StringBuilder nombre = new StringBuilder();
        while (!estTermine() && Character.isDigit(caractereActuel())) {
            nombre.append(caractereActuel());
            avancer(caractereActuel());
        }
        String valeurLue = nombre.toString();
        return new Jeton(TypeJeton.Nombre, valeurLue, ligne, colonneDepart);
    }

    private Jeton lireSymbole() {
        int colonneDepart = colonne;
        char premier = caractereActuel();
        avancer(premier);
        String symbole = String.valueOf(premier);
        // symbole double?
        if (!estTermine()) {
            char suivant = caractereActuel();
            String doubleSymbole = symbole + suivant;
            if (SYMBOLES.containsKey(doubleSymbole)) {
                avancer(suivant);
                symbole = doubleSymbole;
            }
        }if (!SYMBOLES.containsKey(symbole)) {// si i ln'existe pas dans ma map je retourne un  mesage d'erreur
            throw new RuntimeException("Symbole inconnu : '" + symbole + "' à la ligne " + ligne + ", colonne " + colonneDepart);
        }
        return new Jeton(SYMBOLES.get(symbole), symbole, ligne, colonneDepart);
    }

    private void avancer() {
    /**avance d’un seul caractère dans le texte tout en mettant à jour la position, la ligne et la colonne
     * @return void
     */
        if (estTermine()) return;

        char c = texte.charAt(position);
        position++;

        if (position + 3 <= texte.length() &&
                texte.charAt(position) == '/' &&
                texte.charAt(position + 1) == 'l' &&
                texte.charAt(position + 2) == 's' &&
                texte.charAt(position + 3) == '/') {
            position += 4;
            ligne++;
            colonne = 1;
        } else {
            colonne++;
        }
    }
    private void avancer(char caractere) {
        if (estTermine()) return;
        position++;
        colonne++;
    }

    private boolean estTermine() {
        return position >= texte.length();
    }

    private char caractereActuel() {
        if (estTermine()) {
            return '\0'; // caractère vide spécial  pour indiquer la fin
        }
        return texte.charAt(position);
    }


    private boolean estEspace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';

    }

    private boolean estMotCle(String mot) {
        return MOTS_CLES.containsKey(mot);
    }
}
