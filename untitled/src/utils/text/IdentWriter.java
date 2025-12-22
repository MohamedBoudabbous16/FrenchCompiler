package utils.text;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Écrivain de texte avec gestion d'indentation.
 *
 * <p>Cas d'usage typiques dans un compilateur :
 * - génération de code (Java, IR, etc.)
 * - production de sorties lisibles (debug, pretty-print)
 * - construction progressive de chaînes multi-lignes
 *
 * <p>Caractéristiques :
 * - indentation automatique en début de ligne
 * - indent()/dedent() avec garde-fous
 * - helpers ligne(), nl(), blocIndenté(...)
 * - compatible avec \n (on normalise en interne)
 */
public final class IdentWriter {

    /** Indentation appliquée à chaque niveau (par défaut : 2 espaces). */
    private final String uniteIndent;

    private final StringBuilder sb = new StringBuilder();

    /** Niveau d'indentation courant (>= 0). */
    private int niveau = 0;

    /** Vrai si on est au début d'une ligne (donc indentation à injecter). */
    private boolean debutDeLigne = true;

    /** Crée un writer avec 2 espaces par niveau. */
    public IdentWriter() {
        this("  ");
    }

    /** Crée un writer avec une unité d'indentation personnalisée (ex: "\t" ou "    "). */
    public IdentWriter(String uniteIndent) {
        this.uniteIndent = Objects.requireNonNull(uniteIndent, "uniteIndent");
    }

    /* =========================================================================
     *  Indentation
     * ========================================================================= */

    public int niveau() {
        return niveau;
    }

    public IdentWriter setNiveau(int niveau) {
        if (niveau < 0) throw new IllegalArgumentException("niveau < 0");
        this.niveau = niveau;
        return this;
    }

    public IdentWriter indent() {
        niveau++;
        return this;
    }

    public IdentWriter dedent() {
        if (niveau == 0) {
            // Garde-fou : on ne descend jamais sous 0
            return this;
        }
        niveau--;
        return this;
    }

    /**
     * Exécute une action avec un niveau d'indentation augmenté,
     * puis revient automatiquement au niveau initial (même si exception).
     */
    public IdentWriter avecIndent(Consumer<IdentWriter> action) {
        Objects.requireNonNull(action, "action");
        indent();
        try {
            action.accept(this);
        } finally {
            dedent();
        }
        return this;
    }

    /* =========================================================================
     *  Écriture
     * ========================================================================= */

    public IdentWriter append(Object o) {
        return append(String.valueOf(o));
    }

    /**
     * Ajoute du texte. Si le texte contient des '\n', l'indentation est gérée
     * correctement pour les lignes suivantes.
     */
    public IdentWriter append(String texte) {
        if (texte == null || texte.isEmpty()) return this;

        // On normalise: on traite caractère par caractère pour gérer les \n.
        for (int i = 0; i < texte.length(); i++) {
            char c = texte.charAt(i);

            if (debutDeLigne) {
                injecterIndentation();
                debutDeLigne = false;
            }

            sb.append(c);

            if (c == '\n') {
                debutDeLigne = true;
            }
        }
        return this;
    }

    /** Ajoute une nouvelle ligne (un '\n'). */
    public IdentWriter nl() {
        sb.append('\n');
        debutDeLigne = true;
        return this;
    }

    /** Ajoute une ligne complète (texte + '\n'). */
    public IdentWriter ligne(String texte) {
        append(texte);
        nl();
        return this;
    }

    /** Ajoute une ligne vide. */
    public IdentWriter ligneVide() {
        return nl();
    }

    /**
     * Ajoute une ligne formatée comme String.format(format, args) + '\n'.
     */
    public IdentWriter lignef(String format, Object... args) {
        Objects.requireNonNull(format, "format");
        return ligne(String.format(format, args));
    }

    /**
     * Ajoute un bloc :
     * - écrit "enTete" puis '\n'
     * - indent() ; exécute bloc ; dedent()
     * - écrit "fin" si non-null
     */
    public IdentWriter bloc(String enTete, Consumer<IdentWriter> bloc, String fin) {
        Objects.requireNonNull(bloc, "bloc");
        if (enTete != null && !enTete.isEmpty()) {
            ligne(enTete);
        }
        avecIndent(bloc);
        if (fin != null && !fin.isEmpty()) {
            ligne(fin);
        }
        return this;
    }

    /**
     * Variante pratique : écrit "{" puis bloc indenté puis "}".
     */
    public IdentWriter blocAccolades(Consumer<IdentWriter> bloc) {
        Objects.requireNonNull(bloc, "bloc");
        ligne("{");
        avecIndent(bloc);
        ligne("}");
        return this;
    }

    /* =========================================================================
     *  Accès / utilitaires
     * ========================================================================= */

    public int longueur() {
        return sb.length();
    }

    public boolean estVide() {
        return sb.length() == 0;
    }

    public IdentWriter vider() {
        sb.setLength(0);
        niveau = 0;
        debutDeLigne = true;
        return this;
    }

    /**
     * Retourne le contenu construit.
     * Remarque : ne force pas un '\n' final.
     */
    @Override
    public String toString() {
        return sb.toString();
    }

    /** Retourne l'unité d'indentation (ex: "  "). */
    public String uniteIndent() {
        return uniteIndent;
    }

    /* =========================================================================
     *  Interne
     * ========================================================================= */

    private void injecterIndentation() {
        if (niveau <= 0) return;
        // Injection d'indentation stable : niveau * uniteIndent
        for (int i = 0; i < niveau; i++) {
            sb.append(uniteIndent);
        }
    }
}
