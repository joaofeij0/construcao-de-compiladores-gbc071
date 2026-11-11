package src.ast;

import src.visitor.Visitor;

public final class Return extends Stmt {
    private final Expr value;

    public Return(Expr value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    public Expr getValue() { return value; }
    public boolean hasValue() { return value != null; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitReturn(this); }
}
