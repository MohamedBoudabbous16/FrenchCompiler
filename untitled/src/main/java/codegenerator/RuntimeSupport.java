package main.java.codegenerator;

public final class RuntimeSupport {

    private RuntimeSupport() {}

    public static boolean sourceUsesLire(String source) {
        if (source == null) return false;
        // simple mais efficace : détecte un appel "lire(" avec éventuellement des espaces
        return source.matches("(?s).*\\blire\\s*\\(.*");
    }

    public static String lireIntRuntimeChunk(String scannerFieldName,
                                             String scannerInitExpr,
                                             String lireMethodName) {

        // On génère un champ + une méthode dans la classe Java générée
        // Note : si tu veux éviter les warnings, tu peux ajouter @SuppressWarnings.
        return
                "\n  // ===== Runtime built-in =====\n" +
                        "  private static final java.util.Scanner " + scannerFieldName + " = " + scannerInitExpr + ";\n\n" +
                        "  /** Built-in: lire() -> ENTIER */\n" +
                        "  public static int " + lireMethodName + "() {\n" +
                        "    return " + scannerFieldName + ".nextInt();\n" +
                        "  }\n";
    }
}
