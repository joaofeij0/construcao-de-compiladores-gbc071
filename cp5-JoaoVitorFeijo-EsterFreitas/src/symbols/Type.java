package src.symbols;

import src.ast.BasicType;

public enum Type {
    INT("int"),
    DOUBLE("double"),
    BOOL("bool"),
    CHAR("char"),
    STRING("string"),
    VOID("void"),
    ERRO("erro");

    private final String label;

    Type(String label) { this.label = label; }

    public String label() { return label; }

    public static Type of(BasicType t) {
        if (t == null) return ERRO;
        switch (t) {
            case INT:    return INT;
            case DOUBLE: return DOUBLE;
            case BOOL:   return BOOL;
            case CHAR:   return CHAR;
            default:     return STRING;
        }
    }

    @Override
    public String toString() { return label; }
}
