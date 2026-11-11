package src.symbols;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class StackSymbolTable implements SymbolTable {
    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    @Override
    public void openScope() {
        scopes.push(new HashMap<>());
    }

    @Override
    public void closeScope() {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("closeScope sem escopo aberto");
        }
        scopes.pop();
    }

    @Override
    public int level() {
        return scopes.size() - 1;
    }

    @Override
    public boolean declare(Symbol s) {
        if (s == null || s.name == null) {
            throw new IllegalArgumentException("declare: símbolo ou nome nulo");
        }
        if (scopes.isEmpty()) {
            throw new IllegalStateException("declare sem escopo aberto: '" + s.name + "'");
        }

        return scopes.peek().putIfAbsent(s.name, s) == null;
    }

    @Override
    public Symbol lookupLocal(String name) {
        if (name == null || scopes.isEmpty()) return null;
        return scopes.peek().get(name);
    }

    @Override
    public Symbol lookup(String name) {
        if (name == null) return null;
        for (Map<String, Symbol> m : scopes) {
            Symbol s = m.get(name);
            if (s != null) return s;
        }
        return null;
    }
}
