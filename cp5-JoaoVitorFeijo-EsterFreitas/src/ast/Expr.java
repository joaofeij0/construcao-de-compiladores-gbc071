package src.ast;

import src.symbols.Type;

public abstract class Expr extends Node {
    protected Expr(int line, int column) {
        super(line, column);
    }

    private Type type;

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
}
