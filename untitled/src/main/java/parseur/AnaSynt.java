package main.java.parseur;

import main.java.lexeur.Jeton;
import main.java.lexeur.Lexeur;
import main.java.lexeur.TypeJeton;
import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;
import utils.diag.DiagnosticCollector;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyseur syntaxique (parser) très simple pour le langage maison.
 *
 * Objectif de cette première version :
 *  - prendre une liste de Jeton produite par le Lexeur ;
 *  - construire un AST à partir de cette liste ;
 *  - retourner un Programme (avec pour l’instant une seule classe "ProgrammePrincipal"
 *    qui contient toutes les fonctions trouvées).
 *
 * La grammaire supportée pour le moment est volontairement limitée :
 *
 *  programme  -> { fonction }
 *
 *  fonction   -> "fonction" IDENT "(" [ listeParams ] ")" bloc
 *  listeParams-> IDENT { "," IDENT }
 *
 *  bloc       -> "{" { instruction } "}"
 *
 *  instruction-> bloc
 *              | "si" "(" expression ")" instruction [ "sinon" instruction ]
 *              | "tantque" "(" expression ")" instruction
 *              | "pour" ...   (forme simplifiée, voir analyserPour)
 *              | "retourne" expression ";"
 *              | affectation ";"
 *
 *  affectation-> IDENT "=" expression
 *
 *  expression -> logiqueOu
 *  logiqueOu  -> logiqueEt { "||" logiqueEt }
 *  logiqueEt  -> egalite   { "&&" egalite   }
 *  egalite    -> comparaison { ("==" | "!=") comparaison }
 *  comparaison-> addition { ("<" | "<=" | ">" | ">=") addition }
 *  addition   -> multiplication { ("+" | "-") multiplication }
 *  multiplication -> primaire { ("*" | "/" | "%") primaire }
 *
 *  primaire   -> NOMBRE
 *              | IDENT
 *              | "vrai" | "faux"
 *              | "(" expression ")"
 *
 * À TOI ensuite d’étendre / modifier cette grammaire selon les besoins
 * (ajout des types, vraies classes, littéraux texte, etc.).
 */
public class AnaSynt {

    private final List<Jeton> jetons;
    private int position;

    public AnaSynt(List<Jeton> jetons) {
        this.jetons = jetons;
        this.position = 0;
    }

    /* ======================
     *  MÉTHODES PUBLIQUES
     * ====================== */

    /**
     * Point d’entrée principal du main.java.parseur.
     * On lit une suite de fonctions et on les
     * range dans une seule classe "ProgrammePrincipal".
     */
    public Programme analyserProgramme() {
        List<Fonction> fonctions = new ArrayList<>();

        while (!estFin()) {
            if (est(TypeJeton.Fonction)) {
                fonctions.add(analyserFonction());
            } else if (est(TypeJeton.FinFichier)) {
                // sécurité au cas où un jeton FinFichier est présent dans la liste
                break;
            } else {
                throw erreur("Mot-clé 'fonction' attendu au début d'une définition de fonction");
            }
        }

        // Pour l’instant : une seule classe "ProgrammePrincipal"
        Classe classePrincipale = new Classe(
                "ProgrammePrincipal",
                List.of(),      // meres
                List.of(),      // prives
                List.of(),      // publics
                List.of(),      // finaux
                fonctions
        );

        List<Classe> classes = new ArrayList<>();
        classes.add(classePrincipale);
        return new Programme(classes);
    }

    /**
     * Helper pratique si tu veux parser directement
     * à partir d’une chaîne source.
     */
    public static Programme analyser(String source, DiagnosticCollector diags) {
        Lexeur lexeur = new Lexeur(source, diags);
        List<Jeton> jetons = lexeur.analyser();
        AnaSynt parser = new AnaSynt(jetons); // ou AnaSynt(jetons, diags) si tu le modifies aussi
        return parser.analyserProgramme();
    }

