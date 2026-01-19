package main.java.lexeur;

import utils.diag.DiagnosticCollector;
import utils.diag.Position;

import java.io.Reader;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

// La classe Lexeur va devoir créer et retourner des objets
// de type Jeton à chaque fois qu’elle détecte un mot, un nombre, un symbole, ...

public class Lexeur {

    private final DiagnosticCollector diags;

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

        MOTS_CLES.put("affiche", TypeJeton.Affiche);
        MOTS_CLES.put("afficheSansRetourLigne", TypeJeton.AfficheSansRetourLigne);
        MOTS_CLES.put("lire", TypeJeton.Lire);
    }

    private static final Map<String, TypeJeton> SYMBOLES = new HashMap<>();

    static {
        // Arithmétique
        SYMBOLES.put("+", TypeJeton.Plus);
        SYMBOLES.put("-", TypeJeton.Moins);
        SYMBOLES.put("*", TypeJeton.Mult);
        SYMBOLES.put("/", TypeJeton.Div);
        SYMBOLES.put("%", TypeJeton.Modulo);

        // ++ / --
        SYMBOLES.put("++", TypeJeton.Incr);
        SYMBOLES.put("--", TypeJeton.Decr);

        // Affectations
        SYMBOLES.put("=", TypeJeton.Affecte);

        // ✅ Affectations composées
        SYMBOLES.put("+=", TypeJeton.PlusEgal);
        SYMBOLES.put("-=", TypeJeton.MoinsEgal);
        SYMBOLES.put("*=", TypeJeton.MultEgal);
        SYMBOLES.put("/=", TypeJeton.DivEgal);
        SYMBOLES.put("%=", TypeJeton.ModEgal);

        // Comparaisons
        SYMBOLES.put("==", TypeJeton.Egal);
        SYMBOLES.put("!=", TypeJeton.PasEgal);
        SYMBOLES.put("<", TypeJeton.Inf);
        SYMBOLES.put("<=", TypeJeton.InfEgal);
        SYMBOLES.put(">", TypeJeton.Superieur);
        SYMBOLES.put(">=", TypeJeton.SupEgal);

        // Logiques
        SYMBOLES.put("&&", TypeJeton.Et);
        SYMBOLES.put("||", TypeJeton.Ou);
        SYMBOLES.put("!", TypeJeton.Non);

        // Parenthèses / accolades / crochets
        SYMBOLES.put("(", TypeJeton.ParOuvr);
        SYMBOLES.put(")", TypeJeton.ParFerm);
        SYMBOLES.put("{", TypeJeton.AccoladeOuvr);
        SYMBOLES.put("}", TypeJeton.AccoFerma);
        SYMBOLES.put("[", TypeJeton.CrochetOuvrant);
        SYMBOLES.put("]", TypeJeton.CrochetFermant);

        // Séparateurs
        SYMBOLES.put(";", TypeJeton.PointVirgule);
        SYMBOLES.put(",", TypeJeton.Virgule);
        SYMBOLES.put(":", TypeJeton.DeuxPoints);
        SYMBOLES.put(".", TypeJeton.Point);
    }

    public Lexeur(String texte, DiagnosticCollector diags) {
        this.diags = diags;
        this.texte = texte;
        this.position = 0;
        this.ligne = 1;
        this.colonne = 1;
    }

    // Debug: constructeur pour test:
    public Lexeur(Reader reader, DiagnosticCollector diags) {
        this(readAll(reader), diags);
    }

    public Lexeur(Reader reader) {
        this(reader, new DiagnosticCollector());
    }

    public Lexeur(String texte) {
        this(texte, new DiagnosticCollector());
    }

    private static String readAll(Reader r) {
        if (r == null) return "";
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // Fin debug

    public List<Jeton> analyser() {
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
        char c = caractereActuel();

        if (estEspace(c)) {
            avancer(c);
            return null;
        } else if (Character.isLetter(c)) {
            return lireMotOuIdentifiant();
        } else if (Character.isDigit(c)) {
            return lireNombre();
        } else if (c == '"') {
            return lireTexteLitteral();
        } else if (c == '\'') {
            return lireCaractereLitteral();
        } else {
            if (c == '/' && prochainCaractere() == '/') {
                avancer('/');
                avancer('/');
                ignorerCommentaireLigne();
                return null;
            }
            return lireSymbole();
        }
    }

    private Jeton lireMotOuIdentifiant() {
        int colonneDepart = colonne;
        StringBuilder mot = new StringBuilder();

        while (!estTermine() && (Character.isLetterOrDigit(caractereActuel()) || caractereActuel() == '_')) {
            mot.append(caractereActuel());
            avancer(caractereActuel());
        }

        String valeurLue = mot.toString();
        TypeJeton type = MOTS_CLES.getOrDefault(valeurLue, TypeJeton.Identifiant);
        return new Jeton(type, valeurLue, ligne, colonneDepart);
    }

    private Jeton lireNombre() {
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

        // symbole double ?
        if (!estTermine()) {
            char suivant = caractereActuel();
            String doubleSymbole = symbole + suivant;

            if (SYMBOLES.containsKey(doubleSymbole)) {
                avancer(suivant);
                symbole = doubleSymbole;
            }
        }

        if (!SYMBOLES.containsKey(symbole)) {
            diags.erreur("Symbole inconnu : '" + symbole + "'", new Position(ligne, colonneDepart));
            return null;
        }

        return new Jeton(SYMBOLES.get(symbole), symbole, ligne, colonneDepart);
    }

    private Jeton lireTexteLitteral() {
        int ligneDepart = ligne;
        int colonneDepart = colonne;

        avancer('"');

        StringBuilder sb = new StringBuilder();

        while (!estTermine() && caractereActuel() != '"') {
            char c = caractereActuel();

            if (c == '\n') {
                diags.erreur("TexteLitteral non terminé (\" manquant avant fin de ligne)",
                        new Position(ligneDepart, colonneDepart));
                return new Jeton(TypeJeton.TexteLitteral, sb.toString(), ligneDepart, colonneDepart);
            }

            sb.append(c);
            avancer(c);
        }

        if (estTermine()) {
            diags.erreur("TexteLitteral non terminé (\" manquant) à la fin du fichier",
                    new Position(ligneDepart, colonneDepart));
            return new Jeton(TypeJeton.TexteLitteral, sb.toString(), ligneDepart, colonneDepart);
        }

        avancer('"');

        return new Jeton(TypeJeton.TexteLitteral, sb.toString(), ligneDepart, colonneDepart);
    }

    private Jeton lireCaractereLitteral() {
        int ligneDepart = ligne;
        int colonneDepart = colonne;

        avancer('\'');

        if (estTermine() || caractereActuel() == '\n') {
            diags.erreur("CaractereLitteral invalide (fin de ligne/fichier après ')", new Position(ligneDepart, colonneDepart));
            return null;
        }

        char valeur = caractereActuel();
        avancer(valeur);

        if (estTermine() || caractereActuel() == '\n' || caractereActuel() != '\'') {
            diags.erreur("CaractereLitteral non terminé (') manquant)", new Position(ligneDepart, colonneDepart));

            // récupération: avancer jusqu'à trouver ' ou fin de ligne/fichier
            while (!estTermine() && caractereActuel() != '\n' && caractereActuel() != '\'') {
                avancer(caractereActuel());
            }
            if (!estTermine() && caractereActuel() == '\'') {
                avancer('\'');
            }

            return new Jeton(TypeJeton.CaractereLitteral, String.valueOf(valeur), ligneDepart, colonneDepart);
        }

        avancer('\'');

        return new Jeton(TypeJeton.CaractereLitteral, String.valueOf(valeur), ligne, colonneDepart);
    }

    private void ignorerCommentaireLigne() {
        while (!estTermine() && caractereActuel() != '\n') {
            avancer(caractereActuel());
        }
        if (!estTermine() && caractereActuel() == '\n') {
            avancer('\n');
        }
    }

    private char prochainCaractere() {
        if (position + 1 >= texte.length()) return '\0';
        return texte.charAt(position + 1);
    }

    private void avancer() {
        if (estTermine()) return;

        char c = texte.charAt(position);
        avancer(c);
    }

    private void avancer(char caractere) {
        if (estTermine()) return;

        position++;

        if (caractere == '\n') {
            ligne++;
            colonne = 1;
        } else {
            colonne++;
        }
    }

    private boolean estTermine() {
        return position >= texte.length();
    }

    private char caractereActuel() {
        if (estTermine()) {
            return '\0';
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
