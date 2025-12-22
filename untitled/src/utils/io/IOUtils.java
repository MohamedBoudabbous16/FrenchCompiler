package utils.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Utilitaires d'E/S (fichiers / ressources) pour le compilateur.
 *
 * Objectifs :
 * - Centraliser la lecture/écriture (texte + binaire) avec des méthodes sûres et cohérentes.
 * - Éviter de répéter du code NIO partout (Files.readString, createDirectories, etc.).
 * - Fournir des variantes "checked" (IOException) et "unchecked" (UncheckedIOException).
 *
 * Conventions :
 * - UTF-8 par défaut pour le texte.
 * - Les méthodes "U..." (Unchecked) enveloppent IOException en UncheckedIOException.
 */
public final class IOUtils {

    private IOUtils() {}

    /* =========================
     *  Constantes / Helpers
     * ========================= */

    public static final Charset UTF8 = StandardCharsets.UTF_8;

    public static Path path(String p) {
        Objects.requireNonNull(p, "chemin null");
        return Paths.get(p);
    }

    /**
     * Normalise un texte en remplaçant CRLF et CR par LF.
     * Utile si tes tests comparent des chaînes et que l'OS varie.
     */
    public static String normaliserFinDeLigne(String s) {
        if (s == null) return null;
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    /* =========================
     *  Lecture fichier (texte)
     * ========================= */

    public static String lireTexte(Path fichier) throws IOException {
        return lireTexte(fichier, UTF8);
    }

    public static String lireTexte(Path fichier, Charset charset) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        Objects.requireNonNull(charset, "charset null");
        return Files.readString(fichier, charset);
    }

    public static List<String> lireLignes(Path fichier) throws IOException {
        return lireLignes(fichier, UTF8);
    }

