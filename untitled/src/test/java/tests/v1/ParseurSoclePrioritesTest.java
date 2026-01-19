package tests.v1;



import main.java.parseur.ast.*;
import org.junit.jupiter.api.Test;
import tests.TestTools;

import static org.junit.jupiter.api.Assertions.*;

public class ParseurSoclePrioritesTest {

    private static Fonction mainFn(Programme p) {
        Classe c = p.getClasses().get(0);
        return c.getFonctions().stream().filter(f -> f.getNom().equals("main")).findFirst().orElseThrow();
    }

    private static Retourne uniqueRetourne(Fonction f) {
        Bloc b = f.getCorps();
        return (Retourne) b.getInstructions().stream()
                .filter(i -> i instanceof Retourne)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void precedence_mult_sur_plus() {
        Programme p = (Programme) TestTools.parseProgramme("fonction main(){ retourne 1 + 2 * 3; }");
        Fonction f = mainFn(p);
        Retourne r = uniqueRetourne(f);

        ExpressionBinaire top = (ExpressionBinaire) r.getExpression();
        assertEquals("+", top.getop());

        assertTrue(top.getDroite() instanceof ExpressionBinaire);
        ExpressionBinaire right = (ExpressionBinaire) top.getDroite();
        assertEquals("*", right.getop());
    }

    @Test
    void associativite_affectation_droite() {
        Programme p = (Programme) TestTools.parseProgramme("fonction main(){ a = b = 3; retourne 0; }");
        Fonction f = mainFn(p);

        Instruction first = f.getCorps().getInstructions().get(0);
        assertTrue(first instanceof ExpressionInstr);

        Expression e = ((ExpressionInstr) first).getExpression();
        assertTrue(e instanceof ExpressionAffectation);

        ExpressionAffectation a = (ExpressionAffectation) e;
        assertTrue(a.getValeur() instanceof ExpressionAffectation); // a = (b = 3)
    }
}

