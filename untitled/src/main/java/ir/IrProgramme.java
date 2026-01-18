package main.java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Programme IR.
 *
 * Pour ton langage actuel, on génère généralement une seule classe Java finale
 * contenant toutes les fonctions.
 */
public record IrProgramme(String nomClasse, List<IrFonction> fonctions) implements IrNoeud {

    public IrProgramme {
        // Refuser les nulls (exigé par les tests)
        Objects.requireNonNull(nomClasse, "nomClasse");
        Objects.requireNonNull(fonctions, "fonctions");

        // Normalisation optionnelle (mais sans toucher aux nulls)
        if (nomClasse.isBlank()) {
            nomClasse = "ProgrammePrincipal";
        }

        // Copie défensive
        fonctions = List.copyOf(fonctions);
    }
}
