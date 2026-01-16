package java.parseur.ast;

import java.semantic.AnalyseSemantique;
import utils.diag.Position;

public class AppelFonctionInstr extends Instruction {

    private final AppelFonction appel;

    public AppelFonctionInstr(Position pos,AppelFonction appel) {
        super(pos);
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
