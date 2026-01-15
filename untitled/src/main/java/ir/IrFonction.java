package main.java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Fonction IR.
 *
 * - nom : nom de la fonction
 * - params : noms des paramètres (non typés ici -> Object côté Java)
 * - typeRetour : type de retour (peut venir de la sémantique)
 * - corps : bloc d’instructions
 */
public record IrFonction(String nom, List<String> params, IrType typeRetour, IrBloc corps) implements IrNoeud {

    public IrFonction {
        nom = (nom == null || nom.isBlank()) ? "<anonyme>" : nom;
        params = (params == null) ? List.of() : List.copyOf(params);
        typeRetour = (typeRetour == null) ? IrType.OBJET : typeRetour;
        corps = (corps == null) ? new IrBloc(List.of()) : corps;

        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(typeRetour, "typeRetour");
        Objects.requireNonNull(corps, "corps");
    }
}
