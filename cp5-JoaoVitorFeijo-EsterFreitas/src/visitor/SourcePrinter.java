package src.visitor;

import java.util.ArrayList;
import java.util.List;

import src.ast.*;

public final class SourcePrinter implements Visitor<String> {
    private static final String INDENT = "    ";

    private int level = 0;

    public static String print(Program program) {
        return program.accept(new SourcePrinter());
    }

    private String ind() {
        return INDENT.repeat(level);
    }

    private String header(Stmt s) {
        if (s == null) return "";
        String text = s.accept(this).strip();
        return text.endsWith(";") ? text.substring(0, text.length() - 1) : text;
    }

    private String params(List<Param> ps) {
        List<String> parts = new ArrayList<>();
        for (Param p : ps) parts.add(p.accept(this));
        return String.join(", ", parts);
    }

    private String args(List<Expr> as) {
        List<String> parts = new ArrayList<>();
        for (Expr a : as) parts.add(a.accept(this));
        return String.join(", ", parts);
    }

    @Override
    public String visitProgram(Program n) {
        List<String> parts = new ArrayList<>();
        for (Decl d : n.getDecls()) parts.add(d.accept(this));
        return parts.isEmpty() ? "" : String.join("\n\n", parts) + "\n";
    }

    @Override
    public String visitVarDecl(VarDecl n) {
        String init = n.hasInit() ? " = " + n.getInit().accept(this) : "";
        return ind() + n.getType().keyword() + " " + n.getName() + init + ";";
    }

    @Override
    public String visitProcDecl(ProcDecl n) {
        return ind() + "void " + n.getName() + "(" + params(n.getParams()) + ") "
                + n.getBody().accept(this).strip();
    }

    @Override
    public String visitFuncDecl(FuncDecl n) {
        return ind() + n.getReturnType().keyword() + " " + n.getName() + "(" + params(n.getParams()) + ") "
                + n.getBody().accept(this).strip();
    }

    @Override
    public String visitParam(Param n) {
        return n.getType().keyword() + " " + n.getName();
    }

    @Override
    public String visitBlock(Block n) {
        StringBuilder sb = new StringBuilder();
        sb.append(ind()).append("{\n");
        level++;
        for (Stmt s : n.getStmts()) sb.append(s.accept(this)).append('\n');
        level--;
        sb.append(ind()).append('}');
        return sb.toString();
    }

    @Override
    public String visitAssign(Assign n) {
        return ind() + n.getTarget() + " = " + n.getValue().accept(this) + ";";
    }

    @Override
    public String visitIf(If n) {
        String text = ind() + "if (" + n.getCond().accept(this) + ") " + n.getThen().accept(this).strip();
        if (n.hasElse()) text += " else " + n.getElse().accept(this).strip();
        return text;
    }

    @Override
    public String visitWhile(While n) {
        return ind() + "while (" + n.getCond().accept(this) + ") " + n.getBody().accept(this).strip();
    }

    @Override
    public String visitFor(For n) {
        String cond = n.getCond() == null ? "" : n.getCond().accept(this);
        return ind() + "for (" + header(n.getInit()) + "; " + cond + "; " + header(n.getStep()) + ") "
                + n.getBody().accept(this).strip();
    }

    @Override
    public String visitReturn(Return n) {
        return ind() + "return" + (n.hasValue() ? " " + n.getValue().accept(this) : "") + ";";
    }

    @Override
    public String visitCallStmt(CallStmt n) {
        return ind() + n.getCall().accept(this) + ";";
    }

    @Override
    public String visitIntLit(IntLit n) {
        return Long.toString(n.getValue());
    }

    @Override
    public String visitDoubleLit(DoubleLit n) {
        return n.getLexeme();
    }

    @Override
    public String visitBoolLit(BoolLit n) {
        return n.getValue() ? "true" : "false";
    }

    @Override
    public String visitCharLit(CharLit n) {
        return "'" + Escapes.character(n.getValue()) + "'";
    }

    @Override
    public String visitStringLit(StringLit n) {
        return "\"" + Escapes.string(n.getValue()) + "\"";
    }

    @Override
    public String visitId(Id n) {
        return n.getName();
    }

    @Override
    public String visitBin(Bin n) {
        return "(" + n.getLeft().accept(this) + " " + n.getOp().symbol() + " " + n.getRight().accept(this) + ")";
    }

    @Override
    public String visitUnary(Unary n) {
        return n.getOp().symbol() + n.getOperand().accept(this);
    }

    @Override
    public String visitCall(Call n) {
        return n.getName() + "(" + args(n.getArgs()) + ")";
    }
}
