package main.java.semantic;

import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;
import utils.diag.DiagnosticCollector;
import main.java.parseur.ast.Expression;


import utils.diag.Position;
import main.java.semantic.TypeSimple;
import  main.java.semantic.TableSymboles;

import java.util.*;

/**
 * Vérification sémantique + inférence de types :
 *  - construit une table des signatures de fonctions (arité + types params + type retour)
 *  - infère les types des variables/params (INCONNU -> ENTIER/BOOLEEN/TEXTE/CARACTERE)
 *  - vérifie arité des appels, retours cohérents, usages interdits (VIDE)
 *  - remplit varsParFonction et loopVarsParFonction pour la génération Java
 */
public class AnalyseSemantique {

    private final DiagnosticCollector diags;
    // Types d'expressions (clé = objet AST exact)
    private final Map<Expression, TypeSimple> typesExpr = new IdentityHashMap<>();



    public AnalyseSemantique(DiagnosticCollector diags) {
        this.diags = Objects.requireNonNull(diags, "diags");
    }

    /** Signature complète : arité + types des paramètres + type de retour. */
    private static class SignatureFonction {
        final int arite;
        final List<TypeSimple> typesParams; // même ordre que la liste param de la fonction
        TypeSimple typeRetour;

        SignatureFonction(int arite, List<TypeSimple> typesParams, TypeSimple typeRetour) {
            this.arite = arite;
            this.typesParams = new ArrayList<>(typesParams);
            this.typeRetour = typeRetour;
        }
    }

    /** nom fonction -> signature */
    private final Map<String, SignatureFonction> signatures = new HashMap<>();

    /** Variables utilisées comme compteurs de boucle par fonction. */
    private final Map<String, Set<String>> loopVarsParFonction = new HashMap<>();

    /** Variables (incluant paramètres) et leurs types par fonction. */
    private final Map<String, Map<String, TypeSimple>> varsParFonction = new HashMap<>();

    private final TableSymboles ts = new TableSymboles();

    private String fonctionCourante = "??";
    private TypeSimple retourCourant = null;

    /* =========================
       API utilisée par codegen
       ========================= */

    public TypeSimple typeRetourDe(String nomFonction) {
        SignatureFonction sig = signatures.get(nomFonction);
        return (sig == null) ? TypeSimple.INCONNU : sig.typeRetour;
    }

    public Map<String, TypeSimple> variablesDe(String nomFonction) {
        return varsParFonction.getOrDefault(nomFonction, Map.of());
    }

    public Set<String> loopVariablesDe(String nomFonction) {
        return loopVarsParFonction.getOrDefault(nomFonction, Collections.emptySet());
    }

    public List<TypeSimple> typesParamsDe(String nomFonction) {
        SignatureFonction sig = signatures.get(nomFonction);
        if (sig == null || sig.typesParams == null) return List.of();
        return Collections.unmodifiableList(sig.typesParams);
    }

    /** Type statique inféré d'une expression (rempli pendant verifier()). */
    public TypeSimple typeDe(Expression e) {
        return typesExpr.getOrDefault(e, TypeSimple.INCONNU);
    }

    private TypeSimple record(Expression e, TypeSimple t) {
        typesExpr.put(e, t);
        return t;
    }


    /* =========================
              VERIFIER
       ========================= */

