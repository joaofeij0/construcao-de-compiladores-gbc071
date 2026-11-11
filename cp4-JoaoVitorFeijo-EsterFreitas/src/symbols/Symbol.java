package src.symbols;

import src.ast.Node;

public final class Symbol {
    public static final int NO_OFFSET = -1;

    public final String name;
    public final Kind kind;
    public final Node decl;
    public final int level;

    public Type type = null;
    public int offset = NO_OFFSET;

    public Symbol(String name, Kind kind, Node decl, int level) {
        this.name = name;
        this.kind = kind;
        this.decl = decl;
        this.level = level;
    }

    public int line() { return decl == null ? 0 : decl.getLine(); }

    public String label() { return name + "@" + line(); }

    @Override
    public String toString() {
        return label() + " (" + kind.label() + ", nível " + level + ")";
    }
}
