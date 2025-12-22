package utils.diag;

import java.util.Objects;

/**
 * Position dans un fichier source (ligne/colonne), indexée à partir de 1.
 *
 * Convention:
 * - ligne >= 1
 * - colonne >= 1
 * - source peut être null si inconnu
 */
public final class Position implements Comparable<Position> {

    private final String source; // nom du fichier / unité source (nullable)
    private final int ligne;
    private final int colonne;

    public Position(int ligne, int colonne) {
        this(null, ligne, colonne);
    }

    public Position(String source, int ligne, int colonne) {
        if (ligne < 1) throw new IllegalArgumentException("ligne doit être >= 1");
        if (colonne < 1) throw new IllegalArgumentException("colonne doit être >= 1");
        this.source = (source == null || source.isBlank()) ? null : source;
        this.ligne = ligne;
        this.colonne = colonne;
    }

    public String source() {
        return source;
    }

    public int ligne() {
        return ligne;
    }

    public int colonne() {
        return colonne;
    }

    public Position avecSource(String source) {
        return new Position(source, this.ligne, this.colonne);
    }

    @Override
    public String toString() {
        if (source == null) return ligne + ":" + colonne;
        return source + ":" + ligne + ":" + colonne;
    }

    @Override
    public int compareTo(Position other) {
        int c = Integer.compare(this.ligne, other.ligne);
        if (c != 0) return c;
        return Integer.compare(this.colonne, other.colonne);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position other)) return false;
        return ligne == other.ligne
                && colonne == other.colonne
                && Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, ligne, colonne);
    }
}