    /* ======================
     *  FONCTIONS / BLOCS
     * ====================== */

    private Fonction analyserFonction() {
        consommer(TypeJeton.Fonction, "Mot-clé 'fonction' attendu");

        Jeton nom = consommer(TypeJeton.Identifiant, "Nom de fonction attendu");

        consommer(TypeJeton.ParOuvr, "'(' attendu après le nom de fonction");

        List<String> params = new ArrayList<>();
        if (!est(TypeJeton.ParFerm)) {
            do {
                Jeton param = consommer(TypeJeton.Identifiant, "Nom de paramètre attendu");
                params.add(param.getValeur());
            } while (consommerOptionnel(TypeJeton.Virgule));
        }

        consommer(TypeJeton.ParFerm, "')' attendu après la liste des paramètres");

        Bloc corps = analyserBloc();

        return new Fonction(nom.getValeur(), params, corps);
    }

    private Bloc analyserBloc() {
        consommer(TypeJeton.AccoladeOuvr, "'{' attendu pour commencer un bloc");

        List<Instruction> instructions = new ArrayList<>();
        while (!est(TypeJeton.AccoFerma) && !estFin()) {
            instructions.add(analyserInstruction());
        }

        consommer(TypeJeton.AccoFerma, "'}' attendu pour terminer un bloc");

        return new Bloc(instructions);
    }

    /* ======================
     *     INSTRUCTIONS
     * ====================== */

    private Instruction analyserInstruction() {
        if (est(TypeJeton.AccoladeOuvr)) {
            return analyserBloc();
        } else if (est(TypeJeton.Si)) {
            return analyserSi();
        } else if (est(TypeJeton.TantQue)) {
            return analyserTantQue();
        } else if (est(TypeJeton.Pour)) {
            return analyserPour();
        } else if (est(TypeJeton.Retourne)) {
            return analyserRetourne();
        } else if (est(TypeJeton.Affiche)) {
            consommer(TypeJeton.Affiche, "Mot-clé 'affiche' attendu");
            return analyserAffiche(true);
        }else if (est(TypeJeton.AfficheSansRetourLigne)) {
            consommer(TypeJeton.AfficheSansRetourLigne, "Mot-clé 'afficheSansRetourLigne' attendu");
            return analyserAffiche(false);}
        else if (est(TypeJeton.Identifiant)) {

            // ✅ CAS 1 : appel de fonction comme instruction -> f(...);
            if (prochainType() == TypeJeton.ParOuvr) {
                Expression expr = analyserExpression();
                consommer(TypeJeton.PointVirgule, "';' attendu après appel de fonction");

                if (!(expr instanceof AppelFonction appel)) {
                    throw erreur("Instruction invalide : appel de fonction attendu.");
                }

                return new AppelFonctionInstr(appel);
            }

            // ✅ CAS 2 : affectation -> x = expr;
            return analyserAffectation();

        } else {
            throw erreur("Instruction inattendue : " + courant().getType() +
                    " (" + courant().getValeur() + ")");
        }
    }

    private Instruction analyserAffiche(boolean newline) {
        // consommer le mot-clé (affiche ou afficheSansRetourLigne) avant d’arriver ici
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'affiche'");
        List<Expression> args = new ArrayList<>();
        if (!est(TypeJeton.ParFerm)) {
            do {
                args.add(analyserExpression());
            } while (consommerOptionnel(TypeJeton.Virgule));
        }
        consommer(TypeJeton.ParFerm, "')' attendu après les arguments");
        consommer(TypeJeton.PointVirgule, "';' attendu après affiche(...)");
        return new Affiche(args, newline);
    }


    private Instruction analyserAffectation() {
        Jeton ident = consommer(TypeJeton.Identifiant, "Nom de variable attendu");
        consommer(TypeJeton.Affecte, "'=' attendu après le nom de variable");
        Expression expr = analyserExpression();
        consommer(TypeJeton.PointVirgule, "';' attendu pour terminer l'affectation");
        return new Affectation(ident.getValeur(), expr);
    }

