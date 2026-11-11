package src.ast;

import src.visitor.Visitor;

public final class CallStmt extends Stmt {
    private final Call call;

    public CallStmt(Call call, int line, int column) {
        super(line, column);
        this.call = call;
    }

    public Call getCall() { return call; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitCallStmt(this); }
}
