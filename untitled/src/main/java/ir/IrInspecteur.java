package main.java.ir;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Outils d'analyse rapide de l'IR (ex: détecter si lire() est utilisé).
 * Ça permet de décider d'injecter le runtime Scanner.
 */
public final class IrInspecteur {

    private IrInspecteur() {}

    public static boolean utiliseLire(IrProgramme p) {
        if (p == null) return false;

        Deque<IrNoeud> pile = new ArrayDeque<>();
        pile.push(p);

        while (!pile.isEmpty()) {
            IrNoeud n = pile.pop();

            if (n instanceof IrProgramme prog) {
                for (IrFonction f : prog.fonctions()) pile.push(f);
            } else if (n instanceof IrFonction f) {
                pile.push(f.corps());
            } else if (n instanceof IrBloc b) {
                for (IrInstruction i : b.instructions()) pile.push(i);
            } else if (n instanceof IrAffectation a) {
                pile.push(a.expression());
            } else if (n instanceof IrRetourne r) {
                if (r.expression() != null) pile.push(r.expression());
            } else if (n instanceof IrAffiche af) {
                for (IrExpression e : af.args()) pile.push(e);
            } else if (n instanceof IrExpressionInstr ei) {
                pile.push(ei.expression());
            } else if (n instanceof IrSi s) {
                pile.push(s.condition());
                pile.push(s.alorsInstr());
                if (s.sinonInstr() != null) pile.push(s.sinonInstr());
            } else if (n instanceof IrTantQue t) {
                pile.push(t.condition());
                pile.push(t.corps());
            } else if (n instanceof IrPour pour) {
                pile.push(pour.debut());
                pile.push(pour.fin());
                pile.push(pour.pas());
                pile.push(pour.corps());
            } else if (n instanceof IrLire) {
                return true;
            } else if (n instanceof IrBinaire b) {
                pile.push(b.gauche());
                pile.push(b.droite());
            } else if (n instanceof IrAppel a) {
                for (IrExpression e : a.args()) pile.push(e);
            }
            // Les littéraux / variables -> rien à pousser
        }

        return false;
    }
}