    private Instruction analyserRetourne() {
        consommer(TypeJeton.Retourne, "Mot-clé 'retourne' attendu");
        Expression expr = analyserExpression();
        consommer(TypeJeton.PointVirgule, "';' attendu après l'expression de retour");
        return new Retourne(expr);
    }

    private Instruction analyserSi() {
        consommer(TypeJeton.Si, "Mot-clé 'si' attendu");
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'si'");
        Expression condition = analyserExpression();
        consommer(TypeJeton.ParFerm, "')' attendu après la condition du 'si'");

        Instruction alorsInstr = analyserInstruction();
        Instruction sinonInstr = null;

        if (est(TypeJeton.Sinon)) {
            consommer(TypeJeton.Sinon, "Mot-clé 'sinon' attendu");
            sinonInstr = analyserInstruction();
        }

        return new Si(condition, alorsInstr, sinonInstr);
    }

    private Instruction analyserTantQue() {
        consommer(TypeJeton.TantQue, "Mot-clé 'tantque' attendu");
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'tantque'");
        Expression condition = analyserExpression();
        consommer(TypeJeton.ParFerm, "')' attendu après la condition du 'tantque'");
        Instruction corps = analyserInstruction();
        return new TantQue(condition, corps);
    }

    /**
     * Version simplifiée de :
     *   pour i = [debut ; fin], +=pas instruction
     *
     * Grammaire approximative utilisée ici :
     *
     *   pourInstr -> "pour" IDENT "=" "[" expression ";" expression "]"
     *                 "," operateurPas expression instruction
     *
     *   operateurPas -> "+=" | "-=" | "*=" | "/="
     *
     * Les opérateurs comme "+=" ne sont pas reconnus comme un seul jeton par le main.java.lexeur,
     * donc on les reconstruit à partir de deux jetons : '+' puis '='.
     */
    private Instruction analyserPour() {
        consommer(TypeJeton.Pour, "Mot-clé 'pour' attendu");

        Jeton ident = consommer(TypeJeton.Identifiant, "Nom de variable de boucle attendu");

        consommer(TypeJeton.Affecte, "'=' attendu après le nom de variable dans 'pour'");
        consommer(TypeJeton.CrochetOuvrant, "'[' attendu après '=' dans 'pour'");

        Expression debut = analyserExpression();

        consommer(TypeJeton.PointVirgule, "';' attendu pour séparer début et fin dans 'pour'");

        Expression fin = analyserExpression();

        consommer(TypeJeton.CrochetFermant, "']' attendu après l'expression de fin dans 'pour'");
        consommer(TypeJeton.Virgule, "',' attendu après les bornes dans 'pour'");

        String operateur = lireOperateurPas();

        Expression pas = analyserExpression();

        Instruction corps = analyserInstruction();

        return new Pour(ident.getValeur(), debut, fin, operateur, pas, corps);
    }

    /**
     * Combine deux jetons (par exemple '+' puis '=') en une chaîne "+=".
     */
    private String lireOperateurPas() {
        if (est(TypeJeton.Plus) && prochainType() == TypeJeton.Affecte) {
            consommer(TypeJeton.Plus, "'+' attendu dans l'opérateur de pas");
            consommer(TypeJeton.Affecte, "'=' attendu après '+' dans l'opérateur de pas");
            return "+=";
        } else if (est(TypeJeton.Moins) && prochainType() == TypeJeton.Affecte) {
            consommer(TypeJeton.Moins, "'-' attendu dans l'opérateur de pas");
            consommer(TypeJeton.Affecte, "'=' attendu après '-' dans l'opérateur de pas");
            return "-=";
        } else if (est(TypeJeton.Mult) && prochainType() == TypeJeton.Affecte) {
            consommer(TypeJeton.Mult, "'*' attendu dans l'opérateur de pas");
            consommer(TypeJeton.Affecte, "'=' attendu après '*' dans l'opérateur de pas");
            return "*=";
        } else if (est(TypeJeton.Div) && prochainType() == TypeJeton.Affecte) {
            consommer(TypeJeton.Div, "'/' attendu dans l'opérateur de pas");
            consommer(TypeJeton.Affecte, "'=' attendu après '/' dans l'opérateur de pas");
            return "/=";
        }

        throw erreur("Opérateur de pas invalide dans 'pour' (attendu +=, -=, *= ou /=)");
    }

