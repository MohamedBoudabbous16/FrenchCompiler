package java.codegenerator;

public class CodeWriter {
    private final StringBuilder sb = new StringBuilder();
    private int indent = 0;
    private final String unit = "  ";

    public CodeWriter indent() { indent++; return this; }
    public CodeWriter dedent() { indent = Math.max(0, indent - 1); return this; }

    public CodeWriter line(String s) {
        for (int i = 0; i < indent; i++) sb.append(unit);
        sb.append(s).append("\n");
        return this;
    }

    public CodeWriter raw(String s) {
        sb.append(s);
        return this;
    }

    public String build() {
        return sb.toString();
    }
}
