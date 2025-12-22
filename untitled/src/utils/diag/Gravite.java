package utils.diag;

/**
 * Gravité d'un diagnostic.
 */
public enum Gravite {
    ERREUR(3, "ERREUR"),
    AVERTISSEMENT(2, "AVERTISSEMENT"),
    INFO(1, "INFO");

    private final int niveau;
    private final String libelle;

    Gravite(int niveau, String libelle) {
        this.niveau = niveau;
        this.libelle = libelle;
    }

    public int niveau() {
        return niveau;
    }

    public String libelle() {
        return libelle;
    }

    public boolean estErreur() {
        return this == ERREUR;
    }
}
