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
        // normalisation / defaults
        nomClasse = (nomClasse == null || nomClasse.isBlank()) ? "ProgrammePrincipal" : nomClasse;
        fonctions = (fonctions == null) ? List.of() : List.copyOf(fonctions);

        // validations
        Objects.requireNonNull(nomClasse, "nomClasse");
        Objects.requireNonNull(fonctions, "fonctions");
    }
}
