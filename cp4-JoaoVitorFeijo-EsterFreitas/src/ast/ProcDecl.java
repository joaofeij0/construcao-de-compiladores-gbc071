package src.ast;

import java.util.List;

import src.visitor.Visitor;

public final class ProcDecl extends RoutineDecl {
    public ProcDecl(String name, List<Param> params, Block body, int line, int column) {
        super(name, params, body, line, column);
    }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitProcDecl(this); }
}
