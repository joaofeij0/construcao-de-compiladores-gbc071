package src.ast;

import src.visitor.Visitor;

public final class Param extends Node {
    private final BasicType type;
    private final String name;

    public Param(BasicType type, String name, int line, int column) {
        super(line, column);
        this.type = type;
        this.name = name;
    }

    public BasicType getType() { return type; }
    public String getName() { return name; }

    @Override
    public <T> T accept(Visitor<T> v) { return v.visitParam(this); }
}
