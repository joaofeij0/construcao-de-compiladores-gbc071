package src.ast;

import src.visitor.Visitor;

public final class Bin extends Expr {
    private final BinOp op;
    private final Expr left;
    private final Expr right;

    public Bin(BinOp op, Expr left, Expr right, int line, int column) {
        super(line, column);
        this.op = op;
        this.left = left;
        this.right = right;
    }

    public BinOp getOp() { return op; }
    public Expr getLeft() { return left; }
    public Expr getRight() { return right; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitBin(this); }
}
