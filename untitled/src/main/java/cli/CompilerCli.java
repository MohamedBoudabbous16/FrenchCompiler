package main.java.cli;

import main.java.codegenerator.GenerationResult;
import main.java.codegenerator.JavaGenerator;
import main.java.codegenerator.JavaGeneratorOptions;
import main.java.parseur.AnaSynt;
import main.java.parseur.ast.Programme;
import main.java.semantic.AnalyseSemantique;
import main.java.semantic.ErreurSemantique;
import utils.diag.DiagnosticCollector;
import utils.diag.SourceTexte;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI minimaliste pour compiler un fichier source (.fc ou autre) vers ProgrammePrincipal.java
 * et optionnellement vers un .class.
 *
 * Exemples:
 *   mvn -q -DskipTests package
 *   java -cp target/classes main.java.cli.CompilerCli programme.fc
 *   java -cp target/classes main.java.cli.CompilerCli --class -o out programme.fc
 *   cat programme.fc | java -cp target/classes main.java.cli.CompilerCli --stdout -
 */
public final class CompilerCli {

    // Exit codes (style Unix / sysexits-ish)
    private static final int EXIT_OK = 0;
    private static final int EXIT_DIAGS = 1; // erreurs de compilation (parser/sémantique)
    private static final int EXIT_JAVAC = 2; // javac a échoué
    private static final int EXIT_INTERNAL = 3; // bug / exception inattendue
    private static final int EXIT_USAGE = 64; // mauvaise utilisation

    private static final String DEFAULT_OUT_JAVA = "ProgrammePrincipal.java";

    private CompilerCli() {}

    public static void main(String[] argv) {
        ParseResult pr = Args.parse(argv);

        if (pr.showHelp) {
            // help => stdout
            System.out.print(usage());
            System.exit(EXIT_OK);
            return;
        }

        if (pr.error != null) {
            // usage error => stderr + usage
            System.err.print("ERREUR: " + pr.error + "\n\n");
            System.err.print(usage());
            System.exit(EXIT_USAGE);
            return;
        }

        Args args = pr.args;
        try {
            run(args);
            System.exit(EXIT_OK);
        } catch (Exit e) {
            if (e.message != null && !e.message.isBlank()) {
                System.err.print(e.message);
                if (!e.message.endsWith("\n")) System.err.print("\n");
            }
            System.exit(e.code);
        } catch (Exception e) {
            System.err.println("ERREUR INTERNE: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(EXIT_INTERNAL);
        }
    }

    private static void run(Args args) throws IOException {
        // 1) lire source
        SourceInput in = readSource(args.input);
        String source = in.source;
        String displayName = in.displayName;

        SourceTexte src = new SourceTexte(displayName, source);
        DiagnosticCollector diags = new DiagnosticCollector(src);

        // 2) parsing
        Programme programme = AnaSynt.analyser(source, diags);
        if (programme == null || diags.aDesErreurs()) {
            throw new Exit(EXIT_DIAGS, diags.formatTous());
        }

        // 3) sémantique (sur le MÊME collector pour que tout soit formaté pareil)
        AnalyseSemantique sem = new AnalyseSemantique(diags);
        try {
            sem.verifier(programme);
        } catch (ErreurSemantique ignored) {
            // Certains designs jettent ErreurSemantique, mais les diagnostics existent déjà.
        }
        if (diags.aDesErreurs()) {
            throw new Exit(EXIT_DIAGS, diags.formatTous());
        }

        // 4) génération Java (on réutilise sem, et on désactive la sémantique interne du générateur)
        JavaGenerator gen = new JavaGenerator();
        JavaGeneratorOptions opts = JavaGeneratorOptions.defaults()
                .runSemanticAnalysis(false)
                .semantic(sem);

        GenerationResult res = gen.generate(programme, opts);
        String javaSource = res.getJavaSource();
        if (javaSource == null || javaSource.isBlank()) {
            throw new Exit(EXIT_INTERNAL, "ERREUR INTERNE: génération Java vide.");
        }

        // 5) output Java
        if (args.stdout) {
            System.out.print(javaSource);
            if (!javaSource.endsWith("\n")) System.out.print("\n");
        } else {
            Path outDir = args.outDir.toAbsolutePath().normalize();
            Files.createDirectories(outDir);

            Path javaFile = outDir.resolve(args.outJavaName);
            Files.writeString(javaFile, javaSource, StandardCharsets.UTF_8);

            if (!args.quiet) {
                System.out.println("OK: " + javaFile);
            }

            // 6) compile optionnelle
            if (args.emitClass) {
                compileWithJavac(javaFile, outDir, args.quiet);
            }
        }
    }

    private static void compileWithJavac(Path javaFile, Path outDir, boolean quiet) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new Exit(EXIT_JAVAC,
                    "Impossible de compiler en .class (ToolProvider.getSystemJavaCompiler() == null).\n" +
                            "=> Lance la CLI avec un JDK (pas un JRE).");
        }

        javax.tools.DiagnosticCollector<JavaFileObject> diags = new javax.tools.DiagnosticCollector<>();