    /* ======================
     *      EXPRESSIONS
     * ====================== */

    private Expression analyserExpression() {
        return analyserOu();
    }

    private Expression analyserOu() {
        Expression expr = analyserEt();
        while (est(TypeJeton.Ou)) {
            consommer(TypeJeton.Ou, "'||' attendu");
            Expression droite = analyserEt();
            expr = new ExpressionBinaire(expr, "||", droite);
        }
        return expr;
    }

    private Expression analyserEt() {
        Expression expr = analyserEgalite();
        while (est(TypeJeton.Et)) {
            consommer(TypeJeton.Et, "'&&' attendu");
            Expression droite = analyserEgalite();
            expr = new ExpressionBinaire(expr, "&&", droite);
        }
        return expr;
    }

    private Expression analyserEgalite() {
        Expression expr = analyserComparaison();

        while (est(TypeJeton.Egal) || est(TypeJeton.PasEgal)) {
            Jeton op = courant();
            avancer();
            Expression droite = analyserComparaison();
            String opStr = (op.getType() == TypeJeton.Egal) ? "==" : "!=";
            expr = new ExpressionBinaire(expr, opStr, droite);
        }

        return expr;
    }

    private Expression analyserComparaison() {
        Expression expr = analyserAddition();

        while (est(TypeJeton.Inf) || est(TypeJeton.InfEgal)
                || est(TypeJeton.Superieur) || est(TypeJeton.SupEgal)) {

            Jeton op = courant();
            avancer();
            Expression droite = analyserAddition();

            String opStr;
            switch (op.getType()) {
                case Inf -> opStr = "<";
                case InfEgal -> opStr = "<=";
                case Superieur -> opStr = ">";
                case SupEgal -> opStr = ">=";
                default -> throw new IllegalStateException("Opérateur de comparaison inattendu : " + op.getType());
            }

            expr = new ExpressionBinaire(expr, opStr, droite);
        }

        return expr;
    }

    private Expression analyserAddition() {
        Expression expr = analyserMultiplication();

        while (est(TypeJeton.Plus) || est(TypeJeton.Moins)) {
            Jeton op = courant();
            avancer();
            Expression droite = analyserMultiplication();
            String opStr = (op.getType() == TypeJeton.Plus) ? "+" : "-";
            expr = new ExpressionBinaire(expr, opStr, droite);
        }

        return expr;
    }

    private Expression analyserMultiplication() {
        Expression expr = analyserPrimaire();

        while (est(TypeJeton.Mult) || est(TypeJeton.Div) || est(TypeJeton.Modulo)) {
            Jeton op = courant();
            avancer();
            Expression droite = analyserPrimaire();

            String opStr;
            switch (op.getType()) {
                case Mult -> opStr = "*";
                case Div -> opStr = "/";
                case Modulo -> opStr = "%";
                default -> throw new IllegalStateException("Opérateur de multiplication inattendu : " + op.getType());
            }

            expr = new ExpressionBinaire(expr, opStr, droite);
        }

        return expr;
    }

