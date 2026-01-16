package main.java.codegenerator;

public final class SourcePatcher {

    private SourcePatcher() {}

    public static String injectBeforeLastBrace(String source, String chunk) {
        if (source == null || source.isBlank()) return source;
        if (chunk == null || chunk.isBlank()) return source;

        int idx = source.lastIndexOf('}');
        if (idx < 0) {
            // Pas de classe ? on concatène
            return source + "\n" + chunk;
        }

        return source.substring(0, idx) + chunk + "\n" + source.substring(idx);
    }
}
