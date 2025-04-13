package parseur.ast;

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
    public String genJava() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Instruction instr : instructions) {
            sb.append("  ").append(instr.genJava()).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

}