    private Expression analyserPrimaire() {
        Jeton j = courant();

        if (est(TypeJeton.Nombre)) {
            avancer();
            int val = Integer.parseInt(j.getValeur());
            return new Nombre(val);
        }

        if (est(TypeJeton.Lire)) {
            consommer(TypeJeton.Lire, "Mot-clé 'lire' attendu");
            consommer(TypeJeton.ParOuvr, "'(' attendu après 'lire'");
            consommer(TypeJeton.ParFerm, "')' attendu après 'lire'");
            return new Lire();
        }


        // ✅ AJOUT : texte littéral
        if (est(TypeJeton.TexteLitteral)) {
            avancer();
            return new Texte(j.getValeur());
        }

        // ✅ AJOUT : caractère littéral
        if (est(TypeJeton.CaractereLitteral)) {
            avancer();
            if (j.getValeur() == null || j.getValeur().length() != 1) {
                throw erreur("CaractereLitteral invalide : '" + j.getValeur() + "'");
            }
            return new Caractere(j.getValeur().charAt(0));
        }

        if (est(TypeJeton.Identifiant)) {
            Jeton nom = courant();
            avancer();

            // 👉 APPEL DE FONCTION
            if (est(TypeJeton.ParOuvr)) {
                consommer(TypeJeton.ParOuvr, "'(' attendu après le nom de fonction");

                List<Expression> args = new ArrayList<>();

                if (!est(TypeJeton.ParFerm)) {
                    do {
                        args.add(analyserExpression());
                    } while (consommerOptionnel(TypeJeton.Virgule));
                }

                consommer(TypeJeton.ParFerm, "')' attendu après les arguments");

                return new AppelFonction(nom.getValeur(), args);
            }

            // 👉 IDENTIFIANT SIMPLE
            return new Identifiant(nom.getValeur());
        }


        if (est(TypeJeton.Vrai)) {
            avancer();
            // On génère directement "true" en Java
            return new Identifiant("true");
        }

        if (est(TypeJeton.Faux)) {
            avancer();
            // On génère directement "false" en Java
            return new Identifiant("false");
        }

        if (est(TypeJeton.ParOuvr)) {
            avancer();
            Expression expr = analyserExpression();
            consommer(TypeJeton.ParFerm, "')' attendu après l'expression parenthésée");
            return expr;
        }

        throw erreur("Expression primaire attendue, trouvé : " + j.getType() +
                " (" + j.getValeur() + ")");
    }


    /* ======================
     *   GESTION DES JETONS
     * ====================== */

    private boolean estFin() {
        return position >= jetons.size()
                || courant().getType() == TypeJeton.FinFichier;
    }

    private Jeton courant() {
        if (position >= jetons.size()) {
            // Jeton factice en fin de fichier
            if (jetons.isEmpty()) {
                return new Jeton(TypeJeton.FinFichier, "", -1, -1);
            }
            Jeton dernier = jetons.get(jetons.size() - 1);
            return new Jeton(TypeJeton.FinFichier, "", dernier.getLigne(), dernier.getColonne());
        }
        return jetons.get(position);
    }

    private TypeJeton prochainType() {
        if (position + 1 >= jetons.size()) {
            return TypeJeton.FinFichier;
        }
        return jetons.get(position + 1).getType();
    }

    private boolean est(TypeJeton type) {
        return courant().getType() == type;
    }

    private void avancer() {
        if (!estFin()) {
            position++;
        }
    }

    private Jeton consommer(TypeJeton type, String messageErreur) {
        if (est(type)) {
            Jeton j = courant();
            avancer();
            return j;
        }
        throw erreur(messageErreur + " (trouvé : " + courant().getType() +
                " '" + courant().getValeur() + "')");
    }

    /**
     * Consomme un jeton si son type correspond, sinon ne fait rien.
     * Retourne true si le jeton a été consommé.
     */
    private boolean consommerOptionnel(TypeJeton type) {
        if (est(type)) {
            avancer();
            return true;
        }
        return false;
    }

    /**
     * Construit une exception avec un message d'erreur syntaxique
     * incluant la position (ligne, colonne) du jeton courant.
     */
    private RuntimeException erreur(String message) {
        Jeton j = courant();
        String msg = "Erreur syntaxique ligne " + j.getLigne() +
                ", colonne " + j.getColonne() + " : " + message;
        return new RuntimeException(msg);
    }
}
