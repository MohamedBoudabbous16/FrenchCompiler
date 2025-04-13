package parseur.ast;

public class Nombre extends Expression  {

    private final int valeur;

    public Nombre (int valeur){
        this.valeur = valeur;
    }

    public int getValeur() {
        return valeur;
    }

    @Override
    public String genJava() {
        //pour le moment j'utilise toString definis par java pas la mienne
        return Integer.toString(valeur);
    }

}
