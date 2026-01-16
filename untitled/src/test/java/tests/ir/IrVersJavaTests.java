package tests.ir;
import java.ir.*;
import java.ir.convertisseur.IrVersJava;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IrVersJavaTests {

    private static int countOccurrences(String s, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf(token, idx)) >= 0) {
            count++;
            idx += token.length();
        }
        return count;
    }

    @Test
    void irversjava_injecte_scanner_si_lire_est_utilise() {
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.ENTIER,
                        new IrBloc(List.of(
                                new IrAffectation("x", IrLire.INSTANCE),
                                new IrAffiche(List.of(new IrConstTexte("x="), new IrVariable("x")), true),
                                new IrRetourne(new IrVariable("x"))
                        ))
                )
        ));

        String javaCode = IrVersJava.generate(p);
        assertNotNull(javaCode);

        // 1) Import Scanner OU usage java.util.Scanner (tes tests acceptent l’un ou l’autre)
        assertTrue(javaCode.contains("import java.util.Scanner")
                        || javaCode.contains("java.util.Scanner"),
                "Le code doit contenir Scanner si Lire est utilisé.");

        // 2) Méthode lire() runtime + scanner partagé
        assertTrue(javaCode.contains("public static int lire()"),
                "Le runtime doit fournir public static int lire().");
        assertTrue(javaCode.contains("new Scanner(System.in)") || javaCode.contains("Scanner(System.in)"),
                "Le runtime doit initialiser un Scanner(System.in).");

        // 3) L’appel doit être lire()
        assertTrue(javaCode.contains("x = lire()") || javaCode.contains("= lire()"),
                "L’IR Lire doit se traduire par un appel lire().");
    }

    @Test
    void irversjava_affiche_multi_args_genere_un_print_par_argument_et_un_println_si_newline() {
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.ENTIER,
                        new IrBloc(List.of(
                                new IrAffectation("x", new IrConstInt(3)),
                                new IrAffectation("y", new IrConstInt(4)),
                                new IrAffiche(List.of(
                                        new IrConstTexte("x="),
                                        new IrVariable("x"),
                                        new IrConstTexte(" y="),
                                        new IrVariable("y")
                                ), true),
                                new IrRetourne(new IrConstInt(0))
                        ))
                )
        ));

        String javaCode = IrVersJava.generate(p);
        assertNotNull(javaCode);

        int printCount = countOccurrences(javaCode, "System.out.print(");
        int printlnCount = countOccurrences(javaCode, "System.out.println(");

        assertEquals(4, printCount, "Affiche(args...) doit générer exactement un print par argument.");
        assertTrue(printlnCount >= 1, "newline=true doit générer un System.out.println().");
    }

    @Test
    void irversjava_declare_variable_a_la_premiere_affectation() {
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.ENTIER,
                        new IrBloc(List.of(
                                new IrAffectation("x", new IrConstInt(1)),
                                new IrAffectation("x", new IrConstInt(2)),
                                new IrRetourne(new IrVariable("x"))
                        ))
                )
        ));

        String javaCode = IrVersJava.generate(p);

        // Première affectation: doit ressembler à "int x = 1;" (ou Object si tu as choisi autre chose)
        assertTrue(javaCode.contains("int x = 1;") || javaCode.contains("Object x = 1;"),
                "La première affectation doit déclarer la variable (int x = 1; idéalement).");

        // Deuxième: doit être "x = 2;" (sans redéclaration)
        assertTrue(javaCode.contains("x = 2;"),
                "La seconde affectation ne doit pas redéclarer x.");
    }

    @Test
    void irversjava_escape_texte() {
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.VIDE,
                        new IrBloc(List.of(
                                new IrAffiche(List.of(new IrConstTexte("a\"b\\c\n")), true),
                                new IrRetourne(null)
                        ))
                )
        ));

        String javaCode = IrVersJava.generate(p);

        // On veut voir \" \\ et \n dans le Java généré
        assertTrue(javaCode.contains("\"a\\\"b\\\\c\\n\""),
                "Les chaînes doivent être correctement échappées (\\\", \\\\, \\n).");
    }

    @Test
    void irversjava_binaire_parentheses() {
        IrExpression expr = new IrBinaire(new IrConstInt(1), "+", new IrConstInt(2));
        IrProgramme p = new IrProgramme("ProgrammePrincipal", List.of(
                new IrFonction("main", List.of(), IrType.ENTIER,
                        new IrBloc(List.of(
                                new IrAffectation("x", expr),
                                new IrRetourne(new IrVariable("x"))
                        ))
                )
        ));

        String javaCode = IrVersJava.generate(p);
        assertTrue(javaCode.contains("(1 + 2)"),
                "Les binaires doivent garder les parenthèses: (gauche op droite).");
    }
}
