package src.ast;

import src.visitor.Visitor;

public final class While extends Stmt {
    private final Expr cond;
    private final Block body;

    public While(Expr cond, Block body, int line, int column) {
        super(line, column);
        this.cond = cond;
        this.body = body;
    }

    public Expr getCond() { return cond; }
    public Block getBody() { return body; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitWhile(this); }
}
