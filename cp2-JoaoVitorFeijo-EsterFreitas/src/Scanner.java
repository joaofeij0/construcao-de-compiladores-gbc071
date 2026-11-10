package src;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<LexicalError> errors = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int tokenStartLine = 1;
    private int tokenStartColumn = 1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("int", TokenType.KW_INT);
        KEYWORDS.put("double", TokenType.KW_DOUBLE);
        KEYWORDS.put("bool", TokenType.KW_BOOL);
        KEYWORDS.put("char", TokenType.KW_CHAR);
        KEYWORDS.put("string", TokenType.KW_STRING);
        KEYWORDS.put("void", TokenType.KW_VOID);
        KEYWORDS.put("if", TokenType.KW_IF);
        KEYWORDS.put("else", TokenType.KW_ELSE);
        KEYWORDS.put("while", TokenType.KW_WHILE);
        KEYWORDS.put("for", TokenType.KW_FOR);
        KEYWORDS.put("return", TokenType.KW_RETURN);
        KEYWORDS.put("true", TokenType.KW_TRUE);
        KEYWORDS.put("false", TokenType.KW_FALSE);
    }

    public Scanner(String source) {
        this.source = source != null ? source : "";
    }

    public List<LexicalError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasNext() {
        return current < source.length();
    }

    public char peek() {
        if (current >= source.length()) return '\0';
        return source.charAt(current);
    }

    public char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    public char advance() {
        if (!hasNext()) return '\0';
        char c = source.charAt(current++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    public boolean match(char expected) {
        if (!hasNext()) return false;
        if (source.charAt(current) != expected) return false;
        advance();
        return true;
    }

    public List<Token> scanTokens() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            Token token = nextToken();
            tokens.add(token);
            if (token.getType() == TokenType.EOF) {
                break;
            }
        }
        return tokens;
    }

    public Token nextToken() {
        while (hasNext()) {
            skipWhitespace();
            if (!hasNext()) break;

            start = current;
            tokenStartLine = line;
            tokenStartColumn = column;

            char c = advance();

            if (isAlpha(c)) {
                return scanIdentifierOrKeyword();
            }

            if (isDigit(c)) {
                return scanNumber(c);
            }

            if (c == '"') {
                Token stringToken = scanString();
                if (stringToken != null) return stringToken;
                continue;
            }

            if (c == '\'') {
                Token charToken = scanChar();
                if (charToken != null) return charToken;
                continue;
            }

            switch (c) {
                case '(': return makeToken(TokenType.LPAREN);
                case ')': return makeToken(TokenType.RPAREN);
                case '{': return makeToken(TokenType.LBRACE);
                case '}': return makeToken(TokenType.RBRACE);
                case '[': return makeToken(TokenType.LBRACKET);
                case ']': return makeToken(TokenType.RBRACKET);
                case ';': return makeToken(TokenType.SEMICOLON);
                case ',': return makeToken(TokenType.COMMA);

                case '+': return makeToken(TokenType.OP_PLUS);
                case '-': return makeToken(TokenType.OP_MINUS);
                case '*': return makeToken(TokenType.OP_MULT);
                case '%': return makeToken(TokenType.OP_MOD);

                case '/':
                    if (match('/')) {
                        scanLineComment();
                        continue;
                    } else if (match('*')) {
                        boolean closed = scanBlockComment();
                        if (!closed) {
                            reportError("Comentário de bloco '/*' não fechado até o fim do arquivo (EOF)", "/*");
                        }
                        continue;
                    } else {
                        return makeToken(TokenType.OP_DIV);
                    }

                case '=':
                    return makeToken(match('=') ? TokenType.OP_EQ : TokenType.OP_ASSIGN);

                case '!':
                    return makeToken(match('=') ? TokenType.OP_NE : TokenType.OP_NOT);

                case '<':
                    return makeToken(match('=') ? TokenType.OP_LE : TokenType.OP_LT);

                case '>':
                    return makeToken(match('=') ? TokenType.OP_GE : TokenType.OP_GT);

                case '&':
                    if (match('&')) {
                        return makeToken(TokenType.OP_AND);
                    } else {
                        reportError("Caractere inesperado '&'. Esperado '&&' para operador lógico AND", "&");
                        continue;
                    }

                case '|':
                    if (match('|')) {
                        return makeToken(TokenType.OP_OR);
                    } else {
                        reportError("Caractere inesperado '|'. Esperado '||' para operador lógico OR", "|");
                        continue;
                    }

                default:
                    reportError(String.format("Caractere inválido fora do alfabeto: '%c' (ASCII %d)", c, (int) c),
                            String.valueOf(c));
                    continue;
            }
        }

        return new Token(TokenType.EOF, "", line, column);
    }

    private void skipWhitespace() {
        while (hasNext()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                advance();
            } else {
                break;
            }
        }
    }

    private Token scanIdentifierOrKeyword() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String lexeme = source.substring(start, current);
        TokenType type = KEYWORDS.getOrDefault(lexeme, TokenType.IDENTIFIER);
        return new Token(type, lexeme, tokenStartLine, tokenStartColumn);
    }

    private Token scanNumber(char firstChar) {
        while (isDigit(peek())) {
            advance();
        }

        if (peek() == '.' && isDigit(peekNext())) {
            advance();
            while (isDigit(peek())) {
                advance();
            }
            String lexeme = source.substring(start, current);
            Double val = Double.parseDouble(lexeme);
            return new Token(TokenType.LIT_DOUBLE, lexeme, tokenStartLine, tokenStartColumn, val);
        } else if (peek() == '.' && !isDigit(peekNext())) {
            advance();
            reportError("Número decimal mal formatado: esperado ao menos um dígito após o ponto '.'",
                    source.substring(start, current));
            return new Token(TokenType.ERROR, source.substring(start, current), tokenStartLine, tokenStartColumn);
        }

        String lexeme = source.substring(start, current);
        Long val = Long.parseLong(lexeme);
        return new Token(TokenType.LIT_INT, lexeme, tokenStartLine, tokenStartColumn, val);
    }

    private Token scanString() {
        StringBuilder value = new StringBuilder();
        int strOpenLine = tokenStartLine;
        int strOpenCol = tokenStartColumn;

        while (hasNext()) {
            char c = peek();

            if (c == '\n' || c == '\r') {
                reportErrorAt(strOpenLine, strOpenCol,
                        "String não fechada antes do fim da linha",
                        source.substring(start, current));
                return null;
            }

            if (c == '"') {
                advance();
                String lexeme = source.substring(start, current);
                return new Token(TokenType.LIT_STRING, lexeme, tokenStartLine, tokenStartColumn, value.toString());
            }

            if (c == '\\') {
                advance();
                if (!hasNext()) {
                    reportErrorAt(strOpenLine, strOpenCol,
                            "Sequência de escape inacabada até o fim do arquivo (EOF)",
                            source.substring(start, current));
                    return null;
                }
                char escape = advance();
                switch (escape) {
                    case 'n': value.append('\n'); break;
                    case 't': value.append('\t'); break;
                    case '"': value.append('"'); break;
                    case '\\': value.append('\\'); break;
                    default:
                        reportError("Sequência de escape inválida '\\" + escape + "'", "\\" + escape);
                        value.append(escape);
                }
            } else {
                value.append(advance());
            }
        }

        reportErrorAt(strOpenLine, strOpenCol,
                "String não fechada até o fim do arquivo (EOF)",
                source.substring(start, current));
        return null;
    }

    private Token scanChar() {
        int charOpenLine = tokenStartLine;
        int charOpenCol = tokenStartColumn;

        if (!hasNext() || peek() == '\n') {
            reportErrorAt(charOpenLine, charOpenCol, "Literal de char vazio ou não fechado", "'");
            return null;
        }

        char ch = advance();
        char resolvedChar = ch;

        if (ch == '\\') {
            if (!hasNext()) {
                reportErrorAt(charOpenLine, charOpenCol, "Escape em char não fechado até EOF", "'\\");
                return null;
            }
            char escape = advance();
            switch (escape) {
                case 'n': resolvedChar = '\n'; break;
                case 't': resolvedChar = '\t'; break;
                case '\'': resolvedChar = '\''; break;
                case '\\': resolvedChar = '\\'; break;
                default:
                    reportError("Sequência de escape inválida em char '\\" + escape + "'", "\\" + escape);
                    resolvedChar = escape;
            }
        }

        if (!match('\'')) {
            reportErrorAt(charOpenLine, charOpenCol, "Literal de char não fechado com apóstrofo",
                    source.substring(start, current));
            return null;
        }

        String lexeme = source.substring(start, current);
        return new Token(TokenType.LIT_CHAR, lexeme, tokenStartLine, tokenStartColumn, resolvedChar);
    }

    private void scanLineComment() {
        while (hasNext() && peek() != '\n') {
            advance();
        }
    }

    private boolean scanBlockComment() {
        while (hasNext()) {
            if (peek() == '*' && peekNext() == '/') {
                advance();
                advance();
                return true;
            }
            advance();
        }
        return false;
    }

    private Token makeToken(TokenType type) {
        String lexeme = source.substring(start, current);
        return new Token(type, lexeme, tokenStartLine, tokenStartColumn);
    }

    private void reportError(String message, String offendingText) {
        reportErrorAt(tokenStartLine, tokenStartColumn, message, offendingText);
    }

    private void reportErrorAt(int errLine, int errCol, String message, String offendingText) {
        LexicalError error = new LexicalError(message, offendingText, errLine, errCol);
        errors.add(error);
        System.err.println(error);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
