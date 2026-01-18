package main.java.semantic;

import java.util.Objects;
import main.java.semantic.TypeSimple;

public class Symbole {
    private final String nom;
    private TypeSimple type;            // <-- plus final (pour l'inférence)
    private final boolean estParametre;

    public Symbole(String nom, TypeSimple type, boolean estParametre) {
        this.nom = Objects.requireNonNull(nom, "nom");
        this.type = Objects.requireNonNull(type, "type");
        this.estParametre = estParametre;
    }

    public String getNom() { return nom; }
    public TypeSimple getType() { return type; }
    public boolean estParametre() { return estParametre; }

    public void setType(TypeSimple type) {
        this.type = Objects.requireNonNull(type, "type");
    }
}
