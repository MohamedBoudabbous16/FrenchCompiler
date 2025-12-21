package main.java.ir;

import java.util.List;
import java.util.Objects;

/**
 * Fonction IR : nom, paramètres (non typés pour le moment), type de retour, corps.
 */
public record IrFonction(
        String nom,
        List<String> params,
        IrType typeRetour,
        IrBloc corps
) implements IrNoeud {

    public IrFonction {
        Objects.requireNonNull(nom, "nom");
        Objects.requireNonNull(params, "params");
        Objects.requireNonNull(typeRetour, "typeRetour");
        Objects.requireNonNull(corps, "corps");
    }
}
