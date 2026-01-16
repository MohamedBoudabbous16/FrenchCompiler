package tests.ir;
import java.ir.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IrModelAndInspectorTests {

    @Test
    void irBloc_null_list_devient_liste_vide() {
        IrBloc b = new IrBloc(null);
        assertNotNull(b.instructions());
        assertEquals(0, b.instructions().size());
    }

    @Test
    void irConstTexte_refuse_null() {
        assertThrows(NullPointerException.class, () -> new IrConstTexte(null));
    }

    @Test
    void irProgramme_refuse_nulls() {
        assertThrows(NullPointerException.class, () -> new IrProgramme(null, List.of()));
        assertThrows(NullPointerException.class, () -> new IrProgramme("ProgrammePrincipal", null));
    }

    @Test
    void inspecteur_detecte_lire_dans_programme() {
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.ENTIER,
                        new IrBloc(List.of(
                                new IrAffectation("x", IrLire.INSTANCE),
                                new IrRetourne(new IrVariable("x"))
                        ))
                )
        ));

        assertTrue(IrInspecteur.utiliseLire(p));
    }

    @Test
    void inspecteur_ne_detecte_pas_lire_si_absent() {
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.ENTIER,
                        new IrBloc(List.of(
                                new IrAffectation("x", new IrConstInt(3)),
                                new IrRetourne(new IrVariable("x"))
                        ))
                )
        ));

        assertFalse(IrInspecteur.utiliseLire(p));
    }
}
