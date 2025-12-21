package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

public class AppelFonctionInstr extends Instruction {

    private final AppelFonction appel;

    public AppelFonctionInstr(AppelFonction appel) {
        this.appel = appel;
    }

    public AppelFonction getAppel() {
        return appel;
    }

    @Override
    public String genJava(AnalyseSemantique sem) {
        return appel.genJava(sem) + ";";
    }

}
