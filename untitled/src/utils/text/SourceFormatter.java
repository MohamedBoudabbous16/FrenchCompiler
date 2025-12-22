package utils.text;

import utils.diag.Intervalle;
import utils.diag.Position;
import utils.diag.SourceTexte;

import java.util.Objects;

/**
 * Outils de formatage d'extraits de code source (pour diagnostics).
 *
 * Fournit :
 * - un rendu avec numéros de lignes
 * - une ou plusieurs lignes "caret" (^) pour indiquer une position ou un intervalle
 * - gestion visuelle des tabulations (alignement stable)
 * - tronquage optionnel des lignes longues (en gardant la zone importante visible)
 *
 * Conventions :
 * - lignes / colonnes indexées à partir de 1 (comme Position)
 * - la colonne "c" pointe le caractère "c" (1-indexé)
 */
public final class SourceFormatter {

    private SourceFormatter() {}

    /** Options de rendu (valeurs sûres par défaut). */
    public static final class Options {
        /** Nombre de lignes de contexte avant/après la zone ciblée. */
        public int contexte = 0;

        /** Largeur visuelle d'une tabulation '\t'. */
        public int largeurTab = 4;

        /**
         * Longueur maximale affichée pour une ligne (en "colonnes visuelles").
         * Si <= 0 => pas de tronquage.
         */
        public int longueurMaxLigne = 160;

        /** Afficher les numéros de lignes. */
        public boolean afficherNumeros = true;

        public Options() {}
    }

    /** Rendu simple par défaut (contexte=0, tab=4, maxLine=160, numéros=true). */
    public static String formater(SourceTexte source, Intervalle intervalle) {
        return formater(source, intervalle, new Options());
    }

    /** Rendu configurable. */
    public static String formater(SourceTexte source, Intervalle intervalle, Options opts) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(intervalle, "intervalle");
        Objects.requireNonNull(opts, "opts");

        Position debut = intervalle.debut();
        if (debut == null) return "";

        Position fin = intervalle.fin();
        if (fin == null) fin = debut;

        int ligneDebut = debut.ligne();
        int ligneFin = fin.ligne();

        int nbLignes = source.nbLignes();
        int start = Math.max(1, ligneDebut - Math.max(0, opts.contexte));
        int end = Math.min(nbLignes, ligneFin + Math.max(0, opts.contexte));

        int largeurNo = String.valueOf(end).length();

        StringBuilder sb = new StringBuilder();

        for (int l = start; l <= end; l++) {
            String contenu = source.ligne(l);
            if (contenu == null) contenu = "";

            // Détermine si cette ligne doit être "marquée" (dans l'intervalle)
            boolean dansZone = (l >= ligneDebut && l <= ligneFin);

            // Colonnes (1-indexées) pour la marque sur cette ligne
            int colStart = 1;
            int colEnd = 1;

            if (dansZone) {
                if (l == ligneDebut) colStart = Math.max(1, debut.colonne());
                else colStart = 1;

                if (l == ligneFin) colEnd = Math.max(1, fin.colonne());
                else colEnd = contenu.length() + 1; // jusqu'à "fin de ligne"
            }

            // Tronquage optionnel autour de la zone ciblée
            LigneRendue lr = rendreLigne(contenu, opts, dansZone ? colStart : 1, dansZone ? colEnd : 1);

            // Ligne de texte
            sb.append(prefixeLigne(l, largeurNo, opts.afficherNumeros))
                    .append(lr.texte)
                    .append("\n");

            // Ligne caret si nécessaire
            if (dansZone) {
                sb.append(prefixeCaret(largeurNo, opts.afficherNumeros));

                // placement caret dans la ligne rendue :
                int caretVisuelStart = Math.max(0, lr.caretVisuelStart);
                int caretVisuelSpan = Math.max(1, lr.caretVisuelSpan);

                sb.append(" ".repeat(caretVisuelStart));
                sb.append("^".repeat(caretVisuelSpan));
                sb.append("\n");
            }
        }

