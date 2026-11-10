package src;

/** Erro sintático registrado pelo Parser (análogo ao LexicalError do CP1). */
public class SyntaxError {
    private final String message;
    private final int line;
    private final int column;

    public SyntaxError(String message, int line, int column) {
        this.message = message;
        this.line = line;
        this.column = column;
    }

    public String getMessage() { return message; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    @Override
    public String toString() {
        return String.format("[Erro Sintático] Linha %d, Coluna %d: %s", line, column, message);
    }
}
