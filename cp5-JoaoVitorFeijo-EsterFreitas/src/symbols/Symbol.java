package src.symbols;

import java.util.Collections;
import java.util.List;

import src.ast.Node;

public final class Symbol {
    public static final int NO_OFFSET = -1;

    public final String name;
    public final Kind kind;
    public final Node decl;
    public final int level;

    public Type type = null;
    public List<Type> paramTypes = Collections.emptyList();
    public int offset = NO_OFFSET;

    public Symbol(String name, Kind kind, Node decl, int level) {
        this.name = name;
        this.kind = kind;
        this.decl = decl;
        this.level = level;
    }

    public int line() { return decl == null ? 0 : decl.getLine(); }

    public String label() { return name + "@" + line(); }

    public boolean isRoutine() { return kind == Kind.PROC || kind == Kind.FUNC; }

    public Type typeOrError() { return type == null ? Type.ERRO : type; }

    public List<Type> params() { return paramTypes == null ? Collections.emptyList() : paramTypes; }

    @Override
    public String toString() {
        return label() + " (" + kind.label() + ", nível " + level + ")";
    }
}
