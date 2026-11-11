package src.symbols;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class TracingSymbolTable implements SymbolTable {
    private final SymbolTable inner;
    private final Deque<List<Symbol>> mirror = new ArrayDeque<>();
    private final StringBuilder log = new StringBuilder();

    public TracingSymbolTable(SymbolTable inner) {
        this.inner = inner;
    }

    @Override
    public void openScope() {
        inner.openScope();
        mirror.push(new ArrayList<>());
        line("abre nível " + inner.level());
    }

    @Override
    public void closeScope() {
        int closing = inner.level();
        inner.closeScope();
        mirror.pop();
        line("fecha nível " + closing);
    }

    @Override
    public boolean declare(Symbol s) {
        boolean ok = inner.declare(s);
        if (ok) {
            mirror.peek().add(s);
            line("declara " + s.kind.label() + " " + s.label());
        } else {
            Symbol prev = inner.lookupLocal(s.name);
            line("recusa " + s.kind.label() + " " + s.label()
                    + " (já existe " + (prev == null ? "?" : prev.label()) + ")");
        }
        return ok;
    }

    @Override
    public Symbol lookupLocal(String name) { return inner.lookupLocal(name); }

    @Override
    public Symbol lookup(String name) { return inner.lookup(name); }

    @Override
    public int level() { return inner.level(); }

    public String getLog() { return log.toString(); }

    public String environment() {
        if (mirror.isEmpty()) return "(vazio)";
        StringBuilder sb = new StringBuilder();
        boolean firstScope = true;
        for (List<Symbol> scope : mirror) {
            if (!firstScope) sb.append(" :: ");
            firstScope = false;
            if (scope.isEmpty()) {
                sb.append("{ }");
                continue;
            }
            sb.append("{ ");
            for (int i = 0; i < scope.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(scope.get(i).label());
            }
            sb.append(" }");
        }
        return sb.toString();
    }

    private void line(String action) {
        log.append(action).append(" => ").append(environment()).append('\n');
    }
}
