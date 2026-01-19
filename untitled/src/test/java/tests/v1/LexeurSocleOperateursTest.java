package tests.v1;



import main.java.lexeur.Lexeur;
import main.java.lexeur.Jeton;
import main.java.lexeur.TypeJeton;
import org.junit.jupiter.api.Test;
import utils.diag.DiagnosticCollector;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LexeurSocleOperateursTest {

    @Test
    void lexeur_reconnait_affectations_composees() {
        String src = "fonction main(){ x+=1; y-=2; z*=3; t/=4; u%=5; }";
        DiagnosticCollector diags = new DiagnosticCollector();
        Lexeur lex = new Lexeur(src, diags);
        List<Jeton> js = lex.analyser();

        assertTrue(js.stream().anyMatch(j -> j.getType() == TypeJeton.PlusEgal));
        assertTrue(js.stream().anyMatch(j -> j.getType() == TypeJeton.MoinsEgal));
        assertTrue(js.stream().anyMatch(j -> j.getType() == TypeJeton.MultEgal));
        assertTrue(js.stream().anyMatch(j -> j.getType() == TypeJeton.DivEgal));
        assertTrue(js.stream().anyMatch(j -> j.getType() == TypeJeton.ModEgal));
        assertFalse(diags.aDesErreurs(), diags.toString());
    }

    @Test
    void lexeur_reconnait_incr_decr() {
        String src = "fonction main(){ x++; ++x; x--; --x; }";
        DiagnosticCollector diags = new DiagnosticCollector();
        Lexeur lex = new Lexeur(src, diags);
        List<Jeton> js = lex.analyser();

        long incr = js.stream().filter(j -> j.getType() == TypeJeton.Incr).count();
        long decr = js.stream().filter(j -> j.getType() == TypeJeton.Decr).count();

        assertEquals(2, incr);
        assertEquals(2, decr);
        assertFalse(diags.aDesErreurs(), diags.toString());
    }
}
