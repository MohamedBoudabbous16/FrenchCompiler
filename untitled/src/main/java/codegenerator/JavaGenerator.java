package main.java.codegenerator;

import main.java.parseur.ast.Programme;
import main.java.semantic.AnalyseSemantique;


import java.util.Objects;

/**
 * Générateur Java de haut niveau.
 *
 * Cette classe encapsule la logique de génération du code Java à partir
 * d’un programme AST et d’une analyse sémantique.  Elle peut être
 * étendue ultérieurement pour gérer différents styles de génération.
 */
public class JavaGenerator {

    /**
     * Génère le code Java complet pour un programme donné.
     *
     * @param programme l’AST du programme à compiler

     * @return une chaîne contenant le code source Java correspondant
     */
    public GenerationResult generate(Programme programme) {
    return generate(programme, JavaGeneratorOptions.defaults());
    }

    public GenerationResult generate(Programme programme, JavaGeneratorOptions options) {
    Objects.requireNonNull(programme, "programme");
    Objects.requireNonNull(options, "options");

    // 1) Analyse sémantique (si tu veux que le générateur la fasse)
    AnalyseSemantique sem = options.isRunSemanticAnalysis()
            ? runSemantic(programme)
            : options.getSemanticOrThrow();

    // 2) Génération via ton AST existant
    String source = programme.genJava(sem);

    // 3) Runtime (Scanner / lire()) si besoin
    boolean needsLireRuntime = options.isForceLireRuntime()
            || RuntimeSupport.sourceUsesLire(source);

    if (needsLireRuntime) {
        String runtimeChunk = RuntimeSupport.lireIntRuntimeChunk(
                options.getScannerFieldName(),
                options.getScannerInitExpr(),
                options.getLireMethodName()
        );
        source = SourcePatcher.injectBeforeLastBrace(source, runtimeChunk);
        source = ImportManager.ensureImport(source, "java.util.Scanner");
    }

    // 4) (Optionnel) packager + nom de classe (si tu veux plus tard)
    // Ici on laisse ton Programme.genJava gérer le nom de classe.

    return new GenerationResult(source, sem);
    }

    private AnalyseSemantique runSemantic(Programme programme) {
    AnalyseSemantique sem = new AnalyseSemantique();
    sem.verifier(programme);
    return sem;
    }
    }


