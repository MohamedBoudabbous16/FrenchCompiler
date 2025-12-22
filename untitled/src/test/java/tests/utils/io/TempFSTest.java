package test.java.tests.utils.io;

import org.junit.jupiter.api.Test;
import utils.io.TempFS;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TempFSTest {

    @Test
    void tempfs_ecrire_lire_et_close_supprime() {
        Path racine;

        TempFS fs = TempFS.creer("utils-tests");
        racine = fs.racine();
        assertTrue(Files.exists(racine));

        fs.ecrire("a/b.txt", "hello");
        assertEquals("hello", fs.lire("a/b.txt"));

        fs.close();
        assertTrue(fs.estFerme());
        assertFalse(Files.exists(racine));

        assertThrows(IllegalStateException.class, () -> fs.lire("a/b.txt"));
        assertThrows(IllegalStateException.class, () -> fs.ecrire("x.txt", "nope"));
    }

    @Test
    void tempfs_try_with_resources() {
        Path root;
        try (TempFS fs = TempFS.creer("utils-tests2")) {
            root = fs.racine();
            fs.ecrire("x.txt", "ok");
            assertEquals("ok", fs.lire("x.txt"));
        }
        // après close automatique
        assertFalse(Files.exists(root));
    }
}
