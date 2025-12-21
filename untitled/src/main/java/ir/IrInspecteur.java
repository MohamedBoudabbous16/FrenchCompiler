package main.java.ir;

import java.util.List;

public final class IrInspecteur {
    private IrInspecteur() {}

    public static boolean utiliseLire(IrProgramme p) {
        if (p == null) return false;
        for (IrFonction f : p.fonctions()) {
            if (utiliseLire(f.corps())) return true;
        }
        return false;
    }

    private static boolean utiliseLire(IrInstruction i) {
        if (i == null) return false;

        if (i instanceof IrBloc b) {
            for (IrInstruction x : b.instructions()) if (utiliseLire(x)) return true;
            return false;
        }

        if (i instanceof IrAffectation a) return utiliseLire(a.expression());
        if (i instanceof IrRetourne r) return r.expression() != null && utiliseLire(r.expression());
        if (i instanceof IrAffiche a) {
            for (IrExpression e : a.args()) if (utiliseLire(e)) return true;
            return false;
        }
        if (i instanceof IrExpressionInstr e) return utiliseLire(e.expression());
        if (i instanceof IrSi s) {
            return utiliseLire(s.condition())
                    || utiliseLire(s.alorsInstr())
                    || (s.sinonInstr() != null && utiliseLire(s.sinonInstr()));
        }
        if (i instanceof IrTantQue t) return utiliseLire(t.condition()) || utiliseLire(t.corps());
        if (i instanceof IrPour p) {
            return utiliseLire(p.debut()) || utiliseLire(p.fin()) || utiliseLire(p.pas()) || utiliseLire(p.corps());
        }

        return false;
    }

    private static boolean utiliseLire(IrExpression e) {
        if (e == null) return false;
        if (e == IrLire.INSTANCE) return true;

        if (e instanceof IrBinaire b) return utiliseLire(b.gauche()) || utiliseLire(b.droite());
        if (e instanceof IrAppel a) {
            for (IrExpression x : a.args()) if (utiliseLire(x)) return true;
            return false;
        }
        return false;
    }
}
