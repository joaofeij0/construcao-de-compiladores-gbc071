package src.ast;

import src.symbols.Symbol;
import src.visitor.Visitor;

public final class Assign extends Stmt {
    private final String target;
    private final Expr value;

    public Assign(String target, Expr value, int line, int column) {
        super(line, column);
        this.target = target;
        this.value = value;
    }

    public String getTarget() { return target; }
    public Expr getValue() { return value; }

    private Symbol decl;

    public Symbol getDecl() { return decl; }
    public void setDecl(Symbol decl) { this.decl = decl; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitAssign(this); }
}
