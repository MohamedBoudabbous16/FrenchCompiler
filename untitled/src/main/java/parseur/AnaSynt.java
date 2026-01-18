package main.java.parseur;

import main.java.lexeur.Jeton;
import main.java.lexeur.Lexeur;
import main.java.lexeur.TypeJeton;
import main.java.parseur.ast.*;
import main.java.parseur.ast.controle.Pour;
import main.java.parseur.ast.controle.Si;
import main.java.parseur.ast.controle.TantQue;
import utils.diag.DiagnosticCollector;
import utils.diag.Position;

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
    private final DiagnosticCollector diags;

    private Position pos(Jeton j) {
        if (j == null) return new Position(1, 1);
        int l = Math.max(1, j.getLigne());
        int c = Math.max(1, j.getColonne());
        return new Position(l, c);
    }

    public AnaSynt(List<Jeton> jetons, DiagnosticCollector diags) {
        this.jetons = jetons;
        this.position = 0;
        this.diags = diags;
    }

    //Debug:
    public AnaSynt(Lexeur lexeur) {
        this(lexeur, new DiagnosticCollector());
    }

    public AnaSynt(Lexeur lexeur, DiagnosticCollector diags) {
        this(lexeur.analyser(), diags);
    }
//fin debug

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
                break;
            } else {
                Jeton j = courant();
                diags.erreur(
                        "Mot-clé 'fonction' attendu au début d'une définition de fonction"
                                + " (trouvé : " + j.getType() + " '" + j.getValeur() + "')",
                        new utils.diag.Position(j.getLigne(), j.getColonne())
                );

                // récup simple : on saute le jeton inattendu et on continue
                avancer();
            }
        }
        Position p0 = jetons.isEmpty() ? new Position(1,1) : pos(jetons.get(0));
        Classe classePrincipale = new Classe(
                p0,
                "ProgrammePrincipal",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                fonctions
        );

        List<Classe> classes = new ArrayList<>();
        classes.add(classePrincipale);
        return new Programme(p0, classes);
    }

    //Debug:
    public Programme parse() {
        return analyserProgramme();
    }

    public Programme analyser() {
        return analyserProgramme();
    }
    public static Programme parse(String source, DiagnosticCollector diags) {
        return analyser(source, diags);
    }

