package src.ast;

import src.visitor.Visitor;

public final class CharLit extends Expr {
    private final char value;

    public CharLit(char value, int line, int column) {
        super(line, column);
        this.value = value;
    }

    public char getValue() { return value; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitCharLit(this); }
}
