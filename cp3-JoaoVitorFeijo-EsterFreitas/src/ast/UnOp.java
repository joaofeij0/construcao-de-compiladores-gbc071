package src.ast;

public enum UnOp {
    NOT("!"),
    NEG("-");

    private final String symbol;

    UnOp(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() { return symbol; }
}
