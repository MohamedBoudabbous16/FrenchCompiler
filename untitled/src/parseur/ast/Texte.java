package parseur.ast;

public class Texte extends Expression {

    private final String valeur;

    public Texte(String valeur) {
        this.valeur = valeur;
    }

    public String getValeur() {
        return valeur;
    }

    @Override
    public String genJava() {
        return "\"" + valeur.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
