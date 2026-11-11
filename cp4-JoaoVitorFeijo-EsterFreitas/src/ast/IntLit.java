package src.ast;

import src.visitor.Visitor;

public final class IntLit extends Expr {
    private final long value;

    public IntLit(long value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    public long getValue() { return value; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitIntLit(this); }
}