//fin debug

    /**
     * Helper pratique si tu veux parser directement
     * à partir d’une chaîne source.
     */
    public static Programme analyser(String source, DiagnosticCollector diags) {
        Lexeur lexeur = new Lexeur(source, diags);
        List<Jeton> jetons = lexeur.analyser();
        AnaSynt parser = new AnaSynt(jetons, diags); // ou AnaSynt(jetons, diags) si tu le modifies aussi
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

        return new Fonction(pos(nom), nom.getValeur(), params, corps);
    }

    private Bloc analyserBloc() {
        Jeton ouv = consommer(TypeJeton.AccoladeOuvr, "'{' attendu pour commencer un bloc");
        List<Instruction> instructions = new ArrayList<>();
        while (!est(TypeJeton.AccoFerma) && !estFin()) {
            instructions.add(analyserInstruction());
        }
        consommer(TypeJeton.AccoFerma, "'}' attendu pour terminer un bloc");
        return new Bloc(pos(ouv), instructions);
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
            // IMPORTANT : ne consomme pas ici, analyserAffiche() va consommer et garder la position
            return analyserAffiche(true);
        } else if (est(TypeJeton.AfficheSansRetourLigne)) {
            // IMPORTANT : idem
            return analyserAffiche(false);
        } else if (est(TypeJeton.Identifiant)) {

            // CAS 1 : appel de fonction comme instruction -> f(...);
            if (prochainType() == TypeJeton.ParOuvr) {
                Expression expr = analyserExpression();
                consommer(TypeJeton.PointVirgule, "';' attendu après appel de fonction");

                if (!(expr instanceof AppelFonction appel)) {
                    Jeton j = courant();
                    diags.erreur(
                            "Instruction invalide : appel de fonction attendu.",
                            pos(j) // utilise ton helper pos()
                    );
                    // instruction neutre avec position
                    return new Bloc(pos(j), List.of());
                }

                // ✅ constructeur: AppelFonctionInstr(Position, AppelFonction)
                // on prend la position de l'appel (souvent le nom de fonction)
                return new AppelFonctionInstr(appel.getPosition(), appel);
            }

            // CAS 2 : affectation -> x = expr;
            return analyserAffectation();

        } else {
            Jeton j = courant();
            diags.erreur(
                    "Instruction inattendue : " + j.getType() + " (" + j.getValeur() + ")",
                    pos(j)
            );

            avancer();
            return new Bloc(pos(j), List.of());
        }
    }


    private Instruction analyserAffiche(boolean newline) {
        // 1) consommer le mot-clé ICI pour récupérer sa position
        Jeton kw = newline
                ? consommer(TypeJeton.Affiche, "Mot-clé 'affiche' attendu")
                : consommer(TypeJeton.AfficheSansRetourLigne, "Mot-clé 'afficheSansRetourLigne' attendu");

        // 2) '('
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'affiche'");

        // 3) args
        List<Expression> args = new ArrayList<>();
        if (!est(TypeJeton.ParFerm)) {
            do {
                args.add(analyserExpression());
            } while (consommerOptionnel(TypeJeton.Virgule));
        }

        // 4) ')' + ';'
        consommer(TypeJeton.ParFerm, "')' attendu après les arguments");
        consommer(TypeJeton.PointVirgule, "';' attendu après affiche(...)");

        // 5) AST avec Position
        return new Affiche(pos(kw), args, newline);
    }



    private Instruction analyserAffectation() {
        Jeton ident = consommer(TypeJeton.Identifiant, "Nom de variable attendu");
        consommer(TypeJeton.Affecte, "'=' attendu après le nom de variable");
        Expression expr = analyserExpression();
        consommer(TypeJeton.PointVirgule, "';' attendu pour terminer l'affectation");
        return new Affectation(pos(ident),ident.getValeur(), expr);
    }

    private Instruction analyserRetourne() {
        Jeton kw = consommer(TypeJeton.Retourne, "Mot-clé 'retourne' attendu");
        Expression expr = analyserExpression();
        consommer(TypeJeton.PointVirgule, "';' attendu après l'expression de retour");
        return new Retourne(pos(kw), expr);
    }

    private Instruction analyserSi() {
        Jeton kw = consommer(TypeJeton.Si, "Mot-clé 'si' attendu");
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'si'");
        Expression condition = analyserExpression();
        consommer(TypeJeton.ParFerm, "')' attendu après la condition du 'si'");

        Instruction alorsInstr = analyserInstruction();
        Instruction sinonInstr = null;

        if (est(TypeJeton.Sinon)) {
            consommer(TypeJeton.Sinon, "Mot-clé 'sinon' attendu");
            sinonInstr = analyserInstruction();
        }

        return new Si(pos(kw) ,condition, alorsInstr, sinonInstr);
    }

    private Instruction analyserTantQue() {
        Jeton kw = consommer(TypeJeton.TantQue, "Mot-clé 'tantque' attendu");
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'tantque'");
        Expression condition = analyserExpression();
        consommer(TypeJeton.ParFerm, "')' attendu après la condition du 'tantque'");
        Instruction corps = analyserInstruction();
        return new TantQue(pos(kw), condition, corps);
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
        Jeton kw = consommer(TypeJeton.Pour, "Mot-clé 'pour' attendu");

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

        return new Pour(pos(kw), ident.getValeur(), debut, fin, operateur, pas, corps);
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
            Jeton opTok = consommer(TypeJeton.Ou, "'||' attendu"); // garde le jeton
            Expression droite = analyserEt();
            expr = new ExpressionBinaire(pos(opTok), expr, "||", droite);
        }
        return expr;
    }

    private Expression analyserEt() {
        Expression expr = analyserEgalite();
        while (est(TypeJeton.Et)) {
            Jeton opTok = consommer(TypeJeton.Et, "'&&' attendu");
            Expression droite = analyserEgalite();
            expr = new ExpressionBinaire(pos(opTok), expr, "&&", droite);
        }
        return expr;
    }

    private Expression analyserEgalite() {
        Expression expr = analyserComparaison();

        while (est(TypeJeton.Egal) || est(TypeJeton.PasEgal)) {
            Jeton opTok = courant();
            avancer();
            Expression droite = analyserComparaison();
            String opStr = (opTok.getType() == TypeJeton.Egal) ? "==" : "!=";
            expr = new ExpressionBinaire(pos(opTok), expr, opStr, droite);
        }

        return expr;
    }
    private Expression analyserAddition() {
        Expression expr = analyserMultiplication();

        while (est(TypeJeton.Plus) || est(TypeJeton.Moins)) {
            Jeton opTok = courant();
            avancer();

            Expression droite = analyserMultiplication();
            String opStr = (opTok.getType() == TypeJeton.Plus) ? "+" : "-";

            expr = new ExpressionBinaire(pos(opTok), expr, opStr, droite);
        }

        return expr;
    }

    private Expression analyserMultiplication() {
        Expression expr = analyserPrimaire();

        while (est(TypeJeton.Mult) || est(TypeJeton.Div) || est(TypeJeton.Modulo)) {
            Jeton opTok = courant();
            avancer();

            Expression droite = analyserPrimaire();

            String opStr;
            switch (opTok.getType()) {
                case Mult -> opStr = "*";
                case Div -> opStr = "/";
                case Modulo -> opStr = "%";
                default -> {
                    diags.erreur("Opérateur de multiplication inattendu : " + opTok.getType(), pos(opTok));
                    opStr = "*"; // fallback neutre
                }
            }

            expr = new ExpressionBinaire(pos(opTok), expr, opStr, droite);
        }

        return expr;
    }


    private Expression analyserComparaison() {
        Expression expr = analyserAddition();

        while (est(TypeJeton.Inf) || est(TypeJeton.InfEgal)
                || est(TypeJeton.Superieur) || est(TypeJeton.SupEgal)) {

            Jeton opTok = courant();
            avancer();
            Expression droite = analyserAddition();

            String opStr;
            switch (opTok.getType()) {
                case Inf -> opStr = "<";
                case InfEgal -> opStr = "<=";
                case Superieur -> opStr = ">";
                case SupEgal -> opStr = ">=";
                default -> {
                    // au lieu de throw, on diag + récup
                    diags.erreur(
                            "Opérateur de comparaison inattendu : " + opTok.getType(),
                            pos(opTok)
                    );
                    opStr = "<"; // fallback neutre
                }
            }

            expr = new ExpressionBinaire(pos(opTok), expr, opStr, droite);
        }

        return expr;
    }

    private Expression analyserPrimaire() {
        Jeton j = courant();

        if (est(TypeJeton.Nombre)) {
            avancer();
            int val = Integer.parseInt(j.getValeur());
            return new Nombre(pos(j), val);
        }

        if (est(TypeJeton.Lire)) {
            Jeton lireTok = consommer(TypeJeton.Lire, "Mot-clé 'lire' attendu");
            consommer(TypeJeton.ParOuvr, "'(' attendu après 'lire'");
            consommer(TypeJeton.ParFerm, "')' attendu après 'lire'");
            return new Lire(pos(lireTok));
        }

        if (est(TypeJeton.TexteLitteral)) {
            avancer();
            return new Texte(pos(j), j.getValeur());
        }

        if (est(TypeJeton.CaractereLitteral)) {
            avancer();

            if (j.getValeur() == null || j.getValeur().length() != 1) {
                diags.erreur(
                        "CaractereLitteral invalide : '" + j.getValeur() + "'",
                        pos(j)
                );
                return new Caractere(pos(j), '\0');
            }

            return new Caractere(pos(j), j.getValeur().charAt(0));
        }

        if (est(TypeJeton.Identifiant)) {
            Jeton nom = courant();
            avancer();

            // appel de fonction : f(...)
            if (est(TypeJeton.ParOuvr)) {
                consommer(TypeJeton.ParOuvr, "'(' attendu après le nom de fonction");

                List<Expression> args = new ArrayList<>();
                if (!est(TypeJeton.ParFerm)) {
                    do {
                        args.add(analyserExpression());
                    } while (consommerOptionnel(TypeJeton.Virgule));
                }

                consommer(TypeJeton.ParFerm, "')' attendu après les arguments");
                return new AppelFonction(pos(nom), nom.getValeur(), args);
            }

            // identifiant simple
            return new Identifiant(pos(nom), nom.getValeur());
        }

        if (est(TypeJeton.Vrai)) {
            avancer();
            return new Identifiant(pos(j), "true");
        }

        if (est(TypeJeton.Faux)) {
            avancer();
            return new Identifiant(pos(j), "false");
        }

        if (est(TypeJeton.ParOuvr)) {
            Jeton par = courant();
            avancer();
            Expression expr = analyserExpression();
            consommer(TypeJeton.ParFerm, "')' attendu après l'expression parenthésée");
            // Ici on renvoie expr directement (pas besoin de wrapper)
            return expr;
        }

        diags.erreur(
                "Expression primaire attendue, trouvé : " + j.getType() + " (" + j.getValeur() + ")",
                pos(j)
        );
        avancer();
        return new Nombre(pos(j), 0);
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

        Jeton j = courant();
        diags.erreur(
                messageErreur + " (trouvé : " + j.getType() + " '" + j.getValeur() + "')",
                new utils.diag.Position(j.getLigne(), j.getColonne())
        );

        // Récupération simple : on avance pour éviter de boucler, et on renvoie le jeton courant
        // (le parse continue, mais l’AST peut être bancal si trop d’erreurs).
        avancer();
        return j;
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

    /** Ajoute un diagnostic (PARxxx) avec la position du jeton. */
    private void signalerErreur(String message, Jeton j) {
        if (diags == null) return;
        int l = (j == null) ? -1 : j.getLigne();
        int c = (j == null) ? -1 : j.getColonne();
        diags.erreur("PAR001", message, new Position(l, c));
    }

    /** Récupération “panic-mode” : saute jusqu’à ';' ou '}' ou fin. */
    private void synchroniserInstruction() {
        while (!estFin() && !est(TypeJeton.PointVirgule) && !est(TypeJeton.AccoFerma)) {
            avancer();
        }
        if (est(TypeJeton.PointVirgule)) avancer(); // consommer le ';' si présent
    }
}

