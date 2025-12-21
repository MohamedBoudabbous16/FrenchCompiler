package main.java.ir.convertisseur;

import main.java.ir.*;
import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;
import main.java.semantic.AnalyseSemantique;
import main.java.semantic.TypeSimple;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Convertisseur AST -> IR.
 *
 * Objectif:
 * - Centraliser une représentation intermédiaire stable (IR),
 * - Permettre des inspections simples (ex: lire() ?)
 * - Préparer un pipeline AST -> IR -> Java plus propre plus tard.
 *
 * NOTE:
 * - Le code est compatible avec tes classes AST actuelles.
 * - Il utilise des casts directs quand possible,
 *   et sinon un fallback par réflexion (getters ou champs).
 */
public final class AstVersIr {

    private AstVersIr() {}

    /** Conversion sans sémantique: types de retour par défaut = OBJET. */
    public static IrProgramme convertir(Programme astProgramme) {
        return convertir(astProgramme, null);
    }

    /** Conversion avec sémantique: types de retour alignés sur AnalyseSemantique. */
    public static IrProgramme convertir(Programme astProgramme, AnalyseSemantique sem) {
        if (astProgramme == null) throw new IllegalArgumentException("programme AST null");

        // Ton AST actuel: Programme -> List<Classe>, et tu génères souvent "ProgrammePrincipal"
        List<Classe> classes = astProgramme.getClasses();
        if (classes == null || classes.isEmpty()) {
            return new IrProgramme("ProgrammePrincipal", List.of());
        }

        // Pour une IR simple: on prend la première classe comme "classe principale"
        Classe classePrincipale = classes.get(0);
        String nomClasse = safeString(classePrincipale.getNom(), "ProgrammePrincipal");

        List<IrFonction> fonctionsIr = new ArrayList<>();
        for (Classe c : classes) {
            if (c.getFonctions() == null) continue;
            for (Fonction f : c.getFonctions()) {
                fonctionsIr.add(convertirFonction(f, sem));
            }
        }

        return new IrProgramme(nomClasse, fonctionsIr);
    }

    // =========================
    // Fonctions / blocs / instr
    // =========================

    private static IrFonction convertirFonction(Fonction f, AnalyseSemantique sem) {
        String nom = safeString(f.getNom(), "<anonyme>");
        List<String> params = (f.getParam() == null) ? List.of() : new ArrayList<>(f.getParam());

        IrType typeRetour = IrType.OBJET;
        if (sem != null) {
            try {
                TypeSimple t = sem.typeRetourDe(nom);
                typeRetour = mapType(t);
            } catch (Exception ignored) {
                // si sem n'est pas prêt / fonction inconnue
            }
        }

        IrBloc corps = convertirBloc(f.getCorps());
        return new IrFonction(nom, params, typeRetour, corps);
    }

    private static IrBloc convertirBloc(Bloc bloc) {
        if (bloc == null) return new IrBloc(List.of());

        List<IrInstruction> instrIr = new ArrayList<>();
        List<Instruction> instrAst = bloc.getInstructions();
        if (instrAst != null) {
            for (Instruction i : instrAst) {
                instrIr.add(convertirInstruction(i));
            }
        }
        return new IrBloc(instrIr);
    }

