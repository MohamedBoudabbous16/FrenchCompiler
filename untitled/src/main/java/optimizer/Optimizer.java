package java.optimizer;

import java.parseur.ast.Programme;

/**
 * Optimizer effectue des optimisations simples sur l'arbre abstrait.
 *
 * Cette classe est volontairement minimale : elle fournit un point
 * d'extension pour ajouter des optimisations (suppression de code mort,
 * pliage de constantes, etc.). Pour l'instant, la méthode {@code optimize}
 * se contente de renvoyer le programme tel quel.
 */
public class Optimizer {
    private final DeadCodeEliminator deadCode = new DeadCodeEliminator();
    private final ConstantFolder constantFolder = new ConstantFolder();
    /**
    * Optimise un programme. Les transformations possibles incluent
    * l'élimination du code mort ou le pliage de constantes. À terme,
    * cette méthode pourrait modifier l'AST en place ou en retourner
    * une version optimisée. Pour l'instant, aucun traitement n'est appliqué.
    *
    * @param programme programme à optimiser
    * @return le programme optimisé (identique à l'entrée pour l'instant)
    */





    /**
     * Optimise l'AST du programme.
     * @return le même Programme (optimisé in-place quand possible)
     */
    public Programme optimize(Programme programme) {
        if (programme == null) return null;

        // 1) constant folding (expressions)
        constantFolder.optimize(programme);

        // 2) dead code + simplifications de contrôle (si/tantque)
        deadCode.optimize(programme);

        return programme;
    }
    }
