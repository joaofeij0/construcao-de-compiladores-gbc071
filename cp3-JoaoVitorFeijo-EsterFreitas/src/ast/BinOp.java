package src.ast;

public enum BinOp {
    OR("||"),
    AND("&&"),
    EQ("=="),
    NE("!="),
    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    ADD("+"),
    SUB("-"),
    MUL("*"),
    DIV("/"),
    MOD("%");

    private final String symbol;

    BinOp(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() { return symbol; }
}
