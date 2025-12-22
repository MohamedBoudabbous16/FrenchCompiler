package utils.text;

import java.util.Objects;

/**
 * Utilitaires liés aux espaces / indentation.
 *
 * Usage typique dans un compilateur :
 * - Indenter proprement du code généré (IR → Java, AST → Java)
 * - Aligner des caret '^' dans les diagnostics
 * - Construire rapidement des préfixes d'indentation stables
 */
public final class StringEspaces {

    /** Indentation par défaut : 2 espaces (cohérent avec ton code généré actuel). */
    public static final int INDENT_DEFAUT = 2;

    private StringEspaces() {}

    // -------------------------------------------------------------------------
    // Répétition basique
    // -------------------------------------------------------------------------

    /**
     * Répète un caractère {@code count} fois.
     * - count <= 0 => ""
     */
    public static String repeter(char c, int count) {
        if (count <= 0) return "";
        char[] arr = new char[count];
        for (int i = 0; i < count; i++) arr[i] = c;
        return new String(arr);
    }

    /**
     * Répète une chaîne {@code s} {@code count} fois.
     * - count <= 0 => ""
     * - s vide => ""
     */
    public static String repeter(String s, int count) {
        if (count <= 0) return "";
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) sb.append(s);
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Espaces / indentation
    // -------------------------------------------------------------------------

    /**
     * Renvoie une chaîne composée de {@code n} espaces.
     * - n <= 0 => ""
     */
    public static String espaces(int n) {
        return repeter(' ', n);
    }

    /**
     * Renvoie une indentation "niveau * pas" espaces.
     * Exemple : indent(3, 2) => "      " (6 espaces)
     */
    public static String indent(int niveau, int pas) {
        if (niveau <= 0) return "";
        if (pas <= 0) pas = INDENT_DEFAUT;
        long total = (long) niveau * (long) pas;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Indentation trop grande: " + total);
        }
        return espaces((int) total);
    }

    /**
     * Renvoie une indentation "niveau * INDENT_DEFAUT".
     */
    public static String indent(int niveau) {
        return indent(niveau, INDENT_DEFAUT);
    }

    // -------------------------------------------------------------------------
    // Alignment caret / colonnes (diagnostics)
    // -------------------------------------------------------------------------

    /**
     * Calcule une chaîne d'espaces permettant d'aligner un caret '^' à une colonne 1-indexée,
     * en tenant compte des tabulations.
     *
     * Convention simple et stable :
     * - une tabulation '\t' compte pour {@code largeurTab} espaces visuels.
     *
     * @param ligne       ligne source brute
     * @param colonne1    colonne 1-indexée (1 = début de ligne)
     * @param largeurTab  largeur visuelle d'une tabulation (ex: 4)
     * @return une chaîne d'espaces à placer avant '^'
     */
    public static String espacesPourCaret(String ligne, int colonne1, int largeurTab) {
        Objects.requireNonNull(ligne, "ligne");

        int col = Math.max(1, colonne1);
        int cible = col - 1;
        int tab = (largeurTab <= 0) ? 4 : largeurTab;

        StringBuilder sb = new StringBuilder(Math.min(cible, 64));

        int visuel = 0;
        for (int i = 0; i < ligne.length() && visuel < cible; i++) {
            char c = ligne.charAt(i);
            if (c == '\t') {
                sb.append(' '.repeat(tab));
                visuel += tab;
            } else {
                sb.append(' ');
                visuel += 1;
            }
        }

        while (visuel < cible) {
            sb.append(' ');
            visuel++;
        }

        return sb.toString();
    }

    /**
     * Surcharge avec tab = 4.
     */
    public static String espacesPourCaret(String ligne, int colonne1) {
        return espacesPourCaret(ligne, colonne1, 4);
    }

    // -------------------------------------------------------------------------
    // Indentation de bloc de texte
    // -------------------------------------------------------------------------

    /**
     * Indente chaque ligne d'un texte par un préfixe donné.
     * - conserve les lignes vides
     * - gère \n et \r\n
     */
    public static String indenterBloc(String texte, String prefixe) {
        if (texte == null || texte.isEmpty()) return texte == null ? "" : texte;
        if (prefixe == null) prefixe = "";

        // Normaliser sans perdre la forme générale :
        // On traite \r\n, puis \n, puis \r restant.
        String normalized = texte.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1); // -1 => conserve dernière ligne vide

        StringBuilder sb = new StringBuilder(texte.length() + prefixe.length() * lines.length);
        for (int i = 0; i < lines.length; i++) {
            sb.append(prefixe).append(lines[i]);
            if (i < lines.length - 1) sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Indente un bloc avec un niveau et un pas en espaces.
     */
    public static String indenterBloc(String texte, int niveau, int pas) {
        return indenterBloc(texte, indent(niveau, pas));
    }

    /**
     * Indente un bloc avec un niveau et INDENT_DEFAUT.
     */
    public static String indenterBloc(String texte, int niveau) {
        return indenterBloc(texte, indent(niveau, INDENT_DEFAUT));
    }
}