    public static List<String> lireLignes(Path fichier, Charset charset) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        Objects.requireNonNull(charset, "charset null");
        return Files.readAllLines(fichier, charset);
    }

    /* =========================
     *  Écriture fichier (texte)
     * ========================= */

    public static void ecrireTexte(Path fichier, String contenu) throws IOException {
        ecrireTexte(fichier, contenu, UTF8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void ecrireTexte(Path fichier,
                                   String contenu,
                                   Charset charset,
                                   OpenOption... options) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        Objects.requireNonNull(contenu, "contenu null");
        Objects.requireNonNull(charset, "charset null");

        creerDossierParentSiBesoin(fichier);

        // Options par défaut si rien n'est fourni
        OpenOption[] ops = (options == null || options.length == 0)
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                : options;

        Files.writeString(fichier, contenu, charset, ops);
    }

    /**
     * Écriture atomique "best effort" :
     * - écrit dans un fichier temporaire dans le même dossier
     * - puis remplace le fichier cible
     *
     * Avantage : évite de laisser un fichier partiellement écrit si le process crash.
     */
    public static void ecrireTexteAtomique(Path fichier, String contenu) throws IOException {
        ecrireTexteAtomique(fichier, contenu, UTF8);
    }

    public static void ecrireTexteAtomique(Path fichier, String contenu, Charset charset) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        Objects.requireNonNull(contenu, "contenu null");
        Objects.requireNonNull(charset, "charset null");

        creerDossierParentSiBesoin(fichier);

        Path parent = fichier.toAbsolutePath().getParent();
        String prefix = fichier.getFileName().toString();
        if (prefix.length() < 3) prefix = "tmp-" + prefix;

        Path tmp = Files.createTempFile(parent, prefix, ".tmp");
        try {
            Files.writeString(tmp, contenu, charset, StandardOpenOption.TRUNCATE_EXISTING);
            // ATOMIC_MOVE peut échouer selon FS, on fallback en MOVE classique.
            try {
                Files.move(tmp, fichier, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, fichier, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // Si quelque chose a échoué avant le move, on nettoie.
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    /* =========================
     *  Lecture/Écriture binaire
     * ========================= */

    public static byte[] lireOctets(Path fichier) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        return Files.readAllBytes(fichier);
    }

    public static void ecrireOctets(Path fichier, byte[] data, OpenOption... options) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        Objects.requireNonNull(data, "data null");

        creerDossierParentSiBesoin(fichier);

        OpenOption[] ops = (options == null || options.length == 0)
                ? new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
                : options;

        Files.write(fichier, data, ops);
    }

    /* =========================
     *  Ressources (classpath)
     * ========================= */

    /**
     * Lit une ressource du classpath (ex: "/runtime/Support.java") en texte.
     * @param ancre une classe du même module (utile pour classloader)
     * @param resourcePath chemin ressource, avec ou sans '/' initial
     */
    public static String lireRessourceTexte(Class<?> ancre, String resourcePath) throws IOException {
        return lireRessourceTexte(ancre, resourcePath, UTF8);
    }

    public static String lireRessourceTexte(Class<?> ancre, String resourcePath, Charset charset) throws IOException {
        Objects.requireNonNull(ancre, "ancre null");
        Objects.requireNonNull(resourcePath, "resourcePath null");
        Objects.requireNonNull(charset, "charset null");

        String rp = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream in = ancre.getResourceAsStream(rp)) {
            if (in == null) {
                throw new IOException("Ressource introuvable: " + rp);
            }
            return new String(in.readAllBytes(), charset);
        }
    }

    /**
     * Retourne le Path réel d'une ressource si elle est accessible en tant que fichier.
     * Attention : peut échouer si la ressource est dans un JAR.
     */
    public static Path ressourceVersPath(Class<?> ancre, String resourcePath) throws IOException {
        Objects.requireNonNull(ancre, "ancre null");
        Objects.requireNonNull(resourcePath, "resourcePath null");
        String rp = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;

        URL url = ancre.getResource(rp);
        if (url == null) throw new IOException("Ressource introuvable: " + rp);

        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException | FileSystemNotFoundException | IllegalArgumentException e) {
            throw new IOException("Ressource non accessible comme fichier: " + rp + " (" + url + ")", e);
        }
    }

    /* =========================
     *  Dossiers / fichiers
     * ========================= */

    public static void creerDossierParentSiBesoin(Path fichier) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        Path parent = fichier.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    public static boolean existe(Path p) {
        return p != null && Files.exists(p);
    }

    public static void copier(Path src, Path dst, CopyOption... opts) throws IOException {
        Objects.requireNonNull(src, "src null");
        Objects.requireNonNull(dst, "dst null");
        creerDossierParentSiBesoin(dst);

        CopyOption[] options = (opts == null || opts.length == 0)
                ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                : opts;

        Files.copy(src, dst, options);
    }

    /**
     * Supprime récursivement un dossier ou un fichier.
     * Ne lève pas si la cible n'existe pas.
     */
    public static void supprimerRecursif(Path racine) throws IOException {
        if (racine == null || !Files.exists(racine)) return;

        Files.walkFileTree(racine, new SimpleFileVisitor<>() {
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
    }

    /**
     * Liste les fichiers (non récursif) d'un dossier, triés par nom.
     */
    public static List<Path> listerFichiers(Path dossier) throws IOException {
        Objects.requireNonNull(dossier, "dossier null");
        if (!Files.isDirectory(dossier)) return List.of();

        List<Path> res = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dossier)) {
            for (Path p : stream) res.add(p);
        }
        res.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return res;
    }

    /* =========================
     *  Streams (optionnel mais pratique)
     * ========================= */

    public static InputStream ouvrirLecture(Path fichier) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        return new BufferedInputStream(Files.newInputStream(fichier));
    }

    public static OutputStream ouvrirEcriture(Path fichier) throws IOException {
        Objects.requireNonNull(fichier, "fichier null");
        creerDossierParentSiBesoin(fichier);
        return new BufferedOutputStream(Files.newOutputStream(
                fichier, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
    }

    /* =========================
     *  Variantes Unchecked
     * ========================= */

    public static String U_lireTexte(Path fichier) {
        try { return lireTexte(fichier); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static void U_ecrireTexte(Path fichier, String contenu) {
        try { ecrireTexte(fichier, contenu); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static void U_supprimerRecursif(Path racine) {
        try { supprimerRecursif(racine); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
