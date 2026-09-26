package src;

public enum TokenType {
    KW_INT,         // int
    KW_DOUBLE,      // double
    KW_BOOL,        // bool
    KW_CHAR,        // char
    KW_STRING,      // string
    KW_VOID,        // void
    KW_IF,          // if
    KW_ELSE,        // else
    KW_WHILE,       // while
    KW_FOR,         // for
    KW_RETURN,      // return
    KW_TRUE,        // true
    KW_FALSE,       // false

    IDENTIFIER,     // [a-zA-Z_][a-zA-Z0-9_]*
    LIT_INT,        // 1234
    LIT_DOUBLE,     // 3.1415
    LIT_STRING,     // "texto"
    LIT_CHAR,       // 'c'

    OP_ASSIGN,      // =
    OP_PLUS,        // +
    OP_MINUS,       // -
    OP_MULT,        // *
    OP_DIV,         // /
    OP_MOD,         // %

    OP_EQ,          // ==
    OP_NE,          // !=
    OP_LT,          // <
    OP_LE,          // <=
    OP_GT,          // >
    OP_GE,          // >=
    OP_AND,         // &&
    OP_OR,          // ||
    OP_NOT,         // !

    LPAREN,         // (
    RPAREN,         // )
    LBRACE,         // {
    RBRACE,         // }
    LBRACKET,       // [
    RBRACKET,       // ]
    SEMICOLON,      // ;
    COMMA,          // ,

    EOF,            // Fim de arquivo
    ERROR           // Token emitido em caso de erro léxico para recuperação
}
