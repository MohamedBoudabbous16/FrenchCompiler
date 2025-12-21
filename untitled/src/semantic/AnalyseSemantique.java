package semantic;

import parseur.ast.*;
import parseur.ast.controle.Pour;
import parseur.ast.controle.Si;
import parseur.ast.controle.TantQue;

import java.util.HashMap;
import java.util.Map;

public class AnalyseSemantique {
    // nomFonction -> arité
    private final Map<String, Integer> ariteParFonction = new HashMap<>();
    private final Map<String, TypeSimple> retourParFonction = new HashMap<>();
    private TypeSimple retourCourant = null;
    private final Map<String, Map<String, TypeSimple>> varsParFonction = new HashMap<>();
    private final TableSymboles ts = new TableSymboles();
    private String fonctionCourante = "??";

    public void verifier(Programme programme) {
        for (Classe c : programme.getClasses()) {
            for (Fonction f : c.getFonctions()) {
                verifierFonction(f);
            }
        }
    }
    public TypeSimple typeRetourDe(String nomFonction) {
        return retourParFonction.getOrDefault(nomFonction, TypeSimple.INCONNU);
    }


    public Map<String, TypeSimple> variablesDe(String nomFonction) {
        return varsParFonction.getOrDefault(nomFonction, Map.of());
    }

    private void verifierFonction(Fonction f) {
        fonctionCourante = f.getNom();

        // ✅ ENREGISTRER L’ARITÉ DE LA FONCTION
        ariteParFonction.put(fonctionCourante, f.getParam().size());

        // Table des variables inférées de cette fonction
        varsParFonction.put(fonctionCourante, new HashMap<>());

        retourCourant = null;
        ts.entrerPortee(); // portée de la fonction

        // Paramètres : typés ENTIER par défaut
        for (String p : f.getParam()) {
            ts.declarer(p, TypeSimple.ENTIER, true);
            varsParFonction.get(fonctionCourante).put(p, TypeSimple.ENTIER);
        }

        verifierBloc(f.getCorps());

        ts.sortirPortee();

        // Type de retour final
        retourParFonction.put(
                fonctionCourante,
                (retourCourant == null) ? TypeSimple.VIDE : retourCourant
        );
    }


    private void verifierBloc(Bloc bloc) {
        ts.entrerPortee(); // portée de bloc
        for (Instruction instr : bloc.getInstructions()) {
            verifierInstruction(instr);
        }
        ts.sortirPortee();
    }

