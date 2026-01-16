package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

import java.util.List;

public class Programme extends NoeudAst {

    private final List<Classe> classes;

    public Programme(Position pos, List<Classe> classes) {
        super(pos);
        this.classes = classes;
    }

    public List<Classe> getClasses() {
        return classes;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder code = new StringBuilder();
        for (Classe classe : classes) {
            code.append(classe.genJava(sem)).append("\n\n");
        }
        return code.toString();
    }
//une fois que le langage est stable, peut etre je vais enlever cette methode
    public void sauvegarderDansFichier(String nomFichier, AnalyseSemantique sem) {
        try (java.io.FileWriter writer = new java.io.FileWriter(nomFichier)) {
            writer.write(this.genJava(sem));
            System.out.println("Le programme a été écrit dans " + nomFichier);
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }
}

