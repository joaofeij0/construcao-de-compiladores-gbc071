package src.ast;

import src.visitor.Visitor;

public final class If extends Stmt {
    private final Expr cond;
    private final Block thenBranch;
    private final Stmt elseBranch;

    public If(Expr cond, Block thenBranch, Stmt elseBranch, int line, int column) {
        super(line, column);
        this.cond = cond;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Expr getCond() { return cond; }
    public Block getThen() { return thenBranch; }
    public Stmt getElse() { return elseBranch; }
    public boolean hasElse() { return elseBranch != null; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitIf(this); }
}
