package src.ast;

import src.visitor.Visitor;

public final class StringLit extends Expr {
    private final String value;

    public StringLit(String value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    public String getValue() { return value; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitStringLit(this); }
}
