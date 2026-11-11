package src.ast;

import src.visitor.Visitor;

public abstract class Node {
    private final int line;
    private final int column;

    protected Node(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }

    public abstract <T> T accept(Visitor<T> v);
}
