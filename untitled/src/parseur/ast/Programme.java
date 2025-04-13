package parseur.ast;

import java.util.List;

public class Programme implements Noeud {

    private final List<Classe> classes;

    public Programme(List<Classe> classes) {
        this.classes = classes;
    }

    public List<Classe> getClasses() {
        return classes;
    }

    @Override
    public String genJava() {
        StringBuilder code = new StringBuilder();

        for (Classe classe : classes) {
            code.append(classe.genJava()).append("\n\n");
        }

        return code.toString();
    }
    //une fois que le langage est stable, peut etre je vais enlever cette methode
    public void sauvegarderDansFichier(String nomFichier) {
        try (java.io.FileWriter writer = new java.io.FileWriter(nomFichier)) {
            writer.write(this.genJava());
            System.out.println("Le programme a été écrit dans " + nomFichier);
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

}
