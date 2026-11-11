package src.visitor;

import src.ast.*;

public final class AstPrinter implements Visitor<Void> {
    private static final String INDENT = "  ";

    private final boolean withPositions;
    private final StringBuilder out = new StringBuilder();
    private int depth = 0;
    private String label = "";

    public AstPrinter() {
        this(false);
    }

    public AstPrinter(boolean withPositions) {
        this.withPositions = withPositions;
    }

    public static String print(Node root) {
        return print(root, false);
    }

    public static String print(Node root, boolean withPositions) {
        AstPrinter p = new AstPrinter(withPositions);
        root.accept(p);
        return p.out.toString();
    }

    private void line(String text, Node n) {
        for (int i = 0; i < depth; i++) out.append(INDENT);
        out.append(label).append(text);
        if (withPositions) out.append(" @").append(n.getLine()).append(':').append(n.getColumn());
        out.append('\n');
        label = "";
    }

    private void child(Node n) {
        depth++;
        n.accept(this);
        depth--;
    }

    private void child(String lbl, Node n) {
        label = lbl;
        child(n);
    }

    @Override
    public Void visitProgram(Program n) {
        line("Program", n);
        for (Decl d : n.getDecls()) {
            depth++;
            d.accept(this);
            depth--;
        }
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl n) {
        line("VarDecl(" + n.getType().keyword() + " " + n.getName() + ")", n);
        if (n.hasInit()) child(n.getInit());
        return null;
    }

    @Override
    public Void visitProcDecl(ProcDecl n) {
        line("ProcDecl(" + n.getName() + ")", n);
        for (Param p : n.getParams()) child(p);
        child(n.getBody());
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl n) {
        line("FuncDecl(" + n.getReturnType().keyword() + " " + n.getName() + ")", n);
        for (Param p : n.getParams()) child(p);
        child(n.getBody());
        return null;
    }

    @Override
    public Void visitParam(Param n) {
        line("Param(" + n.getType().keyword() + " " + n.getName() + ")", n);
        return null;
    }

    @Override
    public Void visitBlock(Block n) {
        line("Block", n);
        for (Stmt s : n.getStmts()) child(s);
        return null;
    }

    @Override
    public Void visitAssign(Assign n) {
        line("Assign(" + n.getTarget() + ")", n);
        child(n.getValue());
        return null;
    }

    @Override
    public Void visitIf(If n) {
        line("If", n);
        child(n.getCond());
        child(n.getThen());
        if (n.hasElse()) child(n.getElse());
        return null;
    }

    @Override
    public Void visitWhile(While n) {
        line("While", n);
        child(n.getCond());
        child(n.getBody());
        return null;
    }

    @Override
    public Void visitFor(For n) {
        line("For", n);
        if (n.getInit() != null) child("init: ", n.getInit());
        if (n.getCond() != null) child("cond: ", n.getCond());
        if (n.getStep() != null) child("step: ", n.getStep());
        child(n.getBody());
        return null;
    }

    @Override
    public Void visitReturn(Return n) {
        line("Return", n);
        if (n.hasValue()) child(n.getValue());
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt n) {
        line("CallStmt", n);
        child(n.getCall());
        return null;
    }

    @Override
    public Void visitIntLit(IntLit n) {
        line("Int(" + n.getValue() + ")", n);
        return null;
    }

    @Override
    public Void visitDoubleLit(DoubleLit n) {
        line("Double(" + n.getLexeme() + ")", n);
        return null;
    }

    @Override
    public Void visitBoolLit(BoolLit n) {
        line("Bool(" + n.getValue() + ")", n);
        return null;
    }

    @Override
    public Void visitCharLit(CharLit n) {
        line("Char('" + Escapes.character(n.getValue()) + "')", n);
        return null;
    }

    @Override
    public Void visitStringLit(StringLit n) {
        line("Str(\"" + Escapes.string(n.getValue()) + "\")", n);
        return null;
    }

    @Override
    public Void visitId(Id n) {
        line("Id(" + n.getName() + ")", n);
        return null;
    }

    @Override
    public Void visitBin(Bin n) {
        line("Bin(" + n.getOp().symbol() + ")", n);
        child(n.getLeft());
        child(n.getRight());
        return null;
    }

    @Override
    public Void visitUnary(Unary n) {
        line("Unary(" + n.getOp().symbol() + ")", n);
        child(n.getOperand());
        return null;
    }

    @Override
    public Void visitCall(Call n) {
        line("Call(" + n.getName() + ")", n);
        for (Expr a : n.getArgs()) child(a);
        return null;
    }
}
