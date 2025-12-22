package utils.diag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Diagnostics {

    private final List<Diagnostic> liste = new ArrayList<>();

    public void ajouter(Diagnostic d) {
        if (d != null) liste.add(d);
    }

    public List<Diagnostic> tous() {
        return Collections.unmodifiableList(liste);
    }

    public boolean aDesErreurs() {
        return liste.stream().anyMatch(Diagnostic::estErreur);
    }

    public int nbErreurs() {
        return (int) liste.stream().filter(Diagnostic::estErreur).count();
    }

    public void vider() {
        liste.clear();
    }

    public String formatTous(SourceTexte source) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : liste) {
            sb.append(d.formatAvecSource(source)).append("\n\n");
        }
        return sb.toString().trim();
    }
}
