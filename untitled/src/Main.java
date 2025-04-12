public class Main {
    // la c;est la methode pricipale pour fonctionner le compilateur

    public static void main(String[] args) {
        String sourceCode = "..."; // Lire le fichier source ici
        Lexer lexer = new Lexer(sourceCode);
        Parser parser = new Parser(lexer);
        ASTNode ast = parser.parse();

        SemanticAnalyzer semantic = new SemanticAnalyzer();
        semantic.analyze(ast);

        IntermediateCodeGenerator irGen = new IntermediateCodeGenerator();
        List<IRInstruction> ir = irGen.generate(ast);

        Optimizer optimizer = new Optimizer();
        List<IRInstruction> optimizedIR = optimizer.optimize(ir);

        CodeGenerator codeGen = new CodeGenerator();
        String finalCode = codeGen.generate(optimizedIR);

        System.out.println(finalCode);
    }
}
