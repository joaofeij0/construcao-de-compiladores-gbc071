package src.visitor;

import src.ast.*;

public abstract class DepthFirstVisitor implements Visitor<Void> {
    protected void visit(Node n) {
        if (n != null) n.accept(this);
    }

    @Override
    public Void visitProgram(Program n) {
        for (Decl d : n.getDecls()) if (d != null) d.accept(this);
        return null;
    }

    @Override public Void visitVarDecl(VarDecl n) { visit(n.getInit()); return null; }

    @Override
    public Void visitProcDecl(ProcDecl n) {
        for (Param p : n.getParams()) visit(p);
        visit(n.getBody());
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl n) {
        for (Param p : n.getParams()) visit(p);
        visit(n.getBody());
        return null;
    }

    @Override public Void visitParam(Param n) { return null; }

    @Override
    public Void visitBlock(Block n) {
        for (Stmt s : n.getStmts()) visit(s);
        return null;
    }

    @Override public Void visitAssign(Assign n) { visit(n.getValue()); return null; }

    @Override
    public Void visitIf(If n) {
        visit(n.getCond());
        visit(n.getThen());
        visit(n.getElse());
        return null;
    }

    @Override
    public Void visitWhile(While n) {
        visit(n.getCond());
        visit(n.getBody());
        return null;
    }

    @Override
    public Void visitFor(For n) {
        visit(n.getInit());
        visit(n.getCond());
        visit(n.getStep());
        visit(n.getBody());
        return null;
    }

    @Override public Void visitReturn(Return n) { visit(n.getValue()); return null; }
    @Override public Void visitCallStmt(CallStmt n) { visit(n.getCall()); return null; }

    @Override public Void visitIntLit(IntLit n) { return null; }
    @Override public Void visitDoubleLit(DoubleLit n) { return null; }
    @Override public Void visitBoolLit(BoolLit n) { return null; }
    @Override public Void visitCharLit(CharLit n) { return null; }
    @Override public Void visitStringLit(StringLit n) { return null; }
    @Override public Void visitId(Id n) { return null; }

    @Override
    public Void visitBin(Bin n) {
        visit(n.getLeft());
        visit(n.getRight());
        return null;
    }

    @Override public Void visitUnary(Unary n) { visit(n.getOperand()); return null; }

    @Override
    public Void visitCall(Call n) {
        for (Expr a : n.getArgs()) visit(a);
        return null;
    }
}
