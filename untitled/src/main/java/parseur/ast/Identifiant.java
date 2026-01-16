package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class Identifiant extends Expression{

    private String nom;

    public Identifiant(Position pos, String nom) {
        super(pos);
        this.nom = nom;}

    public String getNom() {return nom;}

    @Override
    public String genJava(AnalyseSemantique sem) {
        return nom;
    }
}
