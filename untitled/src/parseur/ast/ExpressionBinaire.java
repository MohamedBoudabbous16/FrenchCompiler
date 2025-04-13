package parseur.ast;

public class ExpressionBinaire extends Expression {
    private final Expression gauche;
    private final String op;
    private final Expression droite;
    public ExpressionBinaire(Expression gauche, String op, Expression droite ){
        this.gauche = gauche;
        this.op = op;
        this.droite = droite;
    }
    public Expression getGauche() {
        return gauche;
    }

    public String getop() {
        return op;
    }

    public Expression getDroite() {
        return droite;
    }

    @Override
    public String genJava() {
        return "(" + gauche.genJava() + " " + op + " " + droite.genJava() + ")";
    }
}
