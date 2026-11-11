package src.ast;

import src.visitor.Visitor;

public final class BoolLit extends Expr {
    private final boolean value;

    public BoolLit(boolean value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    public boolean getValue() { return value; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitBoolLit(this); }
}
