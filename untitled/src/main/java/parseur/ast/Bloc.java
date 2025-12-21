package main.java.parseur.ast;

import main.java.semantic.AnalyseSemantique;

import java.util.List;

public class Bloc extends Instruction{
    private final List<Instruction> instructions;

    public Bloc(List<Instruction> instructions) {
        this.instructions = instructions;
    }
    public List<Instruction> getInstructions() {
        return instructions;
    }
    @Override
    public String genJava(AnalyseSemantique sem) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Instruction instr : instructions) {
            sb.append("  ").append(instr.genJava(sem)).append("\n"); // ✅ sem
        }
        sb.append("}");
        return sb.toString();
    }

}