        try (var fileManager = compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units =
                    fileManager.getJavaFileObjects(javaFile.toFile());

            // Reco: --release pour éviter warnings modules
            List<String> options = List.of(
                    "--release", "17",
                    "-encoding", "UTF-8",
                    "-d", outDir.toString()
            );

            Boolean ok = compiler.getTask(null, fileManager, diags, options, null, units).call();
            if (ok == null || !ok) {
                StringBuilder sb = new StringBuilder();
                sb.append("Echec javac pour ").append(javaFile).append("\n");
                for (javax.tools.Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
                    sb.append(formatJavacDiagnostic(d)).append("\n");
                }
                throw new Exit(EXIT_JAVAC, sb.toString());
            }
        } catch (IOException e) {
            throw new Exit(EXIT_JAVAC, "Echec javac: " + e.getMessage());
        }

        if (!quiet) {
            System.out.println("OK: .class générés dans " + outDir);
        }
    }

    private static String formatJavacDiagnostic(javax.tools.Diagnostic<? extends JavaFileObject> d) {
        String file = (d.getSource() == null) ? "<unknown>" : d.getSource().getName();
        long line = d.getLineNumber();
        long col = d.getColumnNumber();
        String pos = (line <= 0) ? "" : (":" + line + ":" + Math.max(col, 1));
        return file + pos + ": " + d.getKind() + ": " + d.getMessage(null);
    }

    private static SourceInput readSource(Path inputArg) throws IOException {
        // "-" => stdin
        if (inputArg.toString().equals("-")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return new SourceInput("<stdin>", sb.toString());
        }

        Path input = inputArg.toAbsolutePath().normalize();
        if (!Files.exists(input) || Files.isDirectory(input)) {
            throw new Exit(EXIT_USAGE, "Fichier introuvable: " + input);
        }

        String source = Files.readString(input, StandardCharsets.UTF_8);
        return new SourceInput(input.toString(), source);
    }

    private static String usage() {
        return """
                Usage: compiler [options] <fichier.fc|->

                Entrée:
                  <fichier.fc>   fichier source (UTF-8)
                  -             lit depuis stdin

                Sortie:
                  ProgrammePrincipal.java dans le dossier -o (par défaut), ou vers stdout si --stdout

                Options:
                  -o, --out <dossier>    Dossier de sortie (défaut: .)
                  --out-java <nom>       Nom du fichier Java (défaut: ProgrammePrincipal.java)
                  --class                Compile aussi en .class (via javac)
                  --stdout               Écrit le Java généré sur stdout (ignore -o/--class)
                  -q, --quiet            Mode silencieux
                  -h, --help             Aide
                """;
    }

    // ----------------------------
    // Args parsing (robuste)
    // ----------------------------

    private static final class Args {
        final Path input;
        final Path outDir;
        final String outJavaName;
        final boolean emitClass;
        final boolean quiet;
        final boolean stdout;

        private Args(Path input, Path outDir, String outJavaName, boolean emitClass, boolean quiet, boolean stdout) {
            this.input = input;
            this.outDir = outDir;
            this.outJavaName = outJavaName;
            this.emitClass = emitClass;
            this.quiet = quiet;
            this.stdout = stdout;
        }

        static ParseResult parse(String[] argv) {
            if (argv == null || argv.length == 0) {
                return ParseResult.help();
            }

            Path out = Paths.get(".");
            String outJava = DEFAULT_OUT_JAVA;
            boolean emitClass = false;
            boolean quiet = false;
            boolean stdout = false;

            List<String> positionals = new ArrayList<>();

            for (int i = 0; i < argv.length; i++) {
                String a = argv[i];

                switch (a) {
                    case "-h", "--help" -> { return ParseResult.help(); }
                    case "-q", "--quiet" -> quiet = true;
                    case "--class" -> emitClass = true;
                    case "--stdout" -> stdout = true;

                    case "-o", "--out" -> {
                        if (i + 1 >= argv.length) return ParseResult.error("option " + a + " attend un dossier.");
                        out = Paths.get(argv[++i]);
                    }

                    case "--out-java" -> {
                        if (i + 1 >= argv.length) return ParseResult.error("option --out-java attend un nom de fichier.");
                        outJava = argv[++i];
                        if (!outJava.endsWith(".java")) outJava = outJava + ".java";
                    }

                    default -> {
                        if (a.startsWith("-")) {
                            return ParseResult.error("option inconnue: " + a);
                        }
                        positionals.add(a);
                    }
                }
            }

            if (positionals.size() != 1) {
                return ParseResult.error("il faut exactement 1 fichier en entrée (ou '-')");
            }

            Path input = Paths.get(positionals.get(0));
            return ParseResult.ok(new Args(input, out, outJava, emitClass, quiet, stdout));
        }
    }

    private static final class ParseResult {
        final Args args;
        final boolean showHelp;
        final String error;

        private ParseResult(Args args, boolean showHelp, String error) {
            this.args = args;
            this.showHelp = showHelp;
            this.error = error;
        }

        static ParseResult ok(Args args) { return new ParseResult(args, false, null); }
        static ParseResult help() { return new ParseResult(null, true, null); }
        static ParseResult error(String msg) { return new ParseResult(null, false, msg); }
    }

    private static final class SourceInput {
        final String displayName;
        final String source;

        SourceInput(String displayName, String source) {
            this.displayName = displayName;
            this.source = source;
        }
    }

    private static final class Exit extends RuntimeException {
        final int code;
        final String message;

        Exit(int code, String message) {
            super(message);
            this.code = code;
            this.message = message;
        }
    }
}
