package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class Identifiant extends Expression{

    private String nom;

    public Identifiant(String nom) {this.nom = nom;}

    public String getNom() {return nom;}

    @Override
    public String genJava(AnalyseSemantique sem) {
        return nom;
    }
}
