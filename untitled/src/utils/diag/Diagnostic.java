package utils.diag;

import utils.text.SourceFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Diagnostic structuré (erreur / avertissement / info) pour un compilateur.
 *
 * Contient :
 * - gravité
 * - code (ex: LEX001, PAR002, SEM010…)
 * - message principal
 * - intervalle (position)
 * - aide (suggestion de correction)
 * - notes (infos additionnelles)
 * - cause (exception optionnelle)
 *
 * Objectif : pouvoir afficher un diagnostic simple OU un diagnostic riche avec extrait du code.
 */
public final class Diagnostic {

    private final Gravite gravite;
    private final String code;         // nullable
    private final String message;      // non-null
    private final Intervalle intervalle; // nullable
    private final String aide;         // nullable
    private final List<String> notes;  // non-null (immutable)
    private final Throwable cause;     // nullable

    private Diagnostic(Builder b) {
        this.gravite = Objects.requireNonNull(b.gravite, "gravite");
        this.code = (b.code == null || b.code.isBlank()) ? null : b.code;
        this.message = Objects.requireNonNull(b.message, "message");
        this.intervalle = b.intervalle;
        this.aide = (b.aide == null || b.aide.isBlank()) ? null : b.aide;
        this.notes = Collections.unmodifiableList(new ArrayList<>(b.notes));
        this.cause = b.cause;
    }

    /* =========================
     *  FABRIQUES PRATIQUES
     * ========================= */

    public static Diagnostic erreur(String message) {
        return builder(Gravite.ERREUR, message).build();
    }

    public static Diagnostic erreur(String message, Intervalle intervalle) {
        return builder(Gravite.ERREUR, message).intervalle(intervalle).build();
    }

    public static Diagnostic avertissement(String message) {
        return builder(Gravite.AVERTISSEMENT, message).build();
    }

    public static Diagnostic avertissement(String message, Intervalle intervalle) {
        return builder(Gravite.AVERTISSEMENT, message).intervalle(intervalle).build();
    }

    public static Diagnostic info(String message) {
        return builder(Gravite.INFO, message).build();
    }

    public static Diagnostic info(String message, Intervalle intervalle) {
        return builder(Gravite.INFO, message).intervalle(intervalle).build();
    }

    public static Builder builder(Gravite gravite, String message) {
        return new Builder(gravite, message);
    }
    public static Diagnostic erreur(String message, Position position) {
        return builder(Gravite.ERREUR, message).position(position).build();
    }

    public static Diagnostic avertissement(String message, Position position) {
        return builder(Gravite.AVERTISSEMENT, message).position(position).build();
    }

    public static Diagnostic info(String message, Position position) {
        return builder(Gravite.INFO, message).position(position).build();
    }


    /* =========================
     *  ACCESSEURS
     * ========================= */

