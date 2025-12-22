package utils.diag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collecteur mutable de diagnostics (erreurs / avertissements / infos).
 *
 * Idée :
 * - tes modules (lexeur, parseur, sémantique, codegen...) ajoutent des diagnostics ici,
 * - puis le "pipeline" décide quoi faire (afficher, arrêter si erreurs, etc.).
 */
public final class DiagnosticCollector {

    private final Diagnostics diagnostics = new Diagnostics();

    /**
     * Source par défaut utilisée si un diagnostic ajouté n'a pas de source explicite.
     * (Optionnel : peut rester null.)
     */
    private SourceTexte sourceParDefaut;

    public DiagnosticCollector() {
        this(null);
    }

    public DiagnosticCollector(SourceTexte sourceParDefaut) {
        this.sourceParDefaut = sourceParDefaut;
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    public SourceTexte sourceParDefaut() {
        return sourceParDefaut;
    }

    public void definirSourceParDefaut(SourceTexte sourceParDefaut) {
        this.sourceParDefaut = sourceParDefaut;
    }

    // -------------------------------------------------------------------------
    // Accès diagnostics
    // -------------------------------------------------------------------------

    /** Retourne le conteneur mutable interne (Diagnostics). */
    public Diagnostics diagnostics() {
        return diagnostics;
    }

    /** Vide tous les diagnostics collectés. */
    public void vider() {
        diagnostics.vider();
    }

    /** Nombre total de diagnostics. */
    public int taille() {
        return diagnostics.tous().size();
    }

    // -------------------------------------------------------------------------
    // Ajout générique
    // -------------------------------------------------------------------------

    /**
     * Ajoute un diagnostic (si null => ignoré).
     * Si le diagnostic n'a pas de source, on tente d'appliquer la source par défaut.
     */
    public void ajouter(Diagnostic d) {
        if (d == null) return;
        diagnostics.ajouter(d);
    }

    // -------------------------------------------------------------------------
    // Ajout par gravité + helpers
    // -------------------------------------------------------------------------

    public void erreur(String message) {
        ajouter(Diagnostic.erreur(message));
    }

    public void erreur(String message, Position position) {
        ajouter(Diagnostic.erreur(message, Intervalle.at(position)));
    }

    public void erreur(String message, Intervalle intervalle) {
        ajouter(Diagnostic.erreur(message, intervalle));
    }

    public void avertissement(String message) {
        ajouter(Diagnostic.avertissement(message));
    }

    public void avertissement(String message, Position position) {
        ajouter(Diagnostic.avertissement(message, Intervalle.at(position)));
    }

    public void avertissement(String message, Intervalle intervalle) {
        ajouter(Diagnostic.avertissement(message, intervalle));
    }

    public void info(String message) {
        ajouter(Diagnostic.info(message));
    }

    public void info(String message, Position position) {
        ajouter(Diagnostic.info(message, Intervalle.at(position)));
    }

    public void info(String message, Intervalle intervalle) {
        ajouter(Diagnostic.info(message, intervalle));
    }

    // -------------------------------------------------------------------------
    // Ajout "avancé" (code + cause, etc.)
    // -------------------------------------------------------------------------

    public void erreur(String code, String message, Position position) {
        Objects.requireNonNull(message, "message");
        Diagnostic d = Diagnostic.erreur(message, Intervalle.at(position)).toBuilder().code(code).build();
        ajouter(d);
    }

    public void erreur(String code, String message, Intervalle intervalle) {
        Objects.requireNonNull(message, "message");
        Diagnostic d = Diagnostic.erreur(message, intervalle).toBuilder().code(code).build();
        ajouter(d);
    }

    public void erreur(String code, String message, Throwable cause) {
        Objects.requireNonNull(message, "message");
        Diagnostic d = Diagnostic.erreur(message).toBuilder().code(code).cause(cause).build();
        ajouter(d);
    }

    public void avertissement(String code, String message, Position position) {
        Objects.requireNonNull(message, "message");
        Diagnostic d = Diagnostic.avertissement(message, Intervalle.at(position)).toBuilder().code(code).build();
        ajouter(d);
    }

    public void info(String code, String message, Position position) {
        Objects.requireNonNull(message, "message");
        Diagnostic d = Diagnostic.info(message, Intervalle.at(position)).toBuilder().code(code).build();
        ajouter(d);
    }

    // -------------------------------------------------------------------------
    // Requêtes / stats
    // -------------------------------------------------------------------------

    public boolean aDesErreurs() {
        for (Diagnostic d : diagnostics.tous()) {
            if (d.gravite() == Gravite.ERREUR) return true;
        }
        return false;
    }

    public int nombreErreurs() {
        int n = 0;
        for (Diagnostic d : diagnostics.tous()) {
            if (d.gravite() == Gravite.ERREUR) n++;
        }
        return n;
    }

    public int nombreAvertissements() {
        int n = 0;
        for (Diagnostic d : diagnostics.tous()) {
            if (d.gravite() == Gravite.AVERTISSEMENT) n++;
        }
        return n;
    }

    public List<Diagnostic> erreurs() {
        List<Diagnostic> out = new ArrayList<>();
        for (Diagnostic d : diagnostics.tous()) {
            if (d.gravite() == Gravite.ERREUR) out.add(d);
        }
        return out;
    }

    public List<Diagnostic> avertissements() {
        List<Diagnostic> out = new ArrayList<>();
        for (Diagnostic d : diagnostics.tous()) {
            if (d.gravite() == Gravite.AVERTISSEMENT) out.add(d);
        }
        return out;
    }

    // -------------------------------------------------------------------------
    // Formatage
    // -------------------------------------------------------------------------

    /**
     * Formate tous les diagnostics avec une source explicite.
     * Si source == null, on tente la sourceParDefaut (sinon formatage "sans extrait").
     */
    public String formatTous(SourceTexte source) {
        SourceTexte src = (source != null) ? source : sourceParDefaut;
        if (src != null) return diagnostics.formatTous(src);

        // Fallback : sans source, on affiche juste toString() (ou formatSansSource si tu en as un)
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics.tous()) {
            sb.append(d.toString()).append("\n");
        }
        return sb.toString().trim();
    }

    /** Formate avec la source par défaut (si disponible). */
    public String formatTous() {
        return formatTous(null);
    }
}
