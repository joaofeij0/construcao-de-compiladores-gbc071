package src.ast;

import src.visitor.Visitor;

public final class DoubleLit extends Expr {
    private final double value;
    private final String lexeme;

    public DoubleLit(double value, String lexeme, int line, int column) {
        super(line, column);
        this.value = value;
        this.lexeme = lexeme;
    }

    public double getValue() { return value; }
    public String getLexeme() { return lexeme; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitDoubleLit(this); }
}
