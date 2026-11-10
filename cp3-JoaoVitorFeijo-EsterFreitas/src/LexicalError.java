package src;

public class LexicalError {
    private final String message;
    private final String offendingText;
    private final int line;
    private final int column;

    public LexicalError(String message, String offendingText, int line, int column) {
        this.message = message;
        this.offendingText = offendingText;
        this.line = line;
        this.column = column;
    }

    public String getMessage() {
        return message;
    }

    public String getOffendingText() {
        return offendingText;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return String.format("[Erro Léxico] Linha %d, Coluna %d: %s (trecho: '%s')",
                line, column, message, offendingText);
    }
}
