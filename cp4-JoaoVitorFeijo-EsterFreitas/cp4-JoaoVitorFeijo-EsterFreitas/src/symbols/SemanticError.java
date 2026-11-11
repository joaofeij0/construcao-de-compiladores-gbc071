package src.symbols;

public final class SemanticError implements Comparable<SemanticError> {
    private final int line;
    private final int column;
    private final String message;
    private final int sequence;

    public SemanticError(int line, int column, String message, int sequence) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.sequence = sequence;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }
    public String getMessage() { return message; }

    @Override
    public int compareTo(SemanticError o) {
        if (line != o.line) return Integer.compare(line, o.line);
        if (column != o.column) return Integer.compare(column, o.column);
        return Integer.compare(sequence, o.sequence);
    }

    @Override
    public String toString() {
        return "linha " + line + ": " + message;
    }
}
