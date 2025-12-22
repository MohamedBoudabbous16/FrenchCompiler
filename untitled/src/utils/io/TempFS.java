package utils.io;

import utils.lang.Preconditions;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

/**
 * Système de fichiers temporaire "jetable" pour tests / compilation.
 *
 * <p>But :
 * - créer un répertoire temporaire dédié
 * - y créer des fichiers
 * - lire/écrire facilement
 * - tout supprimer proprement à la fin (close()) même en cas d’échec
 *
 * <p>Usage typique :
 * <pre>
 * try (TempFS fs = TempFS.creer("compiler-tests")) {
 *   Path src = fs.ecrire("input.txt", "fonction main() { retourne 0; }");
 *   // ... utiliser src ...
 * }
 * // => le dossier temp est supprimé
 * </pre>
 */
public final class TempFS implements AutoCloseable {

    private final Path racine;
    private final Charset charsetParDefaut;
    private volatile boolean ferme;

    private TempFS(Path racine, Charset charsetParDefaut) {
        this.racine = Objects.requireNonNull(racine, "racine");
        this.charsetParDefaut = (charsetParDefaut == null) ? StandardCharsets.UTF_8 : charsetParDefaut;
        this.ferme = false;
    }

    /* =========================================================================
     *  FABRIQUES
     * ========================================================================= */

    /**
     * Crée un TempFS avec un répertoire temporaire (UTF-8 par défaut).
     */
    public static TempFS creer(String prefixe) {
        return creer(prefixe, StandardCharsets.UTF_8);
    }

    /**
     * Crée un TempFS avec un répertoire temporaire et un charset par défaut.
     */
    public static TempFS creer(String prefixe, Charset charsetParDefaut) {
        String p = (prefixe == null || prefixe.isBlank()) ? "tempfs" : prefixe.trim();
        try {
            Path dir = Files.createTempDirectory(p + "-");
            return new TempFS(dir, charsetParDefaut);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer un répertoire temporaire (prefixe=" + p + ").", e);
        }
    }

    /**
     * Crée un TempFS basé sur un répertoire existant (utile si tu veux contrôler l’emplacement).
     * Le répertoire est créé si nécessaire.
     */
    public static TempFS depuisRepertoire(Path racine, Charset charsetParDefaut) {
        Preconditions.nonNull(racine, "racine");
        try {
            Files.createDirectories(racine);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le répertoire : " + racine, e);
        }
        return new TempFS(racine, charsetParDefaut);
    }

    /* =========================================================================
     *  ACCÈS / INFOS
     * ========================================================================= */

    public Path racine() {
        return racine;
    }

    public Charset charsetParDefaut() {
        return charsetParDefaut;
    }

    public boolean estFerme() {
        return ferme;
    }

    /* =========================================================================
     *  CHEMINS / CRÉATION
     * ========================================================================= */

    /**
     * Résout un chemin relatif sous la racine du TempFS.
     * Exemple: resolve("a/b.txt") -> <racine>/a/b.txt
     */
    public Path resolve(String relatif) {
        Preconditions.nonBlank(relatif, "chemin relatif");
        verifierOuvert();
        Path p = racine.resolve(relatif);
        // Sécurité : empêche de sortir de la racine avec ../../
        Path norm = p.normalize();
        if (!norm.startsWith(racine.normalize())) {
            throw new IllegalArgumentException("Chemin invalide (sort de la racine) : " + relatif);
        }
        return norm;
    }

    /**
     * Crée un sous-dossier (et ses parents) sous la racine.
     */
    public Path dossier(String relatif) {
        Path d = resolve(relatif);
        try {
            Files.createDirectories(d);
            return d;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier : " + d, e);
        }
    }

    /**
     * Crée un fichier vide (et ses parents) sous la racine.
     */
    public Path fichierVide(String relatif) {
        Path f = resolve(relatif);
        try {
            Path parent = f.getParent();
            if (parent != null) Files.createDirectories(parent);
            if (!Files.exists(f)) Files.createFile(f);
            return f;
        } catch (FileAlreadyExistsException e) {
            return f;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le fichier : " + f, e);
        }
    }

    /**
     * Crée un fichier temporaire dans la racine du TempFS.
     */
    public Path fichierTemporaire(String prefixe, String suffixe) {
        verifierOuvert();
        try {
            String p = (prefixe == null || prefixe.isBlank()) ? "tmp" : prefixe;
            String s = (suffixe == null) ? ".tmp" : suffixe;
            return Files.createTempFile(racine, p + "-", s);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer un fichier temporaire dans : " + racine, e);
        }
    }

    /* =========================================================================
     *  LECTURE / ÉCRITURE
     * ========================================================================= */

    /**
     * Écrit du texte UTF-8 dans un fichier relatif (parents créés).
     * Remplace le contenu si le fichier existe.
     */
    public Path ecrire(String relatif, String contenu) {
        return ecrire(relatif, contenu, charsetParDefaut);
    }

    public Path ecrire(String relatif, String contenu, Charset cs) {
        Preconditions.nonBlank(relatif, "chemin relatif");
        verifierOuvert();

        Path f = resolve(relatif);
        try {
            Path parent = f.getParent();
            if (parent != null) Files.createDirectories(parent);

            Charset charset = (cs == null) ? charsetParDefaut : cs;
            Files.writeString(
                    f,
                    contenu == null ? "" : contenu,
                    charset,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return f;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'écrire dans le fichier : " + f, e);
        }
    }

    /**
     * Lit un fichier relatif en UTF-8.
     */
    public String lire(String relatif) {
        return lire(relatif, charsetParDefaut);
    }

    public String lire(String relatif, Charset cs) {
        Preconditions.nonBlank(relatif, "chemin relatif");
        verifierOuvert();

        Path f = resolve(relatif);
        try {
            Charset charset = (cs == null) ? charsetParDefaut : cs;
            return Files.readString(f, charset);
        } catch (NoSuchFileException e) {
            throw new IllegalStateException("Fichier introuvable : " + f, e);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier : " + f, e);
        }
    }

    /**
     * Copie un fichier dans le TempFS.
     */
    public Path copierDansTemp(Path source, String relatifDest) {
        Preconditions.nonNull(source, "source");
        Preconditions.nonBlank(relatifDest, "destination");
        verifierOuvert();

        Path dest = resolve(relatifDest);
        try {
            Path parent = dest.getParent();
            if (parent != null) Files.createDirectories(parent);
            return Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de copier " + source + " vers " + dest, e);
        }
    }

    /**
     * Supprime un fichier ou un dossier (récursif) dans le TempFS.
     */
    public void supprimer(String relatif) {
        Preconditions.nonBlank(relatif, "chemin relatif");
        verifierOuvert();

        Path p = resolve(relatif);
        supprimerRecursif(p);
    }

    /* =========================================================================
     *  FERMETURE / NETTOYAGE
     * ========================================================================= */

    @Override
    public void close() {
        if (ferme) return;
        ferme = true;
        supprimerRecursif(racine);
    }

    private void verifierOuvert() {
        if (ferme) {
            throw new IllegalStateException("TempFS est déjà fermé. Racine=" + racine);
        }
    }

    private static void supprimerRecursif(Path path) {
        if (path == null) return;
        if (!Files.exists(path)) return;

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // On ne rethrow pas en close() dans un finally si possible, mais ici on reste strict :
            throw new IllegalStateException("Impossible de supprimer récursivement : " + path, e);
        }
    }
}
