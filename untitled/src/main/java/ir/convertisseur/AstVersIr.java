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
            String var = safeString(a.getNomVar(), "<var?>");
            IrExpression expr = convertirExpression(a.getExpression());
            return new IrAffectation(var, expr);
        }

        // Retourne
        if (i instanceof Retourne r) {
            IrExpression expr = (r.getExpression() == null) ? null : convertirExpression(r.getExpression());
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
            AppelFonction appel = afi.getAppel();
            return new IrExpressionInstr(convertirExpression(appel));
        }

        // Si
        if (i instanceof Si s) {
            IrExpression cond = convertirExpression(s.getCondition());
            IrInstruction alorsIr = convertirInstruction(s.getAlorsInstr());
            IrInstruction sinonIr = (s.getSinonInstr() == null) ? null : convertirInstruction(s.getSinonInstr());
            return new IrSi(cond, alorsIr, sinonIr);
        }

        // TantQue
        if (i instanceof TantQue t) {
            IrExpression cond = convertirExpression(t.getCondition());
            IrInstruction corpsIr = convertirInstruction(t.getCorps());
            return new IrTantQue(cond, corpsIr);
        }

        // Pour
        if (i instanceof Pour p) {
            return new IrPour(
                    safeString(p.getNomVar(), "<i?>"),
                    convertirExpression(p.getDebut()),
                    convertirExpression(p.getFin()),
                    safeString(p.getOperateur(), "+="),
                    convertirExpression(p.getPas()),
                    convertirInstruction(p.getCorps())
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
        if (e instanceof Nombre n) {
            return new IrConstInt(n.getValeur());
        }

        // Texte
        if (e instanceof Texte t) {
            return new IrConstTexte(safeString(t.getValeur(), ""));
        }

        // Caractere
        if (e instanceof Caractere c) {
            return new IrConstChar(c.getValeur());
        }

        // Identifiant (inclut tes booléens "true"/"false" que tu mets comme Identifiant)
        if (e instanceof Identifiant id) {
            String nom = id.getNom();
            if ("true".equals(nom)) return new IrConstBool(true);
            if ("false".equals(nom)) return new IrConstBool(false);
            return new IrVariable(nom);
        }

        // ExpressionBinaire
        if (e instanceof ExpressionBinaire b) {
            String op = null;

            // chez toi: getop() (minuscule) -> on tente direct puis fallback
            try { op = b.getop(); } catch (Exception ignored) {}
            if (op == null) op = getStringViaGetterOrField(b, "getOp", "getOperateur", "getop", "op", "operateur");

            return new IrBinaire(
                    convertirExpression(b.getGauche()),
                    safeString(op, "?"),
                    convertirExpression(b.getDroite())
            );
        }

        // AppelFonction
        if (e instanceof AppelFonction a) {
            List<IrExpression> argsIr = new ArrayList<>();
            if (a.getArgs() != null) {
                for (Expression arg : a.getArgs()) argsIr.add(convertirExpression(arg));
            }
            return new IrAppel(safeString(a.getNom(), "<f?>"), argsIr);
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

    private static String safeString(String s, String def) {
        return (s == null) ? def : s;
    }

    private static Object getObjViaGetterOrField(Object target, String... names) {
        if (target == null) return null;

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
