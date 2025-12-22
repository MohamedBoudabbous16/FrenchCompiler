package utils.lang;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Paire immutable (couple de 2 valeurs).
 *
 * <p>Utilité typique dans un compilateur :
 * <ul>
 *   <li>retourner (valeur, type) ou (noeud, diagnostics)</li>
 *   <li>associer une clé et une valeur sans créer une classe dédiée</li>
 *   <li>stocker (position, token), (nom, info), etc.</li>
 * </ul>
 *
 * <p>Cette classe est volontairement simple :
 * - immutable
 * - null autorisé (géré par equals/hashCode)
 * - sérialisable (optionnel mais pratique)
 */
public final class Pair<A, B> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final A premier;
    private final B second;

    private Pair(A premier, B second) {
        this.premier = premier;
        this.second = second;
    }

    /** Fabrique statique (plus lisible que new Pair<>(...)). */
    public static <A, B> Pair<A, B> of(A premier, B second) {
        return new Pair<>(premier, second);
    }

    /** Premier élément de la paire. */
    public A premier() {
        return premier;
    }

    /** Second élément de la paire. */
    public B second() {
        return second;
    }

    /** Alias anglo-saxon parfois pratique. */
    public A first() {
        return premier;
    }

    /** Alias anglo-saxon parfois pratique. */
    public B secondValue() {
        return second;
    }

    /** Crée une nouvelle paire en remplaçant le premier élément. */
    public Pair<A, B> avecPremier(A nouveauPremier) {
        if (Objects.equals(this.premier, nouveauPremier)) return this;
        return new Pair<>(nouveauPremier, this.second);
    }

    /** Crée une nouvelle paire en remplaçant le second élément. */
    public Pair<A, B> avecSecond(B nouveauSecond) {
        if (Objects.equals(this.second, nouveauSecond)) return this;
        return new Pair<>(this.premier, nouveauSecond);
    }

    /** Indique si au moins un élément est null. */
    public boolean contientNull() {
        return premier == null || second == null;
    }

    @Override
    public String toString() {
        return "Pair[" + premier + ", " + second + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> other)) return false;
        return Objects.equals(premier, other.premier) && Objects.equals(second, other.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(premier, second);
    }
}