    private static IrInstruction convertirInstruction(Instruction i) {
        if (i == null) return new IrBloc(List.of());

        // Bloc
        if (i instanceof Bloc b) return convertirBloc(b);

        // Affectation
        if (i instanceof Affectation a) {
            String var = getStringViaGetterOrField(a, "getNom", "nom", "variable", "nomVariable");
            if (var == null) var = getStringViaGetterOrField(a, "getVariable", "var");
            IrExpression expr = convertirExpression(getExprViaGetterOrField(a, "getExpression", "expression", "expr"));
            return new IrAffectation(safeString(var, "<var?>"), expr);
        }

        // Retourne
        if (i instanceof Retourne r) {
            IrExpression expr = null;
            try {
                Expression astExpr = getExprViaGetterOrField(r, "getExpression", "expression", "expr");
                if (astExpr != null) expr = convertirExpression(astExpr);
            } catch (Exception ignored) {}
            return new IrRetourne(expr);
        }

        // Affiche
        if (i instanceof main.java.parseur.ast.Affiche a) {
            List<Expression> astArgs = a.getExpressions();
            List<IrExpression> argsIr = new ArrayList<>();
            if (astArgs != null) {
                for (Expression e : astArgs) argsIr.add(convertirExpression(e));
            }
            return new IrAffiche(argsIr, a.isNewline());
        }

        // AppelFonctionInstr
        if (i instanceof AppelFonctionInstr afi) {
            // ta classe contient "AppelFonction appel" (d'après ton parser)
            Expression ast = getExprViaGetterOrField(afi, "getAppel", "appel", "expression");
            if (ast == null) {
                // fallback: si le champ s'appelle autrement
                ast = getExprViaGetterOrField(afi, "getExpression", "expr");
            }
            return new IrExpressionInstr(convertirExpression(ast));
        }

        // Si
        if (i instanceof Si s) {
            IrExpression cond = convertirExpression(getExprViaGetterOrField(s, "getCondition", "condition", "cond"));
            Instruction alorsAst = (Instruction) getObjViaGetterOrField(s, "getAlorsInstr", "alorsInstr", "alors");
            Instruction sinonAst = (Instruction) getObjViaGetterOrField(s, "getSinonInstr", "sinonInstr", "sinon");

            IrInstruction alorsIr = convertirInstruction(alorsAst);
            IrInstruction sinonIr = (sinonAst == null) ? null : convertirInstruction(sinonAst);
            return new IrSi(cond, alorsIr, sinonIr);
        }

        // TantQue
        if (i instanceof TantQue t) {
            IrExpression cond = convertirExpression(getExprViaGetterOrField(t, "getCondition", "condition", "cond"));
            Instruction corpsAst = (Instruction) getObjViaGetterOrField(t, "getCorps", "corps", "body");
            return new IrTantQue(cond, convertirInstruction(corpsAst));
        }

        // Pour
        if (i instanceof Pour p) {
            String var = getStringViaGetterOrField(p, "getNomVariable", "nomVariable", "ident", "variable");
            Expression debut = getExprViaGetterOrField(p, "getDebut", "debut");
            Expression fin = getExprViaGetterOrField(p, "getFin", "fin");
            String op = getStringViaGetterOrField(p, "getOperateur", "operateur", "operateurPas");
            Expression pas = getExprViaGetterOrField(p, "getPas", "pas");
            Instruction corps = (Instruction) getObjViaGetterOrField(p, "getCorps", "corps", "body");

            return new IrPour(
                    safeString(var, "<i?>"),
                    convertirExpression(debut),
                    convertirExpression(fin),
                    safeString(op, "+="),
                    convertirExpression(pas),
                    convertirInstruction(corps)
            );
        }

        throw new IllegalArgumentException("Instruction AST non supportée: " + i.getClass().getName());
    }

    // =========================
    // Expressions
    // =========================

