package src.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import src.ast.*;
import src.symbols.Kind;
import src.symbols.SemanticError;
import src.symbols.Symbol;
import src.symbols.Type;
import src.visitor.Visitor;

import static src.symbols.Type.*;

public final class TypeChecker implements Visitor<Type> {
    private final List<SemanticError> messages = new ArrayList<>();

    private String routineName = null;
    private Type routineReturn = null;
    private boolean sawReturnWithValue = false;

    private final List<Node> globalsWithoutValue = new ArrayList<>();
    private Set<Node> withoutValue = newNodeSet();

    public static List<SemanticError> check(Program tree) {
        TypeChecker c = new TypeChecker();
        c.run(tree);
        return c.getMessages();
    }

    public void run(Program tree) {
        if (tree == null) {
            throw new IllegalArgumentException(
                    "a verificação de tipos exige uma AST válida (o programa tinha erro léxico ou sintático)");
        }
        tree.accept(this);
    }

    public List<SemanticError> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public boolean hasErrors() {
        for (SemanticError m : messages) if (!m.isWarning()) return true;
        return false;
    }

    private void error(Node at, String message) {
        messages.add(new SemanticError(at.getLine(), at.getColumn(), message, messages.size()));
    }

    private void warning(Node at, String message) {
        messages.add(new SemanticError(at.getLine(), at.getColumn(), message, messages.size(), true));
    }

    private static Set<Node> newNodeSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private static Set<Node> copy(Set<Node> s) {
        Set<Node> c = newNodeSet();
        c.addAll(s);
        return c;
    }

    private static String q(String name) {
        return "'" + name + "'";
    }

    private Type expr(Expr e) {
        if (e == null) return ERRO;
        Type t = e.accept(this);
        if (t == null) t = ERRO;
        e.setType(t);
        return t;
    }

    private void stmt(Node s) {
        if (s != null) s.accept(this);
    }

    private void condition(Expr e, String command) {
        Type t = expr(e);
        if (t != BOOL && t != ERRO) {
            error(e, "condição de " + q(command) + " deve ser bool, mas é " + t);
        }
    }

    @Override
    public Type visitProgram(Program n) {
        for (Decl d : n.getDecls()) if (d != null) d.accept(this);
        return null;
    }

    @Override
    public Type visitVarDecl(VarDecl n) {
        Type declared = Type.of(n.getType());
        if (n.hasInit()) {
            Type t = expr(n.getInit());
            if (!TypeRules.mesmoTipo(t, declared)) {
                error(n, "inicialização de " + q(n.getName()) + ": esperado " + declared + ", veio " + t);
            }
        } else if (routineName == null) {
            globalsWithoutValue.add(n);
        } else {
            withoutValue.add(n);
        }
        return null;
    }

    private void routine(RoutineDecl r, Type returnType) {
        routineName = r.getName();
        routineReturn = returnType;
        sawReturnWithValue = false;
        withoutValue = newNodeSet();
        withoutValue.addAll(globalsWithoutValue);

        for (Param p : r.getParams()) stmt(p);
        Block body = r.getBody();
        if (body != null) {
            for (Stmt s : body.getStmts()) stmt(s);
        }

        routineName = null;
        routineReturn = null;
        withoutValue = newNodeSet();
    }

    @Override
    public Type visitProcDecl(ProcDecl n) {
        routine(n, VOID);
        return null;
    }

    @Override
    public Type visitFuncDecl(FuncDecl n) {
        routine(n, Type.of(n.getReturnType()));
        if (!sawReturnWithValue) {
            error(n, "função " + q(n.getName()) + " não tem return com valor");
        }
        return null;
    }

    @Override
    public Type visitParam(Param n) {
        return null;
    }

    @Override
    public Type visitBlock(Block n) {
        for (Stmt s : n.getStmts()) stmt(s);
        return null;
    }

    @Override
    public Type visitAssign(Assign n) {
        Type value = expr(n.getValue());
        Symbol target = n.getDecl();
        if (target == null) return null;
        if (target.isRoutine()) {
            error(n, "atribuição a " + q(n.getTarget()) + ", que não é variável");
            return null;
        }
        Type declared = target.typeOrError();
        if (!TypeRules.mesmoTipo(value, declared)) {
            error(n, "atribuição a " + q(n.getTarget()) + ": esperado " + declared + ", veio " + value);
        }
        if (target.decl != null) withoutValue.remove(target.decl);
        return null;
    }

