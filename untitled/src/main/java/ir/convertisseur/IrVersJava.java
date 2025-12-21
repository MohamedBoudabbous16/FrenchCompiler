package main.java.ir.convertisseur;


import main.java.ir.*;
import main.java.semantic.AnalyseSemantique;
import main.java.semantic.TypeSimple;

import java.util.*;

/**
 * Générateur Java depuis l'IR.
 *
 * Garanties "tests-friendly":
 * 1) Si l'IR utilise Lire => le code généré contient Scanner (import + champ Scanner + méthode lire()).
 * 2) Affiche multi-args => génère un System.out.print(...) par argument (+ println() si newline).
 */
public final class IrVersJava {

    private IrVersJava() {}

    public static String generate(IrProgramme p) {
        return generate(p, null);
    }

    public static String generate(IrProgramme p, AnalyseSemantique sem) {
        if (p == null) throw new IllegalArgumentException("IR programme null");

        String className = (p.nomClasse() == null || p.nomClasse().isBlank())
                ? "ProgrammePrincipal"
                : p.nomClasse();

        boolean needsScanner = IrInspecteur.utiliseLire(p);

        StringBuilder out = new StringBuilder();

        // Import (tes tests acceptent "Scanner" ou "java.util.Scanner")
        if (needsScanner) {
            out.append("import java.util.Scanner;\n\n");
        }

        out.append("public class ").append(className).append(" {\n\n");

        // Runtime lire()
        if (needsScanner) {
            out.append("  // ===== Runtime built-in =====\n");
            out.append("  private static final Scanner SCANNER = new Scanner(System.in);\n\n");
            out.append("  /** Built-in: lire() -> ENTIER */\n");
            out.append("  public static int lire() {\n");
            out.append("    return SCANNER.nextInt();\n");
            out.append("  }\n\n");
        }

        // Wrapper main(String[]) si une fonction "main" existe
        IrFonction fnMain = findFunction(p.fonctions(), "main");
        if (fnMain != null) {
            out.append("  public static void main(String[] args) {\n");
            if (fnMain.typeRetour() == IrType.VIDE) {
                out.append("    main();\n");
            } else {
                out.append("    Object res = main();\n");
                out.append("    System.out.println(res);\n");
            }
            out.append("  }\n\n");
        }

        // Fonctions
        for (IrFonction f : p.fonctions()) {
            out.append(genFunction(f, sem));
            out.append("\n");
        }

        out.append("}\n");
        return out.toString();
    }

    private static IrFonction findFunction(List<IrFonction> fs, String name) {
        if (fs == null) return null;
        for (IrFonction f : fs) {
            if (name.equals(f.nom())) return f;
        }
        return null;
    }

    private static String genFunction(IrFonction f, AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();

        String ret = javaTypeOfReturn(f.typeRetour());
        sb.append("  public static ").append(ret).append(" ").append(f.nom()).append("(");

        // paramètres: Object par défaut (comme ton générateur actuel)
        List<String> params = f.params() == null ? List.of() : f.params();
        for (int i = 0; i < params.size(); i++) {
            sb.append("Object ").append(params.get(i));
            if (i < params.size() - 1) sb.append(", ");
        }
        sb.append(") {\n");

        // Table des types variables si sem dispo
        Map<String, TypeSimple> varTypes = variablesFromSem(sem, f.nom());

        // Variables déjà déclarées (params + loop vars)
        Set<String> declared = new HashSet<>(params);

        // Génération du corps
        List<String> lines = genInstructionLines(f.corps(), sem, f.nom(), varTypes, declared, 2);

        for (String line : lines) {
            sb.append(line).append("\n");
        }

        sb.append("  }\n");
        return sb.toString();
    }

