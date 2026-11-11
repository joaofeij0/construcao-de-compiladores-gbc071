package src.ast;

public abstract class Expr extends Node {
    protected Expr(int line, int column) {
        super(line, column);
    }
}
