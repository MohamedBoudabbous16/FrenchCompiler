package tests.utils.diag;



import org.junit.jupiter.api.Test;
import utils.diag.*;

import java.util.List;
import utils.diag.Diagnostics;
import utils.diag.Diagnostic;
import utils.diag.DiagnosticCollector;
import utils.diag.SourceTexte;
 import utils.diag.Position;

import static org.junit.jupiter.api.Assertions.*;
import static utils.diag.Gravite.ERREUR;

class DiagnosticsCollectorTest {

    @Test
    void diagnostics_container_basique() {
        Diagnostics ds = new Diagnostics();
        ds.ajouter(null);
        assertEquals(0, ds.tous().size());

        ds.ajouter(Diagnostic.info("i"));
        ds.ajouter(Diagnostic.erreur("e"));
        assertEquals(2, ds.tous().size());
        assertTrue(ds.aDesErreurs());
        assertEquals(1, ds.nbErreurs());

        ds.vider();
        assertEquals(0, ds.tous().size());
    }

    @Test
    void collector_aDesErreurs_et_comptes() {
        DiagnosticCollector c = new DiagnosticCollector();

        c.info("ok");
        c.avertissement("warn");
        c.erreur("err");

        assertTrue(c.aDesErreurs());
        assertEquals(1, c.nombreErreurs());
        assertEquals(1, c.nombreAvertissements());

        List<Diagnostic> erreurs = c.erreurs();
        assertEquals(1, erreurs.size());
        assertEquals(ERREUR, erreurs.get(0).gravite());
    }

    @Test
    void collector_formatTous_utilise_sourceParDefaut_si_aucune_source() {
        utils.diag.SourceTexte src = new SourceTexte("prog", "a\nb\nc");
        DiagnosticCollector c = new DiagnosticCollector(src);

        // Diagnostic sans Position.source() (Position(int,int) => source null)
        c.erreur("bad", new Position(2, 1));

        String out = c.formatTous();
        assertTrue(out.contains("prog:2:1"));
        assertTrue(out.contains("2 | b"));
        assertTrue(out.contains("^"));
    }
}
