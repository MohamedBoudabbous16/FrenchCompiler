package parseur.ast;

public class Caractere extends Expression {

    private final char valeur;

    public Caractere(char valeur) {
        this.valeur = valeur;
    }

    public char getValeur() {
        return valeur;
    }

    @Override
    public String genJava() {
        if (valeur == '\\') return "'\\\\'";
        if (valeur == '\'') return "'\\''";
        return "'" + valeur + "'";
    }
}
