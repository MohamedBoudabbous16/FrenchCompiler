package main.java.codegenerator;

import main.java.parseur.ast.Programme;
import main.java.semantic.AnalyseSemantique;

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
     * @param sem       l’analyse sémantique déjà réalisée sur ce programme
     * @return une chaîne contenant le code source Java correspondant
     */
    public String generate(Programme programme, AnalyseSemantique sem) {
        if (programme == null || sem == null) {
            throw new IllegalArgumentException("Programme ou analyse sémantique manquant");
        }
        // On délègue la génération au nœud de plus haut niveau (Programme).
        return programme.genJava(sem);
    }
}
