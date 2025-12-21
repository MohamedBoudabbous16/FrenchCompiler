package main.java.optimizer;

import main.java.parseur.ast.Programme;

/**
 * Optimizer effectue des optimisations simples sur l'arbre abstrait.
 *
 * Cette classe est volontairement minimale : elle fournit un point
 * d'extension pour ajouter des optimisations (suppression de code mort,
 * pliage de constantes, etc.). Pour l'instant, la méthode {@code optimize}
 * se contente de renvoyer le programme tel quel.
 */
public class Optimizer {

    /**
     * Optimise un programme. Les transformations possibles incluent
     * l'élimination du code mort ou le pliage de constantes. À terme,
     * cette méthode pourrait modifier l'AST en place ou en retourner
     * une version optimisée. Pour l'instant, aucun traitement n'est appliqué.
     *
     * @param programme programme à optimiser
     * @return le programme optimisé (identique à l'entrée pour l'instant)
     */
    public Programme optimize(Programme programme) {
        // Point d'extension pour des optimisations futures
        return programme;
    }
}