    private void verifierInstruction(Instruction i) {

        if (i instanceof Bloc b) {
            verifierBloc(b);
            return;
        }
        if (i instanceof parseur.ast.Affiche a) {
            TypeSimple t = typerExpression(a.getExpression());
            if (t == TypeSimple.VIDE) {
                throw new ErreurSemantique(
                        msg("Impossible d'afficher une expression de type VIDE")
                );
            }
            return;
        }
        if (i instanceof AppelFonctionInstr afi) {
            // On réutilise la même logique que pour un appel dans une expression :
            // - vérifie fonction connue
            // - vérifie arité
            // - type chaque argument
            // Ici, on ignore juste la valeur de retour (même si non-VIDE).
            typerExpression(afi.getAppel());
            return;
        }




        if (i instanceof Affectation a) {
            TypeSimple tExpr = typerExpression(a.getExpression());

            Symbole s = ts.resoudre(a.getNomVar());

            // ✅ Déclaration implicite à la 1ère affectation
            if (s == null) {
                ts.declarer(a.getNomVar(), tExpr, false);
                varsParFonction.get(fonctionCourante).put(a.getNomVar(), tExpr);
                return;
            }

            // ✅ Cohérence de type
            if (s.getType() != tExpr) {
                throw new ErreurSemantique(msg("Affectation incompatible : " + s.getType() + " = " + tExpr));
            }

            return;
        }

        if (i instanceof Retourne r) {
            TypeSimple t = typerExpression(r.getExpression());

            if (retourCourant == null) {
                retourCourant = t; // 1er return rencontré
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
            if (s.getSinonInstr() != null) verifierInstruction(s.getSinonInstr());
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
            // Si tu veux un 'pour' qui déclare implicitement sa variable :
            Symbole s = ts.resoudre(p.getNomVar());
            if (s == null) {
                ts.declarer(p.getNomVar(), TypeSimple.ENTIER, false);
                varsParFonction.get(fonctionCourante).put(p.getNomVar(), TypeSimple.ENTIER);
            } else if (s.getType() != TypeSimple.ENTIER) {
                throw new ErreurSemantique(msg("Variable de boucle '" + p.getNomVar() + "' doit être ENTIER."));
            }

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

    private TypeSimple typerExpression(Expression e) {

        if (e instanceof Nombre) return TypeSimple.ENTIER;
        if (e instanceof Texte) return TypeSimple.TEXTE;
        if (e instanceof Caractere) return TypeSimple.CARACTERE;
        if (e instanceof Identifiant id) {
            if ("true".equals(id.getNom()) || "false".equals(id.getNom())) {
                return TypeSimple.BOOLEEN;
            }

            Symbole s = ts.resoudre(id.getNom());

            // ❌ Lecture avant 1ère affectation
            if (s == null) {
                throw new ErreurSemantique(msg("Identifiant '" + id.getNom() + "' utilisé avant affectation."));
            }

            return s.getType();
        }
        if (e instanceof AppelFonction a) {

            // 1️⃣ Fonction connue ?
            TypeSimple typeRetour = typeRetourDe(a.getNom());
            if (typeRetour == TypeSimple.INCONNU) {
                throw new ErreurSemantique(
                        msg("Fonction inconnue : " + a.getNom())
                );
            }

            // 2️⃣ Vérification arité
            // (simple pour l’instant : uniquement nombre d’arguments)
            int ariteAttendue = nombreParamsDe(a.getNom());
            if (a.getArgs().size() != ariteAttendue) {
                throw new ErreurSemantique(
                        msg("Mauvaise arité pour '" + a.getNom() +
                                "' : attendu " + ariteAttendue +
                                ", trouvé " + a.getArgs().size())
                );
            }

            // 3️⃣ Typer chaque argument
            for (Expression arg : a.getArgs()) {
                typerExpression(arg);
            }

            // 4️⃣ Type de l’appel = type de retour de la fonction
            return typeRetour;
        }


        if (e instanceof ExpressionBinaire b) {
            TypeSimple g = typerExpression(b.getGauche());
            TypeSimple d = typerExpression(b.getDroite());
            String op = b.getop();

            // ===== CONCATÉNATION TEXTE =====
            if (op.equals("+")) {

                // TEXTE + (TEXTE | ENTIER | BOOLEEN | CARACTERE) => TEXTE
                if (g == TypeSimple.TEXTE &&
                        (d == TypeSimple.TEXTE
                                || d == TypeSimple.ENTIER
                                || d == TypeSimple.BOOLEEN
                                || d == TypeSimple.CARACTERE)) {
                    return TypeSimple.TEXTE;
                }

                // ENTIER + ENTIER => ENTIER (arithmétique)
                if (g == TypeSimple.ENTIER && d == TypeSimple.ENTIER) {
                    return TypeSimple.ENTIER;
                }

                // Sinon erreur
                throw new ErreurSemantique(
                        msg("Concaténation invalide : '" + g + " + " + d +
                                "'. TEXTE doit être à gauche.")
                );
            }

            // ===== ARITHMÉTIQUE CLASSIQUE =====
            if (op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    throw new ErreurSemantique(
                            msg("Opérateur '" + op + "' attend ENTIER,ENTIER.")
                    );
                }
                return TypeSimple.ENTIER;
            }

            // ===== COMPARAISONS =====
            if (op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                if (g != TypeSimple.ENTIER || d != TypeSimple.ENTIER) {
                    throw new ErreurSemantique(
                            msg("Comparaison '" + op + "' attend ENTIER,ENTIER.")
                    );
                }
                return TypeSimple.BOOLEEN;
            }

            // ===== ÉGALITÉ =====
            if (op.equals("==") || op.equals("!=")) {
                if (g != d) {
                    throw new ErreurSemantique(
                            msg("Test '" + op + "' attend deux opérandes du même type.")
                    );
                }
                return TypeSimple.BOOLEEN;
            }

            // ===== LOGIQUE =====
            if (op.equals("&&") || op.equals("||")) {
                if (g != TypeSimple.BOOLEEN || d != TypeSimple.BOOLEEN) {
                    throw new ErreurSemantique(
                            msg("Opérateur logique '" + op + "' attend BOOLEEN,BOOLEEN.")
                    );
                }
                return TypeSimple.BOOLEEN;
            }

            throw new ErreurSemantique(
                    msg("Opérateur binaire inconnu : " + op)
            );
        }


        throw new ErreurSemantique(msg("Expression non gérée : " + e.getClass().getSimpleName()));
    }

    private String msg(String details) {
        return "[Fonction " + fonctionCourante + "] " + details;
    }
    private int nombreParamsDe(String nomFonction) {
        if (!ariteParFonction.containsKey(nomFonction)) {
            throw new ErreurSemantique(
                    msg("Fonction inconnue : " + nomFonction)
            );
        }
        return ariteParFonction.get(nomFonction);
    }


}