    public Gravite gravite() {
        return gravite;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public Intervalle intervalle() {
        return intervalle;
    }

    public String aide() {
        return aide;
    }

    public List<String> notes() {
        return notes;
    }

    public Throwable cause() {
        return cause;
    }

    public boolean estErreur() {
        return gravite.estErreur();
    }

    /* =========================
     *  FORMATAGE
     * ========================= */

    /**
     * Format simple :
     *   ERREUR PAR001 fichier:12:3 message...
     */
    public String formatSimple() {
        StringBuilder sb = new StringBuilder();
        sb.append(gravite.libelle());

        if (code != null) sb.append(" ").append(code);
        if (intervalle != null) sb.append(" ").append(intervalle);

        sb.append(" : ").append(message);

        if (aide != null) sb.append("\n  aide: ").append(aide);
        for (String n : notes) sb.append("\n  note: ").append(n);

        return sb.toString();
    }

    /**
     * Format riche avec extrait du code et caret '^'.
     * Si le diagnostic n'a pas d'intervalle ou si la ligne n'existe pas, fallback sur formatSimple().
     */
    /**
     * Format riche avec extrait du code et caret '^'.
     * Si le diagnostic n'a pas d'intervalle, si la source est absente,
     * ou si l'extrait ne peut pas être produit, fallback sur formatSimple().
     */
    public String formatAvecSource(SourceTexte source) {
        if (intervalle == null || source == null) return formatSimple();

        Position debut = intervalle.debut();
        if (debut == null) return formatSimple();

        // Préfère le nom du SourceTexte si dispo, sinon celui de la Position
        String nom = (source.nom() != null) ? source.nom() : debut.source();

        StringBuilder sb = new StringBuilder();

        // Entête
        sb.append(gravite.libelle());
        if (code != null) sb.append(" ").append(code);

        if (nom != null) sb.append(" ").append(nom);

        int li = Math.max(1, debut.ligne());
        int co = Math.max(1, debut.colonne());
        sb.append(":").append(li).append(":").append(co);

        sb.append(" : ").append(message).append("\n");

        // Extrait (gère aussi les intervalles multi-lignes / tabs / etc.)
        String extrait = SourceFormatter.formater(source, intervalle);
        if (extrait == null || extrait.isBlank()) return formatSimple();
        sb.append(extrait).append("\n");

        // Aide + notes
        if (aide != null) sb.append("  aide: ").append(aide).append("\n");
        for (String n : notes) sb.append("  note: ").append(n).append("\n");

        return sb.toString().stripTrailing();
    }

    @Deprecated(forRemoval = true)
    private static String espacesPourCaret(String ligne, int colonne1Index) {
        // colonne1Index = 1 => caret au début (0 espaces)
        // pour les '\t', on remplace par 4 espaces visuellement (simple et stable)
        int cible = colonne1Index - 1;
        StringBuilder sb = new StringBuilder();

        int visuel = 0;
        for (int i = 0; i < ligne.length() && visuel < cible; i++) {
            char c = ligne.charAt(i);
            if (c == '\t') {
                int add = 4; // convention d'affichage
                sb.append(" ".repeat(add));
                visuel += add;
            } else {
                sb.append(' ');
                visuel += 1;
            }
        }

        // Si colonne dépasse la ligne, on complète
        while (visuel < cible) {
            sb.append(' ');
            visuel++;
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return formatSimple();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Diagnostic other)) return false;
        return gravite == other.gravite
                && Objects.equals(code, other.code)
                && message.equals(other.message)
                && Objects.equals(intervalle, other.intervalle)
                && Objects.equals(aide, other.aide)
                && notes.equals(other.notes)
                && Objects.equals(cause, other.cause);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gravite, code, message, intervalle, aide, notes, cause);
    }

    /**
     * Source associée à ce diagnostic (souvent un nom de fichier).
     * <p>
     * Remarque : dans cette implémentation, la source est portée par l'intervalle
     * (Position.source()).
     *
     * @return la source (String) ou null si inconnue
     */
    public String source() {
        if (intervalle == null) return null;
        Position debut = intervalle.debut();
        if (debut == null) return null;
        return debut.source();
    }



    /**
     * Crée un Builder pré-rempli avec les valeurs de ce diagnostic.
     * Utile pour "cloner" puis modifier quelques champs.
     */
    public Builder toBuilder() {
        Builder b = builder(this.gravite, this.message);

        if (this.code != null) b.code(this.code);
        if (this.intervalle != null) b.intervalle(this.intervalle);
        if (this.aide != null) b.aide(this.aide);
        for (String n : this.notes) b.note(n);
        if (this.cause != null) b.cause(this.cause);

        return b;
    }


    /* =========================
     *  BUILDER
     * ========================= */

    public static final class Builder {
        private final Gravite gravite;
        private final String message;

        private String code;
        private Intervalle intervalle;
        private String aide;
        private final List<String> notes = new ArrayList<>();
        private Throwable cause;

        private Builder(Gravite gravite, String message) {
            this.gravite = Objects.requireNonNull(gravite, "gravite");
            this.message = Objects.requireNonNull(message, "message");
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder intervalle(Intervalle intervalle) {
            this.intervalle = intervalle;
            return this;
        }

        public Builder position(Position pos) {
            this.intervalle = (pos == null) ? null : Intervalle.at(pos);
            return this;
        }

        public Builder aide(String aide) {
            this.aide = aide;
            return this;
        }

        public Builder note(String note) {
            if (note != null && !note.isBlank()) this.notes.add(note);
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public Diagnostic build() {
            return new Diagnostic(this);
        }
    }
}
