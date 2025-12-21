package semantic;

import parseur.ast.*;
import parseur.ast.controle.Pour;
import parseur.ast.controle.Si;
import parseur.ast.controle.TantQue;

import java.util.*;

/**
 * Effectue la vérification sémantique :
 *  - gère les tables de symboles (variables et paramètres)
 *  - vérifie l’arité et le type de retour des fonctions
 *  - infère et vérifie les types des expressions
 *  - interdit les usages interdits (ex. afficher ou retourner une valeur VIDE)
 */
public class AnalyseSemantique {

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
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                verifierFonction(f);
            }
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

    /** Vérifie une instruction selon son type. */
    private void verifierInstruction(Instruction i) {
        if (i instanceof Bloc b) {
            verifierBloc(b);
            return;
        }
        if (i instanceof parseur.ast.Affiche a) {
            TypeSimple t = typerExpression(a.getExpression());
            if (t == TypeSimple.VIDE) {
                throw new ErreurSemantique(msg("Impossible d'afficher une expression de type VIDE"));
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
                throw new ErreurSemantique(msg("Impossible d'affecter une expression de type VIDE."));
            }
            Symbole s = ts.resoudre(a.getNomVar());
            if (s == null) {
                ts.declarer(a.getNomVar(), tExpr, false);
                varsParFonction.get(fonctionCourante).put(a.getNomVar(), tExpr);
            } else if (s.getType() != tExpr) {
                throw new ErreurSemantique(msg("Affectation incompatible : " + s.getType() + " = " + tExpr));
            }
            return;
        }
        if (i instanceof Retourne r) {
            TypeSimple t = typerExpression(r.getExpression());
            // Interdit de retourner une expression VIDE
            if (t == TypeSimple.VIDE) {
                throw new ErreurSemantique(msg("Impossible de retourner une expression de type VIDE."));
            }
            if (retourCourant == null) {
                retourCourant = t;
            } else if (retourCourant != t) {
                throw new ErreurSemantique(msg("Types de retour incompatibles : " + retourCourant + " et " + t));
            }
            return;
        }
        if (i instanceof Si s) {
            TypeSimple tCond = typerExpression(s.getCondition());
            if (tCond != TypeSimple.BOOLEEN) {
                throw new ErreurSemantique(msg("Condition de 'si' doit être BOOLEEN, trouvé : " + tCond));
            }
            verifierInstruction(s.getAlorsInstr());
            if (s.getSinonInstr() != null) {
                verifierInstruction(s.getSinonInstr());
            }
            return;
        }
        if (i instanceof TantQue tq) {
            TypeSimple tCond = typerExpression(tq.getCondition());
            if (tCond != TypeSimple.BOOLEEN) {
                throw new ErreurSemantique(msg("Condition de 'tantque' doit être BOOLEEN, trouvé : " + tCond));
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
                ts.declarer(p.getNomVar(), TypeSimple.ENTIER, false);
                varsParFonction.get(fonctionCourante).put(p.getNomVar(), TypeSimple.ENTIER);
            } else if (s.getType() != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Variable de boucle '" + p.getNomVar() + "' doit être ENTIER."));
            }

            // Vérifier les bornes et le pas
            if (typerExpression(p.getDebut()) != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Début du 'pour' doit être ENTIER."));
            }
            if (typerExpression(p.getFin()) != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Fin du 'pour' doit être ENTIER."));
            }
            if (typerExpression(p.getPas()) != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Pas du 'pour' doit être ENTIER."));
            }

            verifierInstruction(p.getCorps());
            return;
        }

        throw new ErreurSemantique(msg("Instruction non gérée : " + i.getClass().getSimpleName()));
    }

    /** Infère le type d’une expression. */
    private TypeSimple typerExpression(Expression e) {
        if (e instanceof Nombre)    return TypeSimple.ENTIER;
        if (e instanceof Texte)     return TypeSimple.TEXTE;
        if (e instanceof Caractere) return TypeSimple.CARACTERE;

        if (e instanceof Identifiant id) {
            // Constantes booléennes
            if ("true".equals(id.getNom()) || "false".equals(id.getNom())) {
                return TypeSimple.BOOLEEN;
            }
            // Résoudre l’identifiant
            Symbole s = ts.resoudre(id.getNom());
            if (s == null) {
                throw new ErreurSemantique(msg("Identifiant '" + id.getNom() + "' utilisé avant affectation."));
            }
            return s.getType();
        }

        if (e instanceof AppelFonction a) {
            // 1️⃣ Vérifier que la fonction existe
            TypeSimple typeRetour = typeRetourDe(a.getNom());
            if (typeRetour == TypeSimple.INCONNU) {
                throw new ErreurSemantique(msg("Fonction inconnue : " + a.getNom()));
            }
            // 2️⃣ Vérifier l’arité
            int ariteAttendue = nombreParamsDe(a.getNom());
            if (a.getArgs().size() != ariteAttendue) {
                throw new ErreurSemantique(msg("Mauvaise arité pour '" + a.getNom() +
                        "' : attendu " + ariteAttendue + ", trouvé " + a.getArgs().size()));
            }
            // 3️⃣ Vérifier chaque argument
            for (Expression arg : a.getArgs()) {
                typerExpression(arg);
            }
            // 4️⃣ Retourner le type de retour de la fonction
            return typeRetour;
        }

        if (e instanceof ExpressionBinaire b) {
            TypeSimple g = typerExpression(b.getGauche());
            TypeSimple d = typerExpression(b.getDroite());
            String op = b.getop();

            // Concaténation de chaînes
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
                throw new ErreurSemantique(msg("Concaténation invalide : '" + g + " + " + d + "'. TEXTE doit être à gauche."));
            }

            // Arithmétique
            if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    throw new ErreurSemantique(msg("Opérateur '" + op + "' attend ENTIER,ENTIER."));
                }
                return TypeSimple.ENTIER;
            }

            // Comparaison
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    throw new ErreurSemantique(msg("Comparaison '" + op + "' attend ENTIER,ENTIER."));
                }
                return TypeSimple.BOOLEEN;
            }

            // Égalité
            if (op.equals("==") || op.equals("!=")) {
                if (g != d) {
                    throw new ErreurSemantique(msg("Test '" + op + "' attend deux opérandes du même type."));
                }
                return TypeSimple.BOOLEEN;
            }

            // Logique
            if (op.equals("&&") || op.equals("||")) {
                if (g != TypeSimple.BOOLEEN || d != TypeSimple.BOOLEEN) {
                    throw new ErreurSemantique(msg("Opérateur logique '" + op + "' attend BOOLEEN,BOOLEEN."));
                }
                return TypeSimple.BOOLEEN;
            }

            throw new ErreurSemantique(msg("Opérateur binaire inconnu : " + op));
        }

        throw new ErreurSemantique(msg("Expression non gérée : " + e.getClass().getSimpleName()));
    }

    /** Message d’erreur avec le nom de la fonction courante. */
    private String msg(String details) {
        return "[Fonction " + fonctionCourante + "] " + details;
    }

    /** Retourne l’arité d’une fonction ou lève une erreur si elle n’existe pas. */
    private int nombreParamsDe(String nomFonction) {
        SignatureFonction sig = signatures.get(nomFonction);
        if (sig == null) {
            throw new ErreurSemantique(msg("Fonction inconnue : " + nomFonction));
        }
        return sig.arite;
    }
}