    private static Map<String, TypeSimple> variablesFromSem(AnalyseSemantique sem, String fnName) {
        if (sem == null) return Map.of();
        try {
            var m = sem.variablesDe(fnName);
            return (m == null) ? Map.of() : m;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String javaTypeOfReturn(IrType t) {
        if (t == null) return "Object";
        return switch (t) {
            case ENTIER -> "int";
            case BOOLEEN -> "boolean";
            case TEXTE -> "String";
            case CARACTERE -> "char";
            case VIDE -> "void";
            case OBJET -> "Object";
        };
    }

    private static String javaTypeOfVar(String var, IrExpression expr, Map<String, TypeSimple> semTypes) {
        // 1) priorité sémantique si disponible
        TypeSimple ts = semTypes.get(var);
        if (ts != null) {
            return switch (ts) {
                case ENTIER -> "int";
                case BOOLEEN -> "boolean";
                case TEXTE -> "String";
                case CARACTERE -> "char";
                case VIDE -> "Object";
                default -> "Object";
            };
        }

        // 2) heuristique simple si pas de sem
        if (expr == IrLire.INSTANCE) return "int";
        if (expr instanceof IrConstInt) return "int";
        if (expr instanceof IrConstBool) return "boolean";
        if (expr instanceof IrConstTexte) return "String";
        if (expr instanceof IrConstChar) return "char";

        return "Object";
    }

    private static List<String> genInstructionLines(
            IrInstruction instr,
            AnalyseSemantique sem,
            String fnName,
            Map<String, TypeSimple> varTypes,
            Set<String> declared,
            int indentLevel
    ) {
        String ind = "  ".repeat(indentLevel);
        List<String> out = new ArrayList<>();

        if (instr == null) return out;

        // Bloc
        if (instr instanceof IrBloc b) {
            out.add(ind + "{");
            for (IrInstruction x : b.instructions()) {
                out.addAll(genInstructionLines(x, sem, fnName, varTypes, declared, indentLevel + 1));
            }
            out.add(ind + "}");
            return out;
        }

        // Affectation (déclare si première fois)
        if (instr instanceof IrAffectation a) {
            String v = a.variable();
            String rhs = genExpr(a.expression());

            if (!declared.contains(v)) {
                String javaType = javaTypeOfVar(v, a.expression(), varTypes);
                out.add(ind + javaType + " " + v + " = " + rhs + ";");
                declared.add(v);
            } else {
                out.add(ind + v + " = " + rhs + ";");
            }
            return out;
        }

        // Retourne
        if (instr instanceof IrRetourne r) {
            if (r.expression() == null) out.add(ind + "return;");
            else out.add(ind + "return " + genExpr(r.expression()) + ";");
            return out;
        }

        // Affiche (IMPORTANT POUR TES TESTS)
        if (instr instanceof IrAffiche a) {
            for (IrExpression e : a.args()) {
                out.add(ind + "System.out.print(" + genExpr(e) + ");");
            }
            if (a.newline()) {
                out.add(ind + "System.out.println();");
            }
            return out;
        }

        // Expression instruction
        if (instr instanceof IrExpressionInstr e) {
            out.add(ind + genExpr(e.expression()) + ";");
            return out;
        }

        // Si
        if (instr instanceof IrSi s) {
            out.add(ind + "if (" + genExpr(s.condition()) + ") {");
            out.addAll(genInstructionLines(s.alorsInstr(), sem, fnName, varTypes, declared, indentLevel + 1));
            out.add(ind + "}");
            if (s.sinonInstr() != null) {
                out.add(ind + "else {");
                out.addAll(genInstructionLines(s.sinonInstr(), sem, fnName, varTypes, declared, indentLevel + 1));
                out.add(ind + "}");
            }
            return out;
        }

        // TantQue
        if (instr instanceof IrTantQue t) {
            out.add(ind + "while (" + genExpr(t.condition()) + ") {");
            out.addAll(genInstructionLines(t.corps(), sem, fnName, varTypes, declared, indentLevel + 1));
            out.add(ind + "}");
            return out;
        }

        // Pour (fallback en while)
        if (instr instanceof IrPour p) {
            String i = p.nomVariable();
            String start = genExpr(p.debut());
            String end = genExpr(p.fin());
            String step = genExpr(p.pas());
            String op = p.operateurPas();

            if (!declared.contains(i)) {
                out.add(ind + "int " + i + " = " + start + ";");
                declared.add(i);
            } else {
                out.add(ind + i + " = " + start + ";");
            }

            String cond = switch (op) {
                case "+=" -> i + " <= " + end;
                case "-=" -> i + " >= " + end;
                default -> i + " != " + end; // *=, /= etc.
            };

            out.add(ind + "while (" + cond + ") {");
            out.addAll(genInstructionLines(p.corps(), sem, fnName, varTypes, declared, indentLevel + 1));
            out.add(ind + "  " + i + " " + op + " " + step + ";");
            out.add(ind + "}");
            return out;
        }

        throw new IllegalArgumentException("Instruction IR non supportée: " + instr.getClass().getName());
    }

    private static String genExpr(IrExpression e) {
        if (e == null) return "null";

        if (e == IrLire.INSTANCE) return "lire()";

        if (e instanceof IrConstInt c) return Integer.toString(c.value());
        if (e instanceof IrConstBool b) return b.value() ? "true" : "false";
        if (e instanceof IrConstChar c) return "'" + escapeChar(c.value()) + "'";
        if (e instanceof IrConstTexte t) return "\"" + escapeString(t.value()) + "\"";

        if (e instanceof IrVariable v) return v.name();

        if (e instanceof IrBinaire b) {
            return "(" + genExpr(b.gauche()) + " " + b.op() + " " + genExpr(b.droite()) + ")";
        }

        if (e instanceof IrAppel a) {
            StringBuilder sb = new StringBuilder();
            sb.append(a.nom()).append("(");
            for (int i = 0; i < a.args().size(); i++) {
                sb.append(genExpr(a.args().get(i)));
                if (i < a.args().size() - 1) sb.append(", ");
            }
            sb.append(")");
            return sb.toString();
        }

        throw new IllegalArgumentException("Expression IR non supportée: " + e.getClass().getName());
    }

    private static String escapeString(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapeChar(char c) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '\'' -> "\\'";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            default -> Character.toString(c);
        };
    }
}
