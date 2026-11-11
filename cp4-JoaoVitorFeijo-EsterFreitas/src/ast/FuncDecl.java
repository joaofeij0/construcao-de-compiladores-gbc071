package src.ast;

import java.util.List;

import src.visitor.Visitor;

public final class FuncDecl extends RoutineDecl {
    private final BasicType returnType;

    public FuncDecl(BasicType returnType, String name, List<Param> params, Block body, int line, int column) {
        super(name, params, body, line, column);
        this.returnType = returnType;
    }

    public BasicType getReturnType() { return returnType; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitFuncDecl(this); }
}
