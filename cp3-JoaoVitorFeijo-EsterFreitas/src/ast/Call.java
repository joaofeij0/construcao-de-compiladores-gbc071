package src.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import src.visitor.Visitor;

public final class Call extends Expr {
    private final String name;
    private final List<Expr> args;

    public Call(String name, List<Expr> args, int line, int column) {
        super(line, column);
        this.name = name;
        this.args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    public String getName() { return name; }
    public List<Expr> getArgs() { return args; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitCall(this); }
}
