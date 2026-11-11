package src.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import src.ast.*;
import src.symbols.Kind;
import src.symbols.SemanticError;
import src.symbols.StackSymbolTable;
import src.symbols.Symbol;
import src.symbols.SymbolTable;

public final class IdentificationVisitor implements Visitor<Void> {
    private final SymbolTable table;
    private final List<SemanticError> errors = new ArrayList<>();

    public IdentificationVisitor() {
        this(new StackSymbolTable());
    }

    public IdentificationVisitor(SymbolTable table) {
        if (table == null) throw new IllegalArgumentException("tabela de símbolos nula");
        this.table = table;
    }

    public static List<SemanticError> analyze(Program tree) {
        IdentificationVisitor v = new IdentificationVisitor();
        v.run(tree);
        return v.getErrors();
    }

    public void run(Program tree) {
        if (tree == null) {
            throw new IllegalArgumentException(
                    "a passada de identificação exige uma AST válida (o programa tinha erro léxico ou sintático)");
        }
        tree.accept(this);
    }

    public List<SemanticError> getErrors() {
        List<SemanticError> sorted = new ArrayList<>(errors);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    public List<String> getMessages() {
        List<String> out = new ArrayList<>();
        for (SemanticError e : getErrors()) out.add(e.toString());
        return out;
    }

    public boolean hasErrors() { return !errors.isEmpty(); }

    private void visit(Node n) {
        if (n != null) n.accept(this);
    }

    private void error(Node at, String message) {
        errors.add(new SemanticError(at.getLine(), at.getColumn(), message, errors.size()));
    }

    private void declare(String name, Kind kind, Node declNode) {
        if (name == null) return;
        Symbol s = new Symbol(name, kind, declNode, table.level());
        if (!table.declare(s)) {
            Symbol prev = table.lookupLocal(name);
            String where = prev == null ? "?" : String.valueOf(prev.line());
            error(declNode, "'" + name + "' já declarado neste escopo (linha " + where + ")");
        }
    }

    private Symbol resolve(String name, Node useNode) {
        Symbol s = table.lookup(name);
        if (s == null) error(useNode, "'" + name + "' não declarado");
        return s;
    }

    private final class RoutineHeaders extends DepthFirstVisitor {
        @Override public Void visitVarDecl(VarDecl n) { return null; }
        @Override public Void visitProcDecl(ProcDecl n) { declare(n.getName(), Kind.PROC, n); return null; }
        @Override public Void visitFuncDecl(FuncDecl n) { declare(n.getName(), Kind.FUNC, n); return null; }
    }

    private void visitRoutine(RoutineDecl r) {
        table.openScope();
        for (Param p : r.getParams()) visit(p);
        Block body = r.getBody();
        if (body != null) {
            for (Stmt s : body.getStmts()) visit(s);
        }
        table.closeScope();
    }

    @Override
    public Void visitProgram(Program n) {
        table.openScope();
        RoutineHeaders headers = new RoutineHeaders();
        for (Decl d : n.getDecls()) if (d != null) d.accept(headers);
        for (Decl d : n.getDecls()) if (d != null) d.accept(this);
        table.closeScope();
        return null;
    }

    @Override
    public Void visitVarDecl(VarDecl n) {
        visit(n.getInit());
        declare(n.getName(), Kind.VAR, n);
        return null;
    }

    @Override
    public Void visitProcDecl(ProcDecl n) {
        visitRoutine(n);
        return null;
    }

    @Override
    public Void visitFuncDecl(FuncDecl n) {
        visitRoutine(n);
        return null;
    }

    @Override
    public Void visitParam(Param n) {
        declare(n.getName(), Kind.PARAM, n);
        return null;
    }

    @Override
    public Void visitBlock(Block n) {
        table.openScope();
        for (Stmt s : n.getStmts()) visit(s);
        table.closeScope();
        return null;
    }

    @Override
    public Void visitAssign(Assign n) {
        n.setDecl(resolve(n.getTarget(), n));
        visit(n.getValue());
        return null;
    }

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
        table.openScope();
        visit(n.getInit());
        visit(n.getCond());
        visit(n.getStep());
        visit(n.getBody());
        table.closeScope();
        return null;
    }

    @Override
    public Void visitReturn(Return n) {
        visit(n.getValue());
        return null;
    }

    @Override
    public Void visitCallStmt(CallStmt n) {
        visit(n.getCall());
        return null;
    }

    @Override public Void visitIntLit(IntLit n) { return null; }
    @Override public Void visitDoubleLit(DoubleLit n) { return null; }
    @Override public Void visitBoolLit(BoolLit n) { return null; }
    @Override public Void visitCharLit(CharLit n) { return null; }
    @Override public Void visitStringLit(StringLit n) { return null; }

    @Override
    public Void visitId(Id n) {
        n.setDecl(resolve(n.getName(), n));
        return null;
    }

    @Override
    public Void visitBin(Bin n) {
        visit(n.getLeft());
        visit(n.getRight());
        return null;
    }

    @Override
    public Void visitUnary(Unary n) {
        visit(n.getOperand());
        return null;
    }

    @Override
    public Void visitCall(Call n) {
        n.setDecl(resolve(n.getName(), n));
        for (Expr a : n.getArgs()) visit(a);
        return null;
    }
}
