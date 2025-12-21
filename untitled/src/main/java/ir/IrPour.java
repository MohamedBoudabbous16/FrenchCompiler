package main.java.ir;

import java.util.Objects;

/**
 * Représentation proche de ton AST "pour".
 * pour i = [debut ; fin], (+=|-=|*=|/=) pas  corps
 */
public record IrPour(
        String nomVariable,
        IrExpression debut,
        IrExpression fin,
        String operateurPas,
        IrExpression pas,
        IrInstruction corps
) implements IrInstruction {

    public IrPour {
        Objects.requireNonNull(nomVariable, "nomVariable");
        Objects.requireNonNull(debut, "debut");
        Objects.requireNonNull(fin, "fin");
        Objects.requireNonNull(operateurPas, "operateurPas");
        Objects.requireNonNull(pas, "pas");
        Objects.requireNonNull(corps, "corps");
    }
}
