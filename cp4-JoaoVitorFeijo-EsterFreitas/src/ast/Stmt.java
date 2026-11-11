package src.ast;

public abstract class Stmt extends Node {
    protected Stmt(int line, int column) {
        super(line, column);
    }
}
