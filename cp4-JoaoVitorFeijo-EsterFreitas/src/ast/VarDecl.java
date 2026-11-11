package src.ast;

import src.visitor.Visitor;

public final class VarDecl extends Stmt implements Decl {
    private final BasicType type;
    private final String name;
    private final Expr init;

    public VarDecl(BasicType type, String name, Expr init, int line, int column) {
        super(line, column);
        this.type = type;
        this.name = name;
        this.init = init;
    }

    public BasicType getType() { return type; }
    public String getName() { return name; }
    public Expr getInit() { return init; }
    public boolean hasInit() { return init != null; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitVarDecl(this); }
}
