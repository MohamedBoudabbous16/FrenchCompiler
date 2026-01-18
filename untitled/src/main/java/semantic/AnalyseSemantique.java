package main.java.semantic;

import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;
import utils.diag.DiagnosticCollector;

import java.util.*;


/**
 * Effectue la vérification sémantique :
 *  - gère les tables de symboles (variables et paramètres)
 *  - vérifie l’arité et le type de retour des fonctions
 *  - infère et vérifie les types des expressions
 *  - interdit les usages interdits (ex. afficher ou retourner une valeur VIDE)
 */
public class AnalyseSemantique {

    //modification apres implementation de utils:
    private final DiagnosticCollector diags;
    public AnalyseSemantique(DiagnosticCollector diags) {
        this.diags = Objects.requireNonNull(diags, "diags");
    }

    /** Signature (arité et type de retour) d'une fonction. */
    private static class SignatureFonction {
        final int arite;
        final TypeSimple typeRetour;
        SignatureFonction(int arite, TypeSimple typeRetour) {
            this.arite = arite;
            this.typeRetour = typeRetour;
        }
    }

    /** Table globale : nom de fonction -> signature. */
    private final Map<String, SignatureFonction> signatures = new HashMap<>();

    /** Variables utilisées comme compteurs de boucle par fonction. */
    private final Map<String, Set<String>> loopVarsParFonction = new HashMap<>();

    /** Variables locales et leurs types par fonction. */
    private final Map<String, Map<String, TypeSimple>> varsParFonction = new HashMap<>();

    private final TableSymboles ts = new TableSymboles();
    private String fonctionCourante = "??";
    private TypeSimple retourCourant = null;

