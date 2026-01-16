package tests.utils.io;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import utils.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IOUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void normaliserFinDeLigne() {
        assertNull(IOUtils.normaliserFinDeLigne(null));
        assertEquals("a\nb\nc", IOUtils.normaliserFinDeLigne("a\r\nb\rc"));
    }

    @Test
    void lire_ecrire_texte() throws IOException {
        Path f = tempDir.resolve("a.txt");
        IOUtils.ecrireTexte(f, "hello");
        assertEquals("hello", IOUtils.lireTexte(f));
    }

    @Test
    void ecrireAtomique_et_lireLignes() throws IOException {
        Path f = tempDir.resolve("b.txt");
        IOUtils.ecrireTexteAtomique(f, "x\ny\nz");
        List<String> lines = IOUtils.lireLignes(f);
        assertEquals(List.of("x", "y", "z"), lines);
    }

    @Test
    void lister_et_supprimerRecursif() throws IOException {
        Path d = tempDir.resolve("dir");
        Files.createDirectories(d);
        Files.writeString(d.resolve("a.txt"), "a");
        Files.createDirectories(d.resolve("sub"));
        Files.writeString(d.resolve("sub/b.txt"), "b");

        // listerFichiers = NON récursif : liste "a.txt" et "sub" (dossier)
        List<Path> entries = IOUtils.listerFichiers(d);

        assertTrue(entries.stream().anyMatch(p -> p.getFileName().toString().equals("a.txt")));
        assertTrue(entries.stream().anyMatch(p -> p.getFileName().toString().equals("sub")));

        // b.txt existe bien, mais n'apparaît pas dans la liste non récursive
        assertTrue(Files.exists(d.resolve("sub/b.txt")));

        IOUtils.supprimerRecursif(d);
        assertFalse(Files.exists(d));
    }

}
