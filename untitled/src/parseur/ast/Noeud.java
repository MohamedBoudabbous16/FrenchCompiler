package parseur.ast;

import semantic.AnalyseSemantique;

public interface Noeud {
    /**
     *
     * @return le code java qui correwspond au noeud
     */
    String genJava(AnalyseSemantique sem);
}
