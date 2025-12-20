package semantic;


import java.util.*;

public class TableSymboles {

    private final Deque<Map<String, Symbole>> pile = new ArrayDeque<>();

    public TableSymboles() {
        entrerPortee(); // portée globale
    }

    public void entrerPortee() {
        pile.push(new HashMap<>());
    }

    public void sortirPortee() {
        pile.pop();
    }

    public void declarer(String nom, TypeSimple type, boolean estParametre) {
        Map<String, Symbole> scope = pile.peek();
        if (scope.containsKey(nom)) {
            throw new RuntimeException("Erreur sémantique : '" + nom + "' déjà déclaré dans cette portée.");
        }
        scope.put(nom, new Symbole(nom, type, estParametre));
    }

    public Symbole resoudre(String nom) {
        for (Map<String, Symbole> scope : pile) {
            if (scope.containsKey(nom)) return scope.get(nom);
        }
        return null;
    }
}