    public void verifier(Programme programme) {
        // (optionnel) : si tu veux repartir clean à chaque run
        // diags.vider();

        signatures.clear();
        varsParFonction.clear();
        loopVarsParFonction.clear();
        typesExpr.clear();


        // Builtins
        signatures.put("lire", new SignatureFonction(0, List.of(), TypeSimple.ENTIER));
        signatures.put("vide", new SignatureFonction(0, List.of(), TypeSimple.VIDE));

        // PASS 1 : enregistrer toutes les signatures (au moins l’arité)
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                String nom = f.getNom();

                if (signatures.containsKey(nom)) {
                    err("Fonction '" + nom + "' redéfinie.", f.getPosition());
                    continue;
                }

                int arite = f.getParam().size();
                List<TypeSimple> params = new ArrayList<>();
                for (int i = 0; i < arite; i++) params.add(TypeSimple.INCONNU);

                signatures.put(nom, new SignatureFonction(arite, params, TypeSimple.INCONNU));
            }
        }

        // PASS 2 : analyser les corps (inférence params/vars + retours + arité appels)
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                verifierFonction(f);
            }
        }

        if (diags.aDesErreurs()) {
            throw new ErreurSemantique(diags.formatTous());
        }
    }

    /* =========================
           VERIFIER FONCTION
       ========================= */

    private void verifierFonction(Fonction f) {
        fonctionCourante = f.getNom();
        retourCourant = null;

        varsParFonction.put(fonctionCourante, new HashMap<>());
        loopVarsParFonction.put(fonctionCourante, new HashSet<>());

        ts.entrerPortee();

        // 1) Paramètres : commencent en INCONNU (inférence ensuite)
        List<String> params = f.getParam();
        for (String p : params) {
            // doublon param
            if (ts.resoudre(p) != null) {
                err("Paramètre dupliqué '" + p + "'.", f.getPosition());
                continue;
            }
            ts.declarer(p, TypeSimple.INCONNU, true);
            varsParFonction.get(fonctionCourante).put(p, TypeSimple.INCONNU);
        }

        // 2) Corps
        verifierBloc(f.getCorps());

        ts.sortirPortee();

        // 3) Retour : inféré
        TypeSimple typeRetour = (retourCourant == null) ? TypeSimple.VIDE : retourCourant;

        // 4) Mettre à jour signature globale (retour + types params)
        SignatureFonction sig = signatures.get(fonctionCourante);
        if (sig != null) {
            sig.typeRetour = typeRetour;

            for (int i = 0; i < params.size() && i < sig.typesParams.size(); i++) {
                TypeSimple t = varsParFonction.get(fonctionCourante).getOrDefault(params.get(i), TypeSimple.INCONNU);
                // si on a appris un type, on l’enregistre
                if (sig.typesParams.get(i) == TypeSimple.INCONNU && t != TypeSimple.INCONNU) {
                    sig.typesParams.set(i, t);
                }
            }
        } else {
            // ne devrait pas arriver (PASS 1 normalement)
            int arite = params.size();
            List<TypeSimple> typesParams = new ArrayList<>();
            for (int i = 0; i < arite; i++) typesParams.add(TypeSimple.INCONNU);
            signatures.put(fonctionCourante, new SignatureFonction(arite, typesParams, typeRetour));
        }
    }

    private void verifierBloc(Bloc bloc) {
        ts.entrerPortee();
        for (Instruction instr : bloc.getInstructions()) {
            verifierInstruction(instr);
        }
        ts.sortirPortee();
    }

    /* =========================
            INSTRUCTIONS
       ========================= */

    private void verifierInstruction(Instruction i) {
        if (i instanceof Bloc b) {
            verifierBloc(b);
            return;
        }

        if (i instanceof Affiche a) {
            for (Expression expr : a.getExpressions()) {
                TypeSimple t = typerExpression(expr);
                if (t == TypeSimple.VIDE) {
                    err("Impossible d'afficher une expression de type VIDE.", expr.getPosition());
                }
            }
            return;
        }

        if (i instanceof AppelFonctionInstr afi) {
            typerExpression(afi.getAppel()); // check arité etc
            return;
        }

        if (i instanceof Affectation a) {
            TypeSimple tExpr = typerExpression(a.getExpression());

            if (tExpr == TypeSimple.VIDE) {
                err("Impossible d'affecter une expression de type VIDE.", a.getPosition());
                return;
            }

            Symbole s = ts.resoudre(a.getNomVar());
            if (s == null) {
                // première affectation => déclaration implicite
                ts.declarer(a.getNomVar(), tExpr, false);
                varsParFonction.get(fonctionCourante).put(a.getNomVar(), tExpr);
            } else {
                TypeSimple tVar = s.getType();

                // INCONNU := connu  -> on apprend
                if (tVar == TypeSimple.INCONNU && tExpr != TypeSimple.INCONNU) {
                    s.setType(tExpr);
                    varsParFonction.get(fonctionCourante).put(a.getNomVar(), tExpr);
                }
                // connu := INCONNU -> ok (on ne sait pas)
                else if (tVar != TypeSimple.INCONNU && tExpr == TypeSimple.INCONNU) {
                    // rien
                }
                // connu := connu différent -> erreur
                else if (tVar != TypeSimple.INCONNU && tExpr != TypeSimple.INCONNU && tVar != tExpr) {
                    err("Affectation incompatible : " + tVar + " = " + tExpr, a.getPosition());
                }
            }
            return;
        }

        if (i instanceof Retourne r) {
            TypeSimple t = typerExpression(r.getExpression());

            if (t == TypeSimple.VIDE) {
                err("Impossible de retourner une expression de type VIDE.", r.getPosition());
                return;
            }

            if (t == TypeSimple.INCONNU) {
                // on ne fixe pas le retour tant qu’on ne sait pas
                return;
            }

            if (retourCourant == null) {
                retourCourant = t;
            } else if (retourCourant != TypeSimple.INCONNU && retourCourant != t) {
                err("Types de retour incompatibles : " + retourCourant + " et " + t, r.getPosition());
                retourCourant = TypeSimple.INCONNU; // évite de propager un mauvais type
            }
            return;
        }

        if (i instanceof Si s) {
            TypeSimple tCond = typerExpression(s.getCondition());

            // si condition = ident INCONNU, on peut l’inférer booléen
            if (tCond == TypeSimple.INCONNU && (s.getCondition() instanceof Identifiant id) && !estConstBool(id)) {
                infererIdentifiant(id.getNom(), TypeSimple.BOOLEEN, id.getPosition());
                tCond = TypeSimple.BOOLEEN;
            }

            if (tCond != TypeSimple.BOOLEEN && tCond != TypeSimple.INCONNU) {
                err("Condition de 'si' doit être BOOLEEN, trouvé : " + tCond, s.getPosition());
            }

            verifierInstruction(s.getAlorsInstr());
            if (s.getSinonInstr() != null) verifierInstruction(s.getSinonInstr());
            return;
        }

        if (i instanceof TantQue tq) {
            TypeSimple tCond = typerExpression(tq.getCondition());

            if (tCond == TypeSimple.INCONNU && (tq.getCondition() instanceof Identifiant id) && !estConstBool(id)) {
                infererIdentifiant(id.getNom(), TypeSimple.BOOLEEN, id.getPosition());
                tCond = TypeSimple.BOOLEEN;
            }

            if (tCond != TypeSimple.BOOLEEN && tCond != TypeSimple.INCONNU) {
                err("Condition de 'tantque' doit être BOOLEEN, trouvé : " + tCond, tq.getPosition());
            }

            verifierInstruction(tq.getCorps());
            return;
        }

        if (i instanceof Pour p) {
            loopVarsParFonction.get(fonctionCourante).add(p.getNomVar());

            Symbole s = ts.resoudre(p.getNomVar());
            if (s == null) {
                ts.declarer(p.getNomVar(), TypeSimple.ENTIER, false);
                varsParFonction.get(fonctionCourante).put(p.getNomVar(), TypeSimple.ENTIER);
            } else if (s.getType() == TypeSimple.INCONNU) {
                s.setType(TypeSimple.ENTIER);
                varsParFonction.get(fonctionCourante).put(p.getNomVar(), TypeSimple.ENTIER);
            } else if (s.getType() != TypeSimple.ENTIER) {
                err("Variable de boucle '" + p.getNomVar() + "' doit être ENTIER.", p.getPosition());
            }

            // bornes/pas ENTIER (et inférence si ident inconnus)
            expectTypeEntier(p.getDebut());
            expectTypeEntier(p.getFin());
            expectTypeEntier(p.getPas());

            verifierInstruction(p.getCorps());
            return;
        }

        err("Instruction non gérée : " + i.getClass().getSimpleName(), i.getPosition());
    }

    /* =========================
             EXPRESSIONS
       ========================= */

    private TypeSimple typerExpression(Expression e) {
        if (e instanceof Nombre)    return record(e, TypeSimple.ENTIER);
        if (e instanceof Texte)     return record(e, TypeSimple.TEXTE);
        if (e instanceof Caractere) return record(e, TypeSimple.CARACTERE);
        if (e instanceof Lire)      return record(e, TypeSimple.ENTIER);

        if (e instanceof Identifiant id) {
            if (estConstBool(id)) return record(e, TypeSimple.BOOLEEN);

            Symbole s = ts.resoudre(id.getNom());
            if (s == null) {
                err("Identifiant '" + id.getNom() + "' utilisé avant affectation.", id.getPosition());
                return record(e, TypeSimple.INCONNU);
            }
            return record(e, s.getType());
        }

        if (e instanceof AppelFonction a) {
            SignatureFonction sig = signatures.get(a.getNom());
            if (sig == null) {
                err("Fonction inconnue : " + a.getNom(), a.getPosition());
                for (Expression arg : a.getArgs()) typerExpression(arg);
                return record(e, TypeSimple.INCONNU);
            }

            if (a.getArgs().size() != sig.arite) {
                err("Mauvaise arité pour '" + a.getNom() + "' : attendu "
                        + sig.arite + ", trouvé " + a.getArgs().size(), a.getPosition());
            }

            int n = Math.min(a.getArgs().size(), sig.typesParams.size());
            for (int i = 0; i < a.getArgs().size(); i++) {
                TypeSimple tArg = typerExpression(a.getArgs().get(i));
                if (i < n) {
                    TypeSimple tParam = sig.typesParams.get(i);

                    if (tParam == TypeSimple.INCONNU && tArg != TypeSimple.INCONNU && tArg != TypeSimple.VIDE) {
                        sig.typesParams.set(i, tArg);
                    } else if (tParam != TypeSimple.INCONNU && tArg != TypeSimple.INCONNU && tParam != tArg) {
                        err("Argument " + (i + 1) + " de '" + a.getNom()
                                + "' incompatible : attendu " + tParam + ", trouvé " + tArg, a.getPosition());
                    }
                }
            }

            return record(e, sig.typeRetour);
        }

        if (e instanceof ExpressionBinaire b) {
            String op = b.getop();

            TypeSimple g = typerExpression(b.getGauche());
            TypeSimple d = typerExpression(b.getDroite());

            g = infereSelonContexte(op, b.getGauche(), g, d);
            d = infereSelonContexte(op, b.getDroite(), d, g);

            // +
            if ("+".equals(op)) {
                if (g == TypeSimple.TEXTE) {
                    if (d == TypeSimple.VIDE) {
                        err("Concaténation invalide : TEXTE + VIDE.", b.getPosition());
                        return record(e, TypeSimple.INCONNU);
                    }
                    return record(e, TypeSimple.TEXTE);
                }
                if (g == TypeSimple.ENTIER && d == TypeSimple.ENTIER) return record(e, TypeSimple.ENTIER);
                if (g == TypeSimple.INCONNU || d == TypeSimple.INCONNU) return record(e, TypeSimple.INCONNU);
                err("Addition invalide : '" + g + " + " + d + "'.", b.getPosition());
                return record(e, TypeSimple.INCONNU);
            }

            // arith
            if ("-".equals(op) || "*".equals(op) || "/".equals(op) || "%".equals(op)) {
                if (g == TypeSimple.ENTIER && d == TypeSimple.ENTIER) return record(e, TypeSimple.ENTIER);
                if (g == TypeSimple.INCONNU || d == TypeSimple.INCONNU) return record(e, TypeSimple.INCONNU);
                err("Opérateur '" + op + "' attend ENTIER,ENTIER.", b.getPosition());
                return record(e, TypeSimple.INCONNU);
            }

            // compare
            if ("<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op)) {
                if (g == TypeSimple.ENTIER && d == TypeSimple.ENTIER) return record(e, TypeSimple.BOOLEEN);
                if (g == TypeSimple.INCONNU || d == TypeSimple.INCONNU) return record(e, TypeSimple.INCONNU);
                err("Comparaison '" + op + "' attend ENTIER,ENTIER.", b.getPosition());
                return record(e, TypeSimple.INCONNU);
            }

            // == !=
            if ("==".equals(op) || "!=".equals(op)) {
                if (g == TypeSimple.INCONNU || d == TypeSimple.INCONNU) return record(e, TypeSimple.BOOLEEN);
                if (g != d) {
                    err("Test '" + op + "' attend deux opérandes du même type.", b.getPosition());
                    return record(e, TypeSimple.INCONNU);
                }
                return record(e, TypeSimple.BOOLEEN);
            }

            // logique
            if ("&&".equals(op) || "||".equals(op)) {
                if (g == TypeSimple.BOOLEEN && d == TypeSimple.BOOLEEN) return record(e, TypeSimple.BOOLEEN);
                if (g == TypeSimple.INCONNU || d == TypeSimple.INCONNU) return record(e, TypeSimple.BOOLEEN); // tolérance
                err("Opérateur logique '" + op + "' attend BOOLEEN,BOOLEEN.", b.getPosition());
                return record(e, TypeSimple.INCONNU);
            }

            err("Opérateur binaire inconnu : " + op, b.getPosition());
            return record(e, TypeSimple.INCONNU);
        }

        err("Expression non gérée : " + e.getClass().getSimpleName(), e.getPosition());
        return record(e, TypeSimple.INCONNU);
    }

    /* =========================
           INFÉRENCE HELPERS
       ========================= */

    private boolean estConstBool(Identifiant id) {
        return "true".equals(id.getNom()) || "false".equals(id.getNom());
    }

    private void expectTypeEntier(Expression e) {
        TypeSimple t = typerExpression(e);
        if (t == TypeSimple.ENTIER) return;

        if (t == TypeSimple.INCONNU && e instanceof Identifiant id && !estConstBool(id)) {
            infererIdentifiant(id.getNom(), TypeSimple.ENTIER, id.getPosition());
            return;
        }

        if (t != TypeSimple.INCONNU) {
            err("Expression attendue ENTIER, trouvé : " + t, e.getPosition());
        }
    }

    /**
     * Infère un identifiant INCONNU vers un type attendu.
     * Si déjà typé différemment, diag.
     */
    private void infererIdentifiant(String nom, TypeSimple attendu, Position pos) {
        Symbole s = ts.resoudre(nom);
        if (s == null) return;

        TypeSimple actuel = s.getType();
        if (actuel == TypeSimple.INCONNU && attendu != TypeSimple.INCONNU) {
            s.setType(attendu);
            varsParFonction.get(fonctionCourante).put(nom, attendu);
        } else if (actuel != TypeSimple.INCONNU && attendu != TypeSimple.INCONNU && actuel != attendu) {
            err("Type incompatible pour '" + nom + "' : attendu " + attendu + ", trouvé " + actuel, pos);
        }
    }

    /**
     * Inférence contextuelle pour opérations binaires :
     *  - && || => booléen
     *  - - * / % < <= > >= => entier
     *  - == != => si autre côté connu, on pousse le même type
     *  - + => si l’autre côté TEXTE/BOOLEEN/CARACTERE => TEXTE ; si l’autre ENTIER => ENTIER (choix add par défaut)
     */
    private TypeSimple infereSelonContexte(String op, Expression expr, TypeSimple tExpr, TypeSimple tAutre) {
        if (tExpr != TypeSimple.INCONNU) return tExpr;
        if (!(expr instanceof Identifiant id) || estConstBool(id)) return tExpr;

        if ("&&".equals(op) || "||".equals(op)) {
            infererIdentifiant(id.getNom(), TypeSimple.BOOLEEN, id.getPosition());
            return TypeSimple.BOOLEEN;
        }

        if ("-".equals(op) || "*".equals(op) || "/".equals(op) || "%".equals(op)
                || "<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op)) {
            infererIdentifiant(id.getNom(), TypeSimple.ENTIER, id.getPosition());
            return TypeSimple.ENTIER;
        }

        if ("==".equals(op) || "!=".equals(op)) {
            if (tAutre != TypeSimple.INCONNU && tAutre != TypeSimple.VIDE) {
                infererIdentifiant(id.getNom(), tAutre, id.getPosition());
                return tAutre;
            }
            return TypeSimple.INCONNU;
        }

        if ("+".equals(op)) {
            if (tAutre == TypeSimple.TEXTE) {
                infererIdentifiant(id.getNom(), TypeSimple.TEXTE, id.getPosition());
                return TypeSimple.TEXTE;
            }
            if (tAutre == TypeSimple.BOOLEEN || tAutre == TypeSimple.CARACTERE) {
                // ENTIER + BOOLEEN interdit => on force concat
                infererIdentifiant(id.getNom(), TypeSimple.TEXTE, id.getPosition());
                return TypeSimple.TEXTE;
            }
            if (tAutre == TypeSimple.ENTIER) {
                // choix : + par défaut en addition si l'autre est ENTIER
                infererIdentifiant(id.getNom(), TypeSimple.ENTIER, id.getPosition());
                return TypeSimple.ENTIER;
            }
        }

        return TypeSimple.INCONNU;
    }

    /* =========================
              DIAGS
       ========================= */

    private void err(String details, Position pos) {
        diags.erreur(msg(details), pos);
    }

    private String msg(String details) {
        return "[Fonction " + fonctionCourante + "] " + details;
    }
}
