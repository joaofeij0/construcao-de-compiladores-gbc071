package src.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import src.visitor.Visitor;

public final class Program extends Node {
    private final List<Decl> decls;

    public Program(List<Decl> decls, int line, int column) {
        super(line, column);
        this.decls = Collections.unmodifiableList(new ArrayList<>(decls));
    }

    public List<Decl> getDecls() { return decls; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitProgram(this); }
}
