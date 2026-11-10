package src.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import src.visitor.Visitor;

public final class Block extends Stmt {
    private final List<Stmt> stmts;

    public Block(List<Stmt> stmts, int line, int column) {
        super(line, column);
        this.stmts = Collections.unmodifiableList(new ArrayList<>(stmts));
    }

    public List<Stmt> getStmts() { return stmts; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitBlock(this); }
}