    private static IrExpression convertirExpression(Expression e) {
        if (e == null) return new IrVariable("<expr?>");

        // Lire
        if (e instanceof Lire) {
            return IrLire.INSTANCE;
        }

        // Nombre
        if (isInstanceOf(e, "main.java.parseur.ast.Nombre")) {
            Integer v = getIntViaGetterOrField(e, "getValeur", "valeur", "value", "n");
            if (v == null) {
                // parfois stocké dans "val" etc.
                v = getIntViaGetterOrField(e, "getVal", "val");
            }
            return new IrConstInt(v == null ? 0 : v);
        }

        // Texte
        if (isInstanceOf(e, "main.java.parseur.ast.Texte")) {
            String s = getStringViaGetterOrField(e, "getValeur", "valeur", "texte", "value");
            return new IrConstTexte(safeString(s, ""));
        }

        // Caractere
        if (isInstanceOf(e, "main.java.parseur.ast.Caractere")) {
            Character c = getCharViaGetterOrField(e, "getValeur", "valeur", "caractere", "value");
            return new IrConstChar(c == null ? '\0' : c);
        }

        // Identifiant (inclut tes booléens "true"/"false" que tu mets comme Identifiant)
        if (e instanceof main.java.parseur.ast.Identifiant id) {
            String nom = id.getNom();
            if ("true".equals(nom)) return new IrConstBool(true);
            if ("false".equals(nom)) return new IrConstBool(false);
            return new IrVariable(nom);
        }

        // ExpressionBinaire
        if (isInstanceOf(e, "main.java.parseur.ast.ExpressionBinaire")) {
            Expression g = getExprViaGetterOrField(e, "getGauche", "gauche", "left");
            Expression d = getExprViaGetterOrField(e, "getDroite", "droite", "right");
            String op = getStringViaGetterOrField(e, "getOperateur", "operateur", "op", "operator");

            return new IrBinaire(
                    convertirExpression(g),
                    safeString(op, "?"),
                    convertirExpression(d)
            );
        }

        // AppelFonction
        if (isInstanceOf(e, "main.java.parseur.ast.AppelFonction")) {
            String nom = getStringViaGetterOrField(e, "getNom", "nom", "nomFonction", "name");
            @SuppressWarnings("unchecked")
            List<Expression> argsAst = (List<Expression>) getObjViaGetterOrField(e, "getArgs", "args", "arguments");
            if (argsAst == null) argsAst = List.of();

            List<IrExpression> argsIr = new ArrayList<>();
            for (Expression a : argsAst) argsIr.add(convertirExpression(a));

            return new IrAppel(safeString(nom, "<f?>"), argsIr);
        }

        // Si tu rajoutes d'autres types d'expressions plus tard:
        throw new IllegalArgumentException("Expression AST non supportée: " + e.getClass().getName());
    }

    // =========================
    // Helpers mapping & reflection
    // =========================

    private static IrType mapType(TypeSimple t) {
        if (t == null) return IrType.OBJET;
        return switch (t) {
            case ENTIER -> IrType.ENTIER;
            case BOOLEEN -> IrType.BOOLEEN;
            case TEXTE -> IrType.TEXTE;
            case CARACTERE -> IrType.CARACTERE;
            case VIDE -> IrType.VIDE;
            default -> IrType.OBJET;
        };
    }

    private static boolean isInstanceOf(Object o, String fqcn) {
        return o != null && o.getClass().getName().equals(fqcn);
    }

    private static String safeString(String s, String def) {
        return (s == null) ? def : s;
    }

    private static Object getObjViaGetterOrField(Object target, String... names) {
        for (String name : names) {
            // 1) méthode getter exacte
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Exception ignored) {}

            // 2) champ direct
            try {
                Field f = findField(target.getClass(), name);
                if (f != null) {
                    f.setAccessible(true);
                    return f.get(target);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String getStringViaGetterOrField(Object target, String... names) {
        Object o = getObjViaGetterOrField(target, names);
        return (o instanceof String s) ? s : null;
    }

    private static Integer getIntViaGetterOrField(Object target, String... names) {
        Object o = getObjViaGetterOrField(target, names);
        if (o instanceof Integer i) return i;
        if (o instanceof Number n) return n.intValue();
        return null;
    }

    private static Character getCharViaGetterOrField(Object target, String... names) {
        Object o = getObjViaGetterOrField(target, names);
        if (o instanceof Character c) return c;
        if (o instanceof String s && s.length() == 1) return s.charAt(0);
        return null;
    }

    private static Expression getExprViaGetterOrField(Object target, String... names) {
        Object o = getObjViaGetterOrField(target, names);
        return (o instanceof Expression e) ? e : null;
    }

    private static Field findField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null && cur != Object.class) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }
}
