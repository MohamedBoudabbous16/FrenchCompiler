package main.java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Programme IR : pour ton langage actuel, on peut représenter un programme
 * comme une seule "classe" Java contenant des fonctions statiques.
 */
public record IrProgramme(String nomClasse, List<IrFonction> fonctions) implements IrNoeud {

    public IrProgramme {
        Objects.requireNonNull(nomClasse, "nomClasse");
        Objects.requireNonNull(fonctions, "fonctions");
    }
}