        return sb.toString().stripTrailing();
    }

    // -------------------------------------------------------------------------
    // Détails d'implémentation
    // -------------------------------------------------------------------------

    private static String prefixeLigne(int noLigne, int largeurNo, boolean afficherNumeros) {
        if (!afficherNumeros) return "  | ";
        String no = String.valueOf(noLigne);
        return "  " + " ".repeat(Math.max(0, largeurNo - no.length())) + no + " | ";
    }

    private static String prefixeCaret(int largeurNo, boolean afficherNumeros) {
        if (!afficherNumeros) return "  | ";
        return "  " + " ".repeat(largeurNo) + " | ";
    }

    /** Résultat interne : texte (potentiellement tronqué) + position caret en "colonnes visuelles". */
    private static final class LigneRendue {
        final String texte;
        final int caretVisuelStart;
        final int caretVisuelSpan;

        LigneRendue(String texte, int caretVisuelStart, int caretVisuelSpan) {
            this.texte = texte;
            this.caretVisuelStart = caretVisuelStart;
            this.caretVisuelSpan = caretVisuelSpan;
        }
    }

    private static LigneRendue rendreLigne(String ligne, Options opts, int colStart1, int colEnd1) {
        if (ligne == null) ligne = "";

        int tabW = Math.max(1, opts.largeurTab);

        int startVis = visualOffset(ligne, colStart1, tabW);
        int endVis = visualOffset(ligne, colEnd1, tabW);

        int span = Math.max(1, endVis - startVis);

        // Pas de tronquage
        if (opts.longueurMaxLigne <= 0) {
            return new LigneRendue(ligne, startVis, span);
        }

        int fullVis = visualWidth(ligne, tabW);
        int max = opts.longueurMaxLigne;

        if (fullVis <= max) {
            return new LigneRendue(ligne, startVis, span);
        }

        // Fenêtre autour de la zone importante (startVis..endVis)
        int focusMid = startVis + Math.max(0, span / 2);
        int windowStart = clamp(focusMid - max / 3, 0, Math.max(0, fullVis - max));
        int windowEnd = Math.min(fullVis, windowStart + max);

        int iStart = charIndexForVisual(ligne, windowStart, tabW);
        int iEnd = charIndexForVisual(ligne, windowEnd, tabW);

        String slice = ligne.substring(iStart, Math.max(iStart, iEnd));

        boolean cutLeft = windowStart > 0;
        boolean cutRight = windowEnd < fullVis;

        String prefix = cutLeft ? "..." : "";
        String suffix = cutRight ? "..." : "";

        String rendered = prefix + slice + suffix;

        int prefixVis = cutLeft ? 3 : 0;

        int newStartVis = (startVis - windowStart) + prefixVis;
        int newEndVis = (endVis - windowStart) + prefixVis;

        int newSpan = Math.max(1, newEndVis - newStartVis);

        // Si l'utilisateur pointe très loin après la fin de slice, on garde au moins un caret
        if (newStartVis < 0) newStartVis = 0;

        return new LigneRendue(rendered, newStartVis, newSpan);
    }

    /** Largeur visuelle totale d'une ligne. */
    private static int visualWidth(String s, int tabW) {
        int vis = 0;
        for (int i = 0; i < s.length(); i++) {
            vis += (s.charAt(i) == '\t') ? tabW : 1;
        }
        return vis;
    }

    /**
     * Calcule l'offset visuel (en colonnes) avant la colonne 1-indexée.
     * Ex: colonne=1 => 0.
     * Si colonne dépasse la ligne, on "prolonge" par des espaces.
     */
    private static int visualOffset(String line, int colonne1Index, int tabW) {
        int cible = Math.max(0, colonne1Index - 1);
        int vis = 0;

        int maxChars = Math.min(line.length(), cible);
        for (int i = 0; i < maxChars; i++) {
            vis += (line.charAt(i) == '\t') ? tabW : 1;
        }

        // colonne au-delà de la longueur => on complète
        while (vis < cible) vis++;

        return vis;
    }

    /**
     * Donne l'index caractère qui correspond au début de la colonne visuelle "v".
     * (Approx. stable) : on avance jusqu'à atteindre (ou dépasser) v.
     */
    private static int charIndexForVisual(String line, int v, int tabW) {
        if (v <= 0) return 0;

        int vis = 0;
        for (int i = 0; i < line.length(); i++) {
            int w = (line.charAt(i) == '\t') ? tabW : 1;
            if (vis + w > v) return i; // on coupe au bord du char
            vis += w;
            if (vis == v) return i + 1;
        }
        return line.length();
    }

    private static int clamp(int x, int lo, int hi) {
        if (x < lo) return lo;
        if (x > hi) return hi;
        return x;
    }
}
