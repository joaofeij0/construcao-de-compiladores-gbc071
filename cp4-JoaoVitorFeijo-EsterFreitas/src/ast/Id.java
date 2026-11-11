package src.ast;

import src.symbols.Symbol;
import src.visitor.Visitor;

public final class Id extends Expr {
    private final String name;

    public Id(String name, int line, int column) {
        super(line, column);
        this.name = name;
    }

    public String getName() { return name; }

    private Symbol decl;

    public Symbol getDecl() { return decl; }
    public void setDecl(Symbol decl) { this.decl = decl; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitId(this); }
}
