package utils.diag;

import java.util.Objects;

/**
 * Intervalle dans le texte source: début -> fin (fin optionnelle).
 *
 * Utilisation:
 * - Intervalle.at(pos) pour un point unique
 * - new Intervalle(debut, fin) si tu connais les deux
 */
public final class Intervalle {

    private final Position debut;
    private final Position fin; // nullable

    public Intervalle(Position debut, Position fin) {
        this.debut = Objects.requireNonNull(debut, "debut");
        this.fin = fin;
    }

    public static Intervalle at(Position pos) {
        return new Intervalle(pos, null);
    }

    public Position debut() {
        return debut;
    }

    public Position fin() {
        return fin;
    }

    public boolean aUneFin() {
        return fin != null;
    }

    @Override
    public String toString() {
        if (fin == null) return debut.toString();
        // Si même fichier, on évite de répéter
        if (Objects.equals(debut.source(), fin.source())) {
            String src = debut.source();
            String left = (src == null) ? (debut.ligne() + ":" + debut.colonne())
                    : (src + ":" + debut.ligne() + ":" + debut.colonne());
            return left + "-" + fin.ligne() + ":" + fin.colonne();
        }
        return debut + "-" + fin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Intervalle other)) return false;
        return debut.equals(other.debut) && Objects.equals(fin, other.fin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(debut, fin);
    }
}
