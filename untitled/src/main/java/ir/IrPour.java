package main.java.ir;

import java.util.Objects;

/**
 * Pour IR (forme “langage maison”):
 *  pour i = [debut; fin], op=pas instruction
 *
 * operateurPas: "+=" | "-=" | "*=" | "/=" (ou autre fallback)
 *
 * Ton générateur Java peut le traduire en while si tu veux rester simple.
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
        nomVariable = (nomVariable == null || nomVariable.isBlank()) ? "<i?>" : nomVariable;
        debut = (debut == null) ? new IrConstInt(0) : debut;
        fin = (fin == null) ? new IrConstInt(0) : fin;
        operateurPas = (operateurPas == null || operateurPas.isBlank()) ? "+=" : operateurPas;
        pas = (pas == null) ? new IrConstInt(1) : pas;
        corps = (corps == null) ? new IrBloc(java.util.List.of()) : corps;
        Objects.requireNonNull(nomVariable, "nomVariable");
        Objects.requireNonNull(debut, "debut");
        Objects.requireNonNull(fin, "fin");
        Objects.requireNonNull(operateurPas, "operateurPas");
        Objects.requireNonNull(pas, "pas");
        Objects.requireNonNull(corps, "corps");
    }
}
