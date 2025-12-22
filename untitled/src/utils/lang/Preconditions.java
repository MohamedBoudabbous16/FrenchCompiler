package utils.lang;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Préconditions (checks) utilitaires pour sécuriser le code d'un compilateur.
 *
 * <p>Objectif :
 * - Fail-fast (erreurs claires et immédiates)
 * - Messages explicites en français
 * - Aucune dépendance externe
 *
 * <p>Exemples :
 * <pre>
 *   Preconditions.nonNull(programme, "programme");
 *   Preconditions.check(etat == OK, "État invalide");
 *   Preconditions.nonBlank(nom, "nom de fonction");
 * </pre>
 */
public final class Preconditions {

    private Preconditions() {
        // utilitaire
    }

    /* =======================================================================
     *  CHECK GÉNÉRIQUE
     * ======================================================================= */

    public static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void check(boolean condition, Supplier<String> message) {
        if (!condition) throw new IllegalStateException(message == null ? "Précondition violée." : message.get());
    }

    public static void checkArgument(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public static void checkArgument(boolean condition, Supplier<String> message) {
        if (!condition) throw new IllegalArgumentException(message == null ? "Argument invalide." : message.get());
    }

    /* =======================================================================
     *  NULL / ÉTAT
     * ======================================================================= */

    public static <T> T nonNull(T value, String nom) {
        if (value == null) {
            throw new NullPointerException((nom == null || nom.isBlank())
                    ? "Valeur null interdite."
                    : ("Valeur null interdite : " + nom));
        }
        return value;
    }

    public static <T> T nonNull(T value, Supplier<String> message) {
        if (value == null) {
            throw new NullPointerException(message == null ? "Valeur null interdite." : message.get());
        }
        return value;
    }

    public static void checkState(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void checkState(boolean condition, Supplier<String> message) {
        if (!condition) throw new IllegalStateException(message == null ? "État invalide." : message.get());
    }

    /* =======================================================================
     *  CHAÎNES
     * ======================================================================= */

    /**
     * Vérifie que la chaîne n'est ni null, ni vide, ni composée uniquement d'espaces.
     */
    public static String nonBlank(String s, String nom) {
        if (s == null) {
            throw new NullPointerException((nom == null || nom.isBlank())
                    ? "Chaîne null interdite."
                    : ("Chaîne null interdite : " + nom));
        }
        if (s.isBlank()) {
            throw new IllegalArgumentException((nom == null || nom.isBlank())
                    ? "Chaîne vide/interdite."
                    : ("Chaîne vide/interdite : " + nom));
        }
        return s;
    }

    /**
     * Vérifie que la chaîne n'est pas null et n'est pas vide ("").
     * (Autorise des espaces, contrairement à nonBlank).
     */
    public static String nonEmpty(String s, String nom) {
        if (s == null) {
            throw new NullPointerException((nom == null || nom.isBlank())
                    ? "Chaîne null interdite."
                    : ("Chaîne null interdite : " + nom));
        }
        if (s.isEmpty()) {
            throw new IllegalArgumentException((nom == null || nom.isBlank())
                    ? "Chaîne vide interdite."
                    : ("Chaîne vide interdite : " + nom));
        }
        return s;
    }

    /* =======================================================================
     *  COLLECTIONS
     * ======================================================================= */

    public static <T extends Collection<?>> T nonVide(T c, String nom) {
        if (c == null) {
            throw new NullPointerException((nom == null || nom.isBlank())
                    ? "Collection null interdite."
                    : ("Collection null interdite : " + nom));
        }
        if (c.isEmpty()) {
            throw new IllegalArgumentException((nom == null || nom.isBlank())
                    ? "Collection vide interdite."
                    : ("Collection vide interdite : " + nom));
        }
        return c;
    }

    public static <T extends Collection<?>> T tailleMin(T c, int min, String nom) {
        nonNull(c, nom);
        checkArgument(min >= 0, "min doit être >= 0");
        if (c.size() < min) {
            throw new IllegalArgumentException((nom == null || nom.isBlank())
                    ? ("Collection trop petite : taille=" + c.size() + ", min=" + min)
                    : ("Collection trop petite (" + nom + ") : taille=" + c.size() + ", min=" + min));
        }
        return c;
    }

    /* =======================================================================
     *  INDEX / BORNES
     * ======================================================================= */

    public static int index(int index, int size, String nomIndex) {
        if (size < 0) throw new IllegalArgumentException("size doit être >= 0");
        if (index < 0 || index >= size) {
            String label = (nomIndex == null || nomIndex.isBlank()) ? "index" : nomIndex;
            throw new IndexOutOfBoundsException(label + " hors limites : " + index + " (taille=" + size + ")");
        }
        return index;
    }

    public static int positionInclusive(int value, int min, int max, String nom) {
        if (min > max) throw new IllegalArgumentException("min > max : " + min + " > " + max);
        if (value < min || value > max) {
            String label = (nom == null || nom.isBlank()) ? "valeur" : nom;
            throw new IllegalArgumentException(label + " hors bornes : " + value + " (attendu " + min + ".." + max + ")");
        }
        return value;
    }

    public static int positifOuNul(int value, String nom) {
        if (value < 0) {
            String label = (nom == null || nom.isBlank()) ? "valeur" : nom;
            throw new IllegalArgumentException(label + " doit être >= 0, reçu=" + value);
        }
        return value;
    }

    public static int strictementPositif(int value, String nom) {
        if (value <= 0) {
            String label = (nom == null || nom.isBlank()) ? "valeur" : nom;
            throw new IllegalArgumentException(label + " doit être > 0, reçu=" + value);
        }
        return value;
    }

    /* =======================================================================
     *  ÉGALITÉ / INVARIANTS
     * ======================================================================= */

    public static void equals(Object attendu, Object reel, String message) {
        if (!Objects.equals(attendu, reel)) {
            throw new IllegalStateException(message + " (attendu=" + attendu + ", reçu=" + reel + ")");
        }
    }

    public static void notEquals(Object interdit, Object reel, String message) {
        if (Objects.equals(interdit, reel)) {
            throw new IllegalStateException(message + " (valeur interdite=" + interdit + ")");
        }
    }
}
