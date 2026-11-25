package src.ast;

import src.visitor.Visitor;

public final class Unary extends Expr {
    private final UnOp op;
    private final Expr operand;

    public Unary(UnOp op, Expr operand, int line, int column) {
        super(line, column);
        this.op = op;
        this.operand = operand;
    }

    public UnOp getOp() { return op; }
    public Expr getOperand() { return operand; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitUnary(this); }
}