    @Override
    public Type visitIf(If n) {
        condition(n.getCond(), "if");
        Set<Node> before = copy(withoutValue);
        stmt(n.getThen());
        if (n.hasElse()) {
            Set<Node> afterThen = withoutValue;
            withoutValue = copy(before);
            stmt(n.getElse());
            withoutValue.addAll(afterThen);
        } else {
            withoutValue = before;
        }
        return null;
    }

    @Override
    public Type visitWhile(While n) {
        condition(n.getCond(), "while");
        Set<Node> before = copy(withoutValue);
        stmt(n.getBody());
        withoutValue = before;
        return null;
    }

    @Override
    public Type visitFor(For n) {
        stmt(n.getInit());
        if (n.getCond() != null) condition(n.getCond(), "for");
        Set<Node> before = copy(withoutValue);
        stmt(n.getBody());
        stmt(n.getStep());
        withoutValue = before;
        return null;
    }

    @Override
    public Type visitReturn(Return n) {
        Type value = n.hasValue() ? expr(n.getValue()) : null;
        if (routineName == null) return null;
        if (routineReturn == VOID) {
            if (n.hasValue()) error(n, "return com valor no procedimento " + q(routineName));
            return null;
        }
        sawReturnWithValue = true;
        if (!n.hasValue()) {
            error(n, "return sem valor em " + q(routineName) + ", que devolve " + routineReturn);
            return null;
        }
        if (!TypeRules.mesmoTipo(value, routineReturn)) {
            error(n, "return em " + q(routineName) + ": esperado " + routineReturn + ", veio " + value);
        }
        return null;
    }

    @Override
    public Type visitCallStmt(CallStmt n) {
        Call call = n.getCall();
        if (call != null) call.setType(call(call, true));
        return null;
    }

    @Override public Type visitIntLit(IntLit n) { return INT; }
    @Override public Type visitDoubleLit(DoubleLit n) { return DOUBLE; }
    @Override public Type visitBoolLit(BoolLit n) { return BOOL; }
    @Override public Type visitCharLit(CharLit n) { return CHAR; }
    @Override public Type visitStringLit(StringLit n) { return STRING; }

    @Override
    public Type visitId(Id n) {
        Symbol s = n.getDecl();
        if (s == null) return ERRO;
        if (s.kind == Kind.FUNC) {
            error(n, q(n.getName()) + " é função; use " + n.getName() + "(...) para chamá-la");
            return ERRO;
        }
        if (s.kind == Kind.PROC) {
            error(n, q(n.getName()) + " é procedimento e não pode ser usado em expressão");
            return ERRO;
        }
        if (s.decl != null && withoutValue.contains(s.decl)) {
            warning(n, q(n.getName()) + " pode ser usado sem valor");
        }
        return s.typeOrError();
    }

    @Override
    public Type visitBin(Bin n) {
        Type l = expr(n.getLeft());
        Type r = expr(n.getRight());
        Type t = TypeRules.binario(n.getOp(), l, r);
        if (t == null) {
            error(n, "operador " + q(n.getOp().symbol()) + " não se aplica a " + l + " e " + r);
            return ERRO;
        }
        return t;
    }

    @Override
    public Type visitUnary(Unary n) {
        Type t = expr(n.getOperand());
        Type result = TypeRules.unario(n.getOp(), t);
        if (result == null) {
            error(n, "operador " + q(n.getOp().symbol()) + " não se aplica a " + t);
            return ERRO;
        }
        return result;
    }

    @Override
    public Type visitCall(Call n) {
        return call(n, false);
    }

    private Type call(Call n, boolean asCommand) {
        List<Type> args = new ArrayList<>();
        for (Expr a : n.getArgs()) args.add(expr(a));

        Symbol f = n.getDecl();
        if (f == null) return ERRO;
        if (!f.isRoutine()) {
            error(n, q(n.getName()) + " não é função nem procedimento");
            return ERRO;
        }
        if (f.kind == Kind.PROC && !asCommand) {
            error(n, q(n.getName()) + " é procedimento e não pode ser usado em expressão");
            return ERRO;
        }

        List<Type> params = f.params();
        if (args.size() != params.size()) {
            error(n, q(n.getName()) + " espera " + params.size() + " argumento(s), recebeu " + args.size());
        } else {
            for (int i = 0; i < args.size(); i++) {
                if (!TypeRules.argumentoAceito(args.get(i), params.get(i))) {
                    error(n.getArgs().get(i), "argumento " + (i + 1) + " de " + q(n.getName())
                            + ": esperado " + params.get(i) + ", veio " + args.get(i));
                }
            }
        }
        return f.typeOrError();
    }
}
