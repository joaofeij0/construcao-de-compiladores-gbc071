package src.ast;

import src.visitor.Visitor;

public final class For extends Stmt {
    private final Stmt init;
    private final Expr cond;
    private final Assign step;
    private final Block body;

    public For(Stmt init, Expr cond, Assign step, Block body, int line, int column) {
        super(line, column);
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.body = body;
    }

    public Stmt getInit() { return init; }
    public Expr getCond() { return cond; }
    public Assign getStep() { return step; }
    public Block getBody() { return body; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitFor(this); }
}
