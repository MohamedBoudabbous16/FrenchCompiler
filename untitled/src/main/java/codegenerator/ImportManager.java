package main.java.codegenerator;

public final class ImportManager {

    private ImportManager() {}

    public static String ensureImport(String source, String importLine) {
        if (source == null || source.isBlank()) return source;
        if (importLine == null || importLine.isBlank()) return source;

        String imp = "import " + importLine + ";";
        if (source.contains(imp)) return source;

        // Si ton code généré n’a pas de package, on met les imports tout en haut
        // Si un package existe, on met après la ligne package.
        int packageIdx = source.indexOf("package ");
        if (packageIdx >= 0) {
            int semi = source.indexOf(';', packageIdx);
            if (semi >= 0) {
                int insertPos = semi + 1;
                return source.substring(0, insertPos) + "\n\n" + imp + "\n" + source.substring(insertPos);
            }
        }

        return imp + "\n\n" + source;
    }
}
