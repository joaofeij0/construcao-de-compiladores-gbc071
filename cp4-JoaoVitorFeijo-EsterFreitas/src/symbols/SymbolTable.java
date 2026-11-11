package src.symbols;

public interface SymbolTable {
    void openScope();
    void closeScope();
    boolean declare(Symbol s);
    Symbol lookupLocal(String name);
    Symbol lookup(String name);
    int level();
}
