package semantic;

public class Symbole {
    private final String nom;
    private final TypeSimple type;
    private final boolean estParametre;

    public Symbole(String nom, TypeSimple type, boolean estParametre) {
        this.nom = nom;
        this.type = type;
        this.estParametre = estParametre;
    }

    public String getNom() { return nom; }
    public TypeSimple getType() { return type; }
    public boolean estParametre() { return estParametre; }
}
