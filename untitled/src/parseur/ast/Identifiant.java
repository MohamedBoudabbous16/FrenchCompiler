package parseur.ast;

public class Identifiant extends Expression{

    private String nom;

    public Identifiant(String nom) {this.nom = nom;}

    public String getNom() {return nom;}

    @Override
    public String genJava() {
        return this.nom = nom;
    }
}