    /** Analyse toutes les classes et fonctions du programme. */
    public void verifier(Programme programme) {
        // Fonction prédéfinie lire() : arité 0, retourne un ENTIER
        if (!signatures.containsKey("lire")) {
            signatures.put("lire", new SignatureFonction(0, TypeSimple.ENTIER));
        }
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                verifierFonction(f);
            }
        }
        //on leve a la fin si des erreurs existent:
        if (diags.aDesErreurs()) {
            throw new ErreurSemantique(diags.formatTous());
        }
    }


    /** Retourne le type de retour de la fonction, ou INCONNU si elle n'existe pas. */
    public TypeSimple typeRetourDe(String nomFonction) {
        SignatureFonction sig = signatures.get(nomFonction);
        return (sig == null) ? TypeSimple.INCONNU : sig.typeRetour;
    }

    /** Retourne la map des variables (et leurs types) déclarées dans la fonction. */
    public Map<String, TypeSimple> variablesDe(String nomFonction) {
        return varsParFonction.getOrDefault(nomFonction, Map.of());
    }

    /** Retourne l’ensemble des variables utilisées comme compteurs de boucle. */
    public Set<String> loopVariablesDe(String nomFonction) {
        return loopVarsParFonction.getOrDefault(nomFonction, Collections.emptySet());
    }

    /** Vérifie une fonction : paramètres, corps, arité et type de retour. */
    private void verifierFonction(Fonction f) {
        fonctionCourante = f.getNom();
        varsParFonction.put(fonctionCourante, new HashMap<>());
        loopVarsParFonction.put(fonctionCourante, new HashSet<>());
        retourCourant = null;

        ts.entrerPortee();

        // Déclaration implicite des paramètres (ENTIER par défaut)
        for (String p : f.getParam()) {
            ts.declarer(p, TypeSimple.ENTIER, true);
            varsParFonction.get(fonctionCourante).put(p, TypeSimple.ENTIER);
        }

        // Vérifier le corps
        verifierBloc(f.getCorps());

        ts.sortirPortee();

        // Enregistrer la signature (arité + type retour)
        int arite = f.getParam().size();
        TypeSimple typeRetour = (retourCourant == null) ? TypeSimple.VIDE : retourCourant;
        signatures.put(fonctionCourante, new SignatureFonction(arite, typeRetour));
    }

    /** Vérifie les instructions d’un bloc (avec gestion des portées). */
    private void verifierBloc(Bloc bloc) {
        ts.entrerPortee();
        for (Instruction instr : bloc.getInstructions()) {
            verifierInstruction(instr);
        }
        ts.sortirPortee();
    }

    /** Vérifie une instruction selon son type (version diagnostics). */
    private void verifierInstruction(Instruction i) {
        if (i instanceof Bloc b) {
            verifierBloc(b);
            return;
        }

        if (i instanceof Affiche a) {
            for (Expression expr : a.getExpressions()) {
                TypeSimple t = typerExpression(expr);
                if (t == TypeSimple.VIDE) {
                    err("Impossible d'afficher une expression de type VIDE");
                }
            }
            return;
        }

        if (i instanceof AppelFonctionInstr afi) {
            // Vérifie l’appel, mais ignore sa valeur de retour
            typerExpression(afi.getAppel());
            return;
        }

        if (i instanceof Affectation a) {
            TypeSimple tExpr = typerExpression(a.getExpression());

            // Interdit d’affecter une expression VIDE
            if (tExpr == TypeSimple.VIDE) {
                err("Impossible d'affecter une expression de type VIDE.");
                return; // évite de déclarer/valider avec un type interdit
            }

            Symbole s = ts.resoudre(a.getNomVar());
            if (s == null) {
                try {
                    ts.declarer(a.getNomVar(), tExpr, false);
                    varsParFonction.get(fonctionCourante).put(a.getNomVar(), tExpr);
                } catch (RuntimeException ex) {
                    // si TableSymboles rejette la déclaration (doublon par ex.)
                    err("Variable '" + a.getNomVar() + "' déjà déclarée dans cette portée.");
                }
            } else if (tExpr != TypeSimple.INCONNU && s.getType() != tExpr) {
                // si tExpr est INCONNU, on évite l'effet domino
                err("Affectation incompatible : " + s.getType() + " = " + tExpr);
            }
            return;
        }

        if (i instanceof Retourne r) {
            TypeSimple t = typerExpression(r.getExpression());

            // Interdit de retourner une expression VIDE
            if (t == TypeSimple.VIDE) {
                err("Impossible de retourner une expression de type VIDE.");
                return;
            }

            // Si le type est INCONNU (suite à une erreur), on ne fixe pas retourCourant
            if (t == TypeSimple.INCONNU) {
                return;
            }

            if (retourCourant == null) {
                retourCourant = t;
            } else if (retourCourant != t) {
                err("Types de retour incompatibles : " + retourCourant + " et " + t);
            }
            return;
        }

        if (i instanceof Si s) {
            TypeSimple tCond = typerExpression(s.getCondition());
            if (tCond != TypeSimple.BOOLEEN && tCond != TypeSimple.INCONNU) {
                err("Condition de 'si' doit être BOOLEEN, trouvé : " + tCond);
            }
            verifierInstruction(s.getAlorsInstr());
            if (s.getSinonInstr() != null) {
                verifierInstruction(s.getSinonInstr());
            }
            return;
        }

        if (i instanceof TantQue tq) {
            TypeSimple tCond = typerExpression(tq.getCondition());
            if (tCond != TypeSimple.BOOLEEN && tCond != TypeSimple.INCONNU) {
                err("Condition de 'tantque' doit être BOOLEEN, trouvé : " + tCond);
            }
            verifierInstruction(tq.getCorps());
            return;
        }

        if (i instanceof Pour p) {
            // Noter la variable de boucle pour ne pas la déclarer en début de fonction
            loopVarsParFonction.get(fonctionCourante).add(p.getNomVar());

            // La variable doit être de type ENTIER
            Symbole s = ts.resoudre(p.getNomVar());
            if (s == null) {
                try {
                    ts.declarer(p.getNomVar(), TypeSimple.ENTIER, false);
                    varsParFonction.get(fonctionCourante).put(p.getNomVar(), TypeSimple.ENTIER);
                } catch (RuntimeException ex) {
                    err("Variable de boucle '" + p.getNomVar() + "' déjà déclarée dans cette portée.");
                }
            } else if (s.getType() != TypeSimple.ENTIER) {
                err("Variable de boucle '" + p.getNomVar() + "' doit être ENTIER.");
            }

            // Vérifier les bornes et le pas
            TypeSimple tDebut = typerExpression(p.getDebut());
            if (tDebut != TypeSimple.ENTIER && tDebut != TypeSimple.INCONNU) {
                err("Début du 'pour' doit être ENTIER.");
            }

            TypeSimple tFin = typerExpression(p.getFin());
            if (tFin != TypeSimple.ENTIER && tFin != TypeSimple.INCONNU) {
                err("Fin du 'pour' doit être ENTIER.");
            }

            TypeSimple tPas = typerExpression(p.getPas());
            if (tPas != TypeSimple.ENTIER && tPas != TypeSimple.INCONNU) {
                err("Pas du 'pour' doit être ENTIER.");
            }

            verifierInstruction(p.getCorps());
            return;
        }

        err("Instruction non gérée : " + i.getClass().getSimpleName());
    }

    /** Helper: ajoute une erreur avec le contexte de fonction courante. */
    private void err(String details) {
        diags.erreur(msg(details));
    }



    /** Infère le type d’une expression (version diagnostics). */
    private TypeSimple typerExpression(Expression e) {
        if (e instanceof Nombre)    return TypeSimple.ENTIER;
        if (e instanceof Texte)     return TypeSimple.TEXTE;
        if (e instanceof Caractere) return TypeSimple.CARACTERE;
        if (e instanceof Lire)      return TypeSimple.ENTIER;

        if (e instanceof Identifiant id) {
            // Constantes booléennes
            if ("true".equals(id.getNom()) || "false".equals(id.getNom())) {
                return TypeSimple.BOOLEEN;
            }

            // Résoudre l’identifiant
            Symbole s = ts.resoudre(id.getNom());
            if (s == null) {
                err("Identifiant '" + id.getNom() + "' utilisé avant affectation.");
                return TypeSimple.INCONNU;
            }
            return s.getType();
        }

        if (e instanceof AppelFonction a) {
            // Vérifier que la fonction existe
            TypeSimple typeRetour = typeRetourDe(a.getNom());
            if (typeRetour == TypeSimple.INCONNU) {
                err("Fonction inconnue : " + a.getNom());
                // on continue quand même pour relever d'autres erreurs dans les args
                for (Expression arg : a.getArgs()) typerExpression(arg);
                return TypeSimple.INCONNU;
            }

            // Vérifier l’arité
            int ariteAttendue = nombreParamsDe(a.getNom()); // version "safe"
            if (ariteAttendue >= 0 && a.getArgs().size() != ariteAttendue) {
                err("Mauvaise arité pour '" + a.getNom() + "' : attendu "
                        + ariteAttendue + ", trouvé " + a.getArgs().size());
            }

            // Vérifier chaque argument
            for (Expression arg : a.getArgs()) {
                typerExpression(arg);
            }

            return typeRetour;
        }

        if (e instanceof ExpressionBinaire b) {
            TypeSimple g = typerExpression(b.getGauche());
            TypeSimple d = typerExpression(b.getDroite());
            String op = b.getop();

            // Si un côté est déjà INCONNU, on limite l'effet domino
            if (g == TypeSimple.INCONNU || d == TypeSimple.INCONNU) {
                // Sauf pour && / || où on peut encore dire que c'est booléen "attendu"
                if ("&&".equals(op) || "||".equals(op)) return TypeSimple.BOOLEEN;
                return TypeSimple.INCONNU;
            }

            // Concaténation / addition
            if (op.equals("+")) {
                // TEXTE + (TEXTE|ENTIER|BOOLEEN|CARACTERE) -> TEXTE
                if (g == TypeSimple.TEXTE &&
                        (d == TypeSimple.TEXTE ||
                                d == TypeSimple.ENTIER ||
                                d == TypeSimple.BOOLEEN ||
                                d == TypeSimple.CARACTERE)) {
                    return TypeSimple.TEXTE;
                }
                // ENTIER + ENTIER -> ENTIER
                if (g == TypeSimple.ENTIER && d == TypeSimple.ENTIER) {
                    return TypeSimple.ENTIER;
                }

                err("Concaténation/addition invalide : '" + g + " + " + d + "'. "
                        + "Soit ENTIER+ENTIER, soit TEXTE à gauche.");
                return TypeSimple.INCONNU;
            }

            // Arithmétique
            if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    err("Opérateur '" + op + "' attend ENTIER,ENTIER.");
                    return TypeSimple.INCONNU;
                }
                return TypeSimple.ENTIER;
            }

            // Comparaison
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    err("Comparaison '" + op + "' attend ENTIER,ENTIER.");
                    return TypeSimple.INCONNU;
                }
                return TypeSimple.BOOLEEN;
            }

            // Égalité
            if (op.equals("==") || op.equals("!=")) {
                if (g != d) {
                    err("Test '" + op + "' attend deux opérandes du même type.");
                    return TypeSimple.INCONNU;
                }
                return TypeSimple.BOOLEEN;
            }

            // Logique
            if (op.equals("&&") || op.equals("||")) {
                if (g != TypeSimple.BOOLEEN || d != TypeSimple.BOOLEEN) {
                    err("Opérateur logique '" + op + "' attend BOOLEEN,BOOLEEN.");
                    return TypeSimple.INCONNU;
                }
                return TypeSimple.BOOLEEN;
            }

            err("Opérateur binaire inconnu : " + op);
            return TypeSimple.INCONNU;
        }

        err("Expression non gérée : " + e.getClass().getSimpleName());
        return TypeSimple.INCONNU;
    }

    /** Message d’erreur avec le nom de la fonction courante. */
    private String msg(String details) {
        return "[Fonction " + fonctionCourante + "] " + details;
    }

    /** Retourne l’arité d’une fonction ; si inconnue, ajoute un diagnostic et renvoie -1. */
    private int nombreParamsDe(String nomFonction) {
        SignatureFonction sig = signatures.get(nomFonction);
        if (sig == null) {
            err("Fonction inconnue : " + nomFonction);
            return -1;
        }
        return sig.arite;
    }



}
