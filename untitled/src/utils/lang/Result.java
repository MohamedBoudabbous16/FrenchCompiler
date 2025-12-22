package utils.lang;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Résultat typé d'une opération : soit un SUCCÈS avec une valeur, soit un ÉCHEC avec une erreur.
 *
 * <p>Objectifs (compileur-friendly) :
 * - éviter les exceptions pour le flux normal
 * - retourner un diagnostic/erreur riche sans casser le pipeline
 * - composition facile (map / flatMap)
 *
 * <p>Exemples :
 * <pre>
 * Result<Integer, String> r = Result.ok(42);
 * Result<Integer, String> e = Result.err("entrée invalide");
 *
 * int v = r.orElse(0);
 * </pre>
 *
 * @param <T> type de la valeur en cas de succès
 * @param <E> type de l'erreur en cas d'échec
 */
public sealed interface Result<T, E> extends Serializable permits Result.Succes, Result.Echec {

    /* =========================================================================
     *  FABRIQUES
     * ========================================================================= */

    static <T, E> Result<T, E> ok(T valeur) {
        return new Succes<>(valeur);
    }

    static <T, E> Result<T, E> err(E erreur) {
        return new Echec<>(erreur);
    }

    /* =========================================================================
     *  ÉTAT
     * ========================================================================= */

    boolean estSucces();

    default boolean estEchec() {
        return !estSucces();
    }

    /* =========================================================================
     *  ACCÈS
     * ========================================================================= */

    /**
     * Retourne la valeur si succès, sinon lève une exception claire.
     * À utiliser surtout dans les tests ou dans des zones où l'échec est impossible.
     */
    T valeur();

    /**
     * Retourne l'erreur si échec, sinon lève une exception claire.
     */
    E erreur();

    default Optional<T> valeurOptionnelle() {
        return estSucces() ? Optional.ofNullable(valeur()) : Optional.empty();
    }

    default Optional<E> erreurOptionnelle() {
        return estEchec() ? Optional.ofNullable(erreur()) : Optional.empty();
    }

    /* =========================================================================
     *  UTILITAIRES
     * ========================================================================= */

    default T orElse(T defaut) {
        return estSucces() ? valeur() : defaut;
    }

    default T orElseGet(Function<? super E, ? extends T> fournisseur) {
        Objects.requireNonNull(fournisseur, "fournisseur");
        return estSucces() ? valeur() : fournisseur.apply(erreur());
    }

    /**
     * Transforme la valeur (si succès), sinon propage l'échec.
     */
    default <U> Result<U, E> map(Function<? super T, ? extends U> f) {
        Objects.requireNonNull(f, "f");
        if (estSucces()) return Result.ok(f.apply(valeur()));
        return Result.err(erreur());
    }

    /**
     * Transforme l'erreur (si échec), sinon propage le succès.
     */
    default <F> Result<T, F> mapErreur(Function<? super E, ? extends F> f) {
        Objects.requireNonNull(f, "f");
        if (estEchec()) return Result.err(f.apply(erreur()));
        return Result.ok(valeur());
    }

    /**
     * Composition : si succès -> applique f qui renvoie un Result, sinon propage l'échec.
     */
    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> f) {
        Objects.requireNonNull(f, "f");
        if (estSucces()) return Objects.requireNonNull(f.apply(valeur()), "Result retourné par flatMap");
        return Result.err(erreur());
    }

    /**
     * Réduit en une valeur finale (pattern matching simplifié).
     */
    default <R> R plier(Function<? super T, ? extends R> onSucces,
                        Function<? super E, ? extends R> onEchec) {
        Objects.requireNonNull(onSucces, "onSucces");
        Objects.requireNonNull(onEchec, "onEchec");
        return estSucces() ? onSucces.apply(valeur()) : onEchec.apply(erreur());
    }

    /* =========================================================================
     *  IMPLEMENTATIONS
     * ========================================================================= */

    /**
     * Cas succès.
     */
    record Succes<T, E>(T valeur) implements Result<T, E> {
        @Override public boolean estSucces() { return true; }

        @Override public T valeur() { return valeur; }

        @Override
        public E erreur() {
            throw new IllegalStateException("Result est un succès, pas d'erreur disponible.");
        }
    }

    /**
     * Cas échec.
     */
    record Echec<T, E>(E erreur) implements Result<T, E> {
        @Override public boolean estSucces() { return false; }

        @Override
        public T valeur() {
            throw new IllegalStateException("Result est un échec, pas de valeur disponible.");
        }

        @Override public E erreur() { return erreur; }
    }
}
