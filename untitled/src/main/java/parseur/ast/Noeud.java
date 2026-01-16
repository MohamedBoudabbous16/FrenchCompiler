package java.parseur.ast;

import java.semantic.AnalyseSemantique;

public interface Noeud {
    /**
     *
     * @return le code java qui correwspond au noeud
     */
    String genJava(AnalyseSemantique sem);
}
