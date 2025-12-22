package utils.diag;

import java.util.ArrayList;
import java.util.List;

/**
 * Représentation d'un texte source avec indexation par lignes
 * pour produire des messages de diagnostic lisibles.
 */
public final class SourceTexte {

    private final String nom;
    private final List<String> lignes;

    public SourceTexte(String nom, String texte) {
        this.nom = (nom == null || nom.isBlank()) ? null : nom;
        this.lignes = decouperEnLignes(texte == null ? "" : texte);
    }

    public String nom() {
        return nom;
    }

    public int nbLignes() {
        return lignes.size();
    }

    /** ligne indexée à partir de 1. */
    public String ligne(int noLigne) {
        if (noLigne < 1 || noLigne > lignes.size()) return null;
        return lignes.get(noLigne - 1);
    }

    private static List<String> decouperEnLignes(String s) {
        // On garde une logique simple et fiable.
        String[] parts = s.split("\\R", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) out.add(p);
        if (out.isEmpty()) out.add("");
        return out;
    }
}
