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
import main.java.parseur.ast.Expression;
import java.util.ArrayList;
import java.util.List;

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

    // Debug
    public AnaSynt(Lexeur lexeur) { this(lexeur, new DiagnosticCollector()); }
    public AnaSynt(Lexeur lexeur, DiagnosticCollector diags) { this(lexeur.analyser(), diags); }

    /* ======================
     *  PROGRAMME
     * ====================== */

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
                        "Mot-clé 'fonction' attendu (trouvé : " + j.getType() + " '" + j.getValeur() + "')",
                        pos(j)
                );
                avancer();
            }
        }

        Position p0 = jetons.isEmpty() ? new Position(1, 1) : pos(jetons.get(0));
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

    // Helpers debug
    public Programme parse() { return analyserProgramme(); }
    public Programme analyser() { return analyserProgramme(); }
    public static Programme parse(String source, DiagnosticCollector diags) { return analyser(source, diags); }

    public static Programme analyser(String source, DiagnosticCollector diags) {
        Lexeur lexeur = new Lexeur(source, diags);
        List<Jeton> jetons = lexeur.analyser();
        AnaSynt parser = new AnaSynt(jetons, diags);
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
     *  INSTRUCTIONS
     * ====================== */

    private Instruction analyserInstruction() {
        if (est(TypeJeton.AccoladeOuvr)) return analyserBloc();
        if (est(TypeJeton.Si)) return analyserSi();
        if (est(TypeJeton.TantQue)) return analyserTantQue();
        if (est(TypeJeton.Pour)) return analyserPour();
        if (est(TypeJeton.Retourne)) return analyserRetourne();
        if (est(TypeJeton.Affiche)) return analyserAffiche(true);
        if (est(TypeJeton.AfficheSansRetourLigne)) return analyserAffiche(false);

        // ✅ NOUVEAU : toute expression peut être une instruction : x++; ++x; a=b=3; f(); etc.
        if (peutCommencerExpression(courant().getType())) {
            Jeton start = courant();
            Expression e = analyserExpression();
            consommer(TypeJeton.PointVirgule, "';' attendu après l'expression");
            return new ExpressionInstr(pos(start), e);
        }

        Jeton j = courant();
        diags.erreur("Instruction inattendue : " + j.getType() + " (" + j.getValeur() + ")", pos(j));
        avancer();
        return new Bloc(pos(j), List.of());
    }

    private boolean peutCommencerExpression(TypeJeton t) {
        return t == TypeJeton.Nombre
                || t == TypeJeton.TexteLitteral
                || t == TypeJeton.CaractereLitteral
                || t == TypeJeton.Identifiant
                || t == TypeJeton.ParOuvr
                || t == TypeJeton.Lire
                || t == TypeJeton.Vrai
                || t == TypeJeton.Faux
                || t == TypeJeton.Non
                || t == TypeJeton.Plus
                || t == TypeJeton.Moins
                || t == TypeJeton.Incr
                || t == TypeJeton.Decr;
    }

    private Instruction analyserAffiche(boolean newline) {
        Jeton kw = newline
                ? consommer(TypeJeton.Affiche, "Mot-clé 'affiche' attendu")
                : consommer(TypeJeton.AfficheSansRetourLigne, "Mot-clé 'afficheSansRetourLigne' attendu");

        consommer(TypeJeton.ParOuvr, "'(' attendu après 'affiche'");

        List<Expression> args = new ArrayList<>();
        if (!est(TypeJeton.ParFerm)) {
            do {
                args.add(analyserExpression());
            } while (consommerOptionnel(TypeJeton.Virgule));
        }

        consommer(TypeJeton.ParFerm, "')' attendu après les arguments");
        consommer(TypeJeton.PointVirgule, "';' attendu après affiche(...)");

        return new Affiche(pos(kw), args, newline);
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

        return new Si(pos(kw), condition, alorsInstr, sinonInstr);
    }

    private Instruction analyserTantQue() {
        Jeton kw = consommer(TypeJeton.TantQue, "Mot-clé 'tantque' attendu");
        consommer(TypeJeton.ParOuvr, "'(' attendu après 'tantque'");
        Expression condition = analyserExpression();
        consommer(TypeJeton.ParFerm, "')' attendu après la condition du 'tantque'");
        Instruction corps = analyserInstruction();
        return new TantQue(pos(kw), condition, corps);
    }

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

    private String lireOperateurPas() {
        Jeton j = courant();
        TypeJeton t = j.getType();

        if (t == TypeJeton.PlusEgal || t == TypeJeton.MoinsEgal ||
                t == TypeJeton.MultEgal || t == TypeJeton.DivEgal ||
                t == TypeJeton.ModEgal) {
            avancer();
            return switch (t) {
                case PlusEgal -> "+=";
                case MoinsEgal -> "-=";
                case MultEgal -> "*=";
                case DivEgal -> "/=";
                case ModEgal -> "%=";
                default -> "+=";
            };
        }

        throw erreur("Opérateur de pas invalide dans 'pour' (attendu +=, -=, *=, /=, %=)");
    }

    /* ======================
     *  EXPRESSIONS (priorités + associativité)
     *  postfix : x++, x--
     *  unaires  : ++x, --x, !x, -x, +x
     *  mult     : * / %
     *  add      : + -
     *  comp     : < <= > >=
     *  eq       : == !=
     *  logique  : && ||
     *  affect   : = += -= *= /= %=   (RIGHT-ASSOC)
     * ====================== */

    private Expression analyserExpression() {
        return analyserAffectationExpr();
    }

    // affectation right-associative
    private Expression analyserAffectationExpr() {
        Expression gauche = analyserOu();

        if (estAffectOp(courant().getType())) {
            Jeton opTok = courant();
            TypeJeton opType = opTok.getType();
            avancer();

            String op = switch (opType) {
                case Affecte -> "=";
                case PlusEgal -> "+=";
                case MoinsEgal -> "-=";
                case MultEgal -> "*=";
                case DivEgal -> "/=";
                case ModEgal -> "%=";
                default -> "=";
            };

            Expression droite = analyserAffectationExpr(); // ✅ right-assoc
            return new ExpressionAffectation(pos(opTok), gauche, op, droite);
        }

        return gauche;
    }

    private Expression analyserOu() {
        Expression expr = analyserEt();
        while (est(TypeJeton.Ou)) {
            Jeton opTok = consommer(TypeJeton.Ou, "'||' attendu");
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

    private Expression analyserComparaison() {
        Expression expr = analyserAddition();

        while (est(TypeJeton.Inf) || est(TypeJeton.InfEgal)
                || est(TypeJeton.Superieur) || est(TypeJeton.SupEgal)) {

            Jeton opTok = courant();
            avancer();
            Expression droite = analyserAddition();

            String opStr = switch (opTok.getType()) {
                case Inf -> "<";
                case InfEgal -> "<=";
                case Superieur -> ">";
                case SupEgal -> ">=";
                default -> "<";
            };

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
        Expression expr = analyserUnaire();

        while (est(TypeJeton.Mult) || est(TypeJeton.Div) || est(TypeJeton.Modulo)) {
            Jeton opTok = courant();
            avancer();
            Expression droite = analyserUnaire();

            String opStr = switch (opTok.getType()) {
                case Mult -> "*";
                case Div -> "/";
                case Modulo -> "%";
                default -> "*";
            };

            expr = new ExpressionBinaire(pos(opTok), expr, opStr, droite);
        }

        return expr;
    }

    // unaires : ++x --x !x -x +x
    private Expression analyserUnaire() {
        Jeton j = courant();

        if (est(TypeJeton.Incr)) {
            avancer();
            Expression e = analyserUnaire();
            return new ExpressionUnaire(pos(j), "++", e);
        }
        if (est(TypeJeton.Decr)) {
            avancer();
            Expression e = analyserUnaire();
            return new ExpressionUnaire(pos(j), "--", e);
        }
        if (est(TypeJeton.Non)) {
            avancer();
            Expression e = analyserUnaire();
            return new ExpressionUnaire(pos(j), "!", e);
        }
        if (est(TypeJeton.Moins)) {
            avancer();
            Expression e = analyserUnaire();
            return new ExpressionUnaire(pos(j), "-", e);
        }
        if (est(TypeJeton.Plus)) {
            avancer();
            Expression e = analyserUnaire();
            return new ExpressionUnaire(pos(j), "+", e);
        }

        return analyserPostfix();
    }

    // postfix : x++ x--
    private Expression analyserPostfix() {
        Expression expr = analyserPrimaire();

        while (est(TypeJeton.Incr) || est(TypeJeton.Decr)) {
            Jeton opTok = courant();
            avancer();
            String op = (opTok.getType() == TypeJeton.Incr) ? "++" : "--";
            expr = new ExpressionPostfix(pos(opTok), expr, op);
        }

        return expr;
    }

    private Expression analyserPrimaire() {
        Jeton j = courant();

        if (est(TypeJeton.Nombre)) {
            avancer();
            return new Nombre(pos(j), Integer.parseInt(j.getValeur()));
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
                diags.erreur("CaractereLitteral invalide : '" + j.getValeur() + "'", pos(j));
                return new Caractere(pos(j), '\0');
            }
            return new Caractere(pos(j), j.getValeur().charAt(0));
        }

        if (est(TypeJeton.Identifiant)) {
            Jeton nom = courant();
            avancer();

            // appel : f(...)
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

            return new Identifiant(pos(nom), nom.getValeur());
        }

        if (est(TypeJeton.Vrai)) { avancer(); return new Identifiant(pos(j), "true"); }
        if (est(TypeJeton.Faux)) { avancer(); return new Identifiant(pos(j), "false"); }

        if (est(TypeJeton.ParOuvr)) {
            avancer();
            Expression expr = analyserExpression();
            consommer(TypeJeton.ParFerm, "')' attendu après l'expression parenthésée");
            return expr;
        }

        diags.erreur("Expression primaire attendue, trouvé : " + j.getType() + " (" + j.getValeur() + ")", pos(j));
        avancer();
        return new Nombre(pos(j), 0);
    }

    /* ======================
     *  JETONS
     * ====================== */

    private boolean estFin() {
        return position >= jetons.size() || courant().getType() == TypeJeton.FinFichier;
    }

    private Jeton courant() {
        if (position >= jetons.size()) {
            if (jetons.isEmpty()) return new Jeton(TypeJeton.FinFichier, "", -1, -1);
            Jeton dernier = jetons.get(jetons.size() - 1);
            return new Jeton(TypeJeton.FinFichier, "", dernier.getLigne(), dernier.getColonne());
        }
        return jetons.get(position);
    }

    private boolean est(TypeJeton type) { return courant().getType() == type; }

    private void avancer() { if (!estFin()) position++; }

    private Jeton consommer(TypeJeton type, String messageErreur) {
        if (est(type)) {
            Jeton j = courant();
            avancer();
            return j;
        }
        Jeton j = courant();
        diags.erreur(messageErreur + " (trouvé : " + j.getType() + " '" + j.getValeur() + "')", pos(j));
        avancer();
        return j;
    }

    private boolean consommerOptionnel(TypeJeton type) {
        if (est(type)) { avancer(); return true; }
        return false;
    }

    private RuntimeException erreur(String message) {
        Jeton j = courant();
        return new RuntimeException("Erreur syntaxique ligne " + j.getLigne()
                + ", colonne " + j.getColonne() + " : " + message);
    }

    /* ======================
     *  HELPERS OP AFFECT
     * ====================== */

    private boolean estAffectOp(TypeJeton t) {
        return t == TypeJeton.Affecte
                || t == TypeJeton.PlusEgal
                || t == TypeJeton.MoinsEgal
                || t == TypeJeton.MultEgal
                || t == TypeJeton.DivEgal
                || t == TypeJeton.ModEgal;
    }
}
