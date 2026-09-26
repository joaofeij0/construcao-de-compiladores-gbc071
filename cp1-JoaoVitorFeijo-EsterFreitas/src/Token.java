package src;

import java.util.Objects;

public class Token {
    private final TokenType type;
    private final String lexeme;
    private final int line;
    private final int column;
    private final Object literal; // Valor tipado opcional (Integer, Double, String, etc.)

    public Token(TokenType type, String lexeme, int line, int column) {
        this(type, lexeme, line, column, null);
    }

    public Token(TokenType type, String lexeme, int line, int column, Object literal) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.literal = literal;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Object getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        if (literal != null) {
            return String.format("<%s, '%s' (valor=%s), %d:%d>", type, lexeme, literal, line, column);
        }
        return String.format("<%s, '%s', %d:%d>", type, lexeme, line, column);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Token token = (Token) o;
        return line == token.line && column == token.column &&
               type == token.type && Objects.equals(lexeme, token.lexeme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, line, column);
    }
}
