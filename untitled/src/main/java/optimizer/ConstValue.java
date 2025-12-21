package main.java.optimizer;

final class ConstValue {

    enum Kind { INT, TEXT, BOOL }

    final Kind kind;
    final Integer intValue;
    final String textValue;
    final Boolean boolValue;

    private ConstValue(Kind kind, Integer i, String s, Boolean b) {
        this.kind = kind;
        this.intValue = i;
        this.textValue = s;
        this.boolValue = b;
    }

    static ConstValue integer(int v) { return new ConstValue(Kind.INT, v, null, null); }
    static ConstValue text(String v) { return new ConstValue(Kind.TEXT, null, v, null); }
    static ConstValue bool(boolean v) { return new ConstValue(Kind.BOOL, null, null, v); }
}
