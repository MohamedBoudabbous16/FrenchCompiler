package tests.utils.lang;

import org.junit.jupiter.api.Test;
import utils.lang.Preconditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreconditionsTest {

    @Test
    void nonNull_et_nonBlank() {
        assertEquals("x", Preconditions.nonNull("x", "v"));

        assertThrows(NullPointerException.class, () -> Preconditions.nonNull(null, "v"));
        assertThrows(NullPointerException.class, () -> Preconditions.nonBlank(null, "s"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.nonBlank("   ", "s"));

        assertEquals("ok", Preconditions.nonBlank("ok", "s"));
    }

    @Test
    void nonEmpty_nonVide_tailleMin() {
        assertThrows(IllegalArgumentException.class, () -> Preconditions.nonEmpty("", "s"));
        assertEquals("a", Preconditions.nonEmpty("a", "s"));

        assertThrows(IllegalArgumentException.class, () -> Preconditions.nonVide(List.of(), "c"));
        assertEquals(2, Preconditions.nonVide(List.of(1, 2), "c").size());

        assertThrows(IllegalArgumentException.class, () -> Preconditions.tailleMin(List.of(1), 2, "c"));
        assertEquals(2, Preconditions.tailleMin(List.of(1, 2), 2, "c").size());
    }

    @Test
    void checkState_et_index() {
        assertThrows(IllegalStateException.class, () -> Preconditions.checkState(false, "bad state"));

        assertEquals(0, Preconditions.index(0, 3, "i"));
        assertEquals(2, Preconditions.index(2, 3, "i"));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.index(-1, 3, "i"));
        assertThrows(IndexOutOfBoundsException.class, () -> Preconditions.index(3, 3, "i"));
    }

    @Test
    void positions_et_egalites() {
        assertEquals(5, Preconditions.positionInclusive(5, 1, 10, "x"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.positionInclusive(0, 1, 10, "x"));

        assertEquals(0, Preconditions.positifOuNul(0, "n"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.positifOuNul(-1, "n"));

        assertEquals(1, Preconditions.strictementPositif(1, "n"));
        assertThrows(IllegalArgumentException.class, () -> Preconditions.strictementPositif(0, "n"));

        Preconditions.equals(1, 1, "doit être égal");
        assertThrows(IllegalStateException.class, () -> Preconditions.equals(1, 2, "doit être égal"));

        Preconditions.notEquals(1, 2, "doit être différent");
        assertThrows(IllegalStateException.class, () -> Preconditions.notEquals(1, 1, "doit être différent"));
    }
}
