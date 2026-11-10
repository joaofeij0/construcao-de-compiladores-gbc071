package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static src.TokenType.*;

/**
 * Parser recursivo descendente (reconhecedor) da linguagem. Implementa, método a método,
 * a gramática LL(1) de gramatica-first-follow.md: um parseX() por não-terminal.
 *
 * Tradução (Aula 6):
 *   terminal      -> match()
 *   não-terminal  -> chamada recursiva parseX()
 *   alternativa   -> escolhida pelo lookahead, usando os conjuntos FIRST abaixo
 *   epsilon       -> versão ROBUSTA: só é aceito se o lookahead estiver em FOLLOW(X);
 *                    caso contrário é erro (e não "não fazer nada")
 *
 * Recuperação em modo pânico: ao detectar um erro dentro de parseX(), registra-se o erro e
 * descartam-se tokens até um token de sincronização de X: FOLLOW(X) mais as âncoras ';', '{' e '}'.
 * O parser nunca para no primeiro erro e nunca entra em loop (veja "Garantia de progresso" abaixo).
 *
 * Garantia de progresso: toda recursão que forma laço (DeclList, StmtList, ParamListTail,
 * ArgListTail e os *Rest de expressão) só reentra depois de consumir pelo menos um token, ou
 * depois de uma sincronização que descarta o token ofensor (que, por construção, não pertence ao
 * conjunto de sincronização, exceto EOF, onde a recursão não ocorre).
 */
public class Parser {

    // ============================== FIRST ==============================
    static final Set<TokenType> FIRST_TYPE    = setOf(KW_INT, KW_DOUBLE, KW_BOOL, KW_CHAR, KW_STRING);
    static final Set<TokenType> FIRST_PRIMARY = setOf(LIT_INT, LIT_DOUBLE, LIT_STRING, LIT_CHAR,
                                                      KW_TRUE, KW_FALSE, LPAREN, IDENTIFIER);
    static final Set<TokenType> FIRST_EXPR    = union(FIRST_PRIMARY, setOf(OP_NOT, OP_MINUS));
    static final Set<TokenType> FIRST_STMT    = union(FIRST_TYPE,
                                                      setOf(KW_IF, KW_WHILE, KW_FOR, KW_RETURN, LBRACE, IDENTIFIER));
    static final Set<TokenType> FIRST_DECL    = union(FIRST_TYPE, setOf(KW_VOID));
    static final Set<TokenType> FIRST_BLOCK   = setOf(LBRACE);
    static final Set<TokenType> FIRST_VARTAIL = setOf(OP_ASSIGN, SEMICOLON);

    // ============================== FOLLOW =============================
    static final Set<TokenType> FOLLOW_DECLLIST = setOf(EOF);
    static final Set<TokenType> FOLLOW_DECL     = union(FIRST_DECL, setOf(EOF));      // Declaration, DeclTail, FuncTail, VarTail
    static final Set<TokenType> FOLLOW_STMTLIST = setOf(RBRACE);
    static final Set<TokenType> FOLLOW_STMT     = union(FIRST_STMT, setOf(RBRACE));   // Stmt, VarDecl, IfStmt, ElsePart, ...
    static final Set<TokenType> FOLLOW_BLOCK    = union(FOLLOW_DECL, FOLLOW_STMT, setOf(KW_ELSE));
    static final Set<TokenType> FOLLOW_TYPE     = setOf(IDENTIFIER);
    static final Set<TokenType> FOLLOW_SEMI     = setOf(SEMICOLON);                   // InitOpt, ForInit, ForCond, ReturnVal, AssignOrCallTail
    static final Set<TokenType> FOLLOW_RPAREN   = setOf(RPAREN);                      // ParamListOpt, ParamList, ParamListTail, ForStep, ArgList*
    static final Set<TokenType> FOLLOW_PARAM    = setOf(COMMA, RPAREN);
    static final Set<TokenType> FOLLOW_EXPR     = setOf(SEMICOLON, RPAREN, COMMA);    // Expr, OrExpr, OrRest
    static final Set<TokenType> FOLLOW_AND      = union(setOf(OP_OR), FOLLOW_EXPR);   // AndExpr, AndRest
    static final Set<TokenType> FOLLOW_EQ       = union(setOf(OP_AND), FOLLOW_AND);   // EqExpr, EqRest
    static final Set<TokenType> FOLLOW_REL      = union(setOf(OP_EQ, OP_NE), FOLLOW_EQ);
    static final Set<TokenType> FOLLOW_ADD      = union(setOf(OP_LT, OP_LE, OP_GT, OP_GE), FOLLOW_REL);
    static final Set<TokenType> FOLLOW_MUL      = union(setOf(OP_PLUS, OP_MINUS), FOLLOW_ADD);
    static final Set<TokenType> FOLLOW_UNARY    = union(setOf(OP_MULT, OP_DIV, OP_MOD), FOLLOW_MUL); // Unary, Primary, PrimaryTail

    /** Âncoras de sincronização acrescentadas a todo FOLLOW. */
    private static final Set<TokenType> ANCHORS = setOf(SEMICOLON, LBRACE, RBRACE);

    private static final String MSG_AFTER_OPERAND = "operador, ')', ',' ou ';'";

    // ============================== estado =============================
    /** Sinal interno de "erro neste não-terminal": registrado e capturado no próprio parseX(). */
    private static final class Abort extends RuntimeException {
        Abort() { super(null, null, false, false); }
    }
    private static final Abort ABORT = new Abort();

    private final List<Token> tokens;
    private final List<SyntaxError> errors = new ArrayList<>();
    private int pos = 0;
    private int lastErrorPos = -1;

    public Parser(List<Token> tokens) {
        this.tokens = new ArrayList<>(tokens);
        if (this.tokens.isEmpty() || this.tokens.get(this.tokens.size() - 1).getType() != EOF) {
            Token last = this.tokens.isEmpty() ? null : this.tokens.get(this.tokens.size() - 1);
            this.tokens.add(new Token(EOF, "", last == null ? 1 : last.getLine(), last == null ? 1 : last.getColumn()));
        }
    }

    /** Executa a análise. Retorna true se o programa foi aceito (nenhum erro sintático). */
    public boolean parse() {
        pos = 0;
        lastErrorPos = -1;
        errors.clear();
        parseProgram();
        return errors.isEmpty();
    }

    public List<SyntaxError> getErrors() { return Collections.unmodifiableList(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    // ====================== programa e declarações ======================

    // Program → DeclList EOF
    private void parseProgram() {
        try {
            parseDeclList();
            match(EOF);
        } catch (Abort a) {
            // fim da entrada: nada a sincronizar
        }
    }

    // DeclList → Declaration DeclList | ε
    private void parseDeclList() {
        TokenType t = peekType();
        if (FIRST_DECL.contains(t)) {
            parseDeclaration();
            parseDeclList();
        } else if (FOLLOW_DECLLIST.contains(t)) {
            // ε
        } else {
            report("declaração (tipo ou 'void') ou fim do arquivo");
            skipTo(FOLLOW_DECL);                      // FIRST(Declaration) ∪ {EOF}
            if (FIRST_DECL.contains(peekType())) parseDeclList();
        }
    }

    // Declaration → Type IDENTIFIER DeclTail | KW_VOID IDENTIFIER FuncTail
    private void parseDeclaration() {
        try {
            if (FIRST_TYPE.contains(peekType())) {
                parseType();
                match(IDENTIFIER);
                parseDeclTail();
            } else if (check(KW_VOID)) {
                match(KW_VOID);
                match(IDENTIFIER);
                parseFuncTail();
            } else {
                throw fail("declaração (tipo ou 'void')");
            }
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_DECL);
        }
    }

    // DeclTail → FuncTail | VarTail
    private void parseDeclTail() {
        try {
            if (check(LPAREN)) {
                parseFuncTail();
            } else if (FIRST_VARTAIL.contains(peekType())) {
                parseVarTail();
            } else {
                throw fail("'(' (função), '=' ou ';' (variável global)");
            }
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_DECL);
        }
    }

    // FuncTail → LPAREN ParamListOpt RPAREN Block
    private void parseFuncTail() {
        try {
            match(LPAREN);
            parseParamListOpt();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return;             // cabeçalho quebrado: procura o '{' do corpo
        }
        parseBlock();
    }

    // VarTail → InitOpt SEMICOLON
    private void parseVarTail() {
        try {
            parseInitOpt();
            match(SEMICOLON);
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_DECL);
        }
    }

    // InitOpt → OP_ASSIGN Expr | ε
    private void parseInitOpt() {
        try {
            if (check(OP_ASSIGN)) {
                match(OP_ASSIGN);
                parseExpr();
            } else if (!FOLLOW_SEMI.contains(peekType())) {
                throw fail("'=' ou ';'");
            }                                          // senão: ε (lookahead ∈ FOLLOW)
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
        }
    }

    // ParamListOpt → ParamList | ε
    private void parseParamListOpt() {
        try {
            if (FIRST_TYPE.contains(peekType())) {
                parseParamList();
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("tipo de parâmetro ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    // ParamList → Param ParamListTail
    private void parseParamList() {
        parseParam();
        parseParamListTail();
    }

    // ParamListTail → COMMA Param ParamListTail | ε
    private void parseParamListTail() {
        try {
            if (check(COMMA)) {
                match(COMMA);
                parseParam();
                parseParamListTail();
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("',' ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    // Param → Type IDENTIFIER
    private void parseParam() {
        try {
            if (!FIRST_TYPE.contains(peekType())) throw fail("tipo do parâmetro");
            parseType();
            match(IDENTIFIER);
        } catch (Abort a) {
            synchronize(FOLLOW_PARAM);
        }
    }

    // Type → KW_INT | KW_DOUBLE | KW_BOOL | KW_CHAR | KW_STRING
    private void parseType() {
        try {
            if (FIRST_TYPE.contains(peekType())) advance();
            else throw fail("tipo (int, double, bool, char ou string)");
        } catch (Abort a) {
            synchronize(FOLLOW_TYPE);
        }
    }

    // ====================== blocos e comandos ======================

    // Block → LBRACE StmtList RBRACE
    private void parseBlock() {
        try {
            match(LBRACE);
            parseStmtList();
            match(RBRACE);
        } catch (Abort a) {
            synchronize(FOLLOW_BLOCK);
        }
    }

    // StmtList → Stmt StmtList | ε
    private void parseStmtList() {
        TokenType t = peekType();
        if (FIRST_STMT.contains(t)) {
            parseStmt();
            parseStmtList();
        } else if (FOLLOW_STMTLIST.contains(t)) {
            // ε
        } else {
            report("comando ou '}'");
            skipTo(union(FIRST_STMT, FOLLOW_STMTLIST));
            if (FIRST_STMT.contains(peekType())) parseStmtList();
        }
    }

    // Stmt → VarDecl | IfStmt | WhileStmt | ForStmt | ReturnStmt | Block | AssignOrCall
    private void parseStmt() {
        try {
            TokenType t = peekType();
            if (FIRST_TYPE.contains(t))      parseVarDecl();
            else if (t == KW_IF)             parseIfStmt();
            else if (t == KW_WHILE)          parseWhileStmt();
            else if (t == KW_FOR)            parseForStmt();
            else if (t == KW_RETURN)         parseReturnStmt();
            else if (t == LBRACE)            parseBlock();
            else if (t == IDENTIFIER)        parseAssignOrCall();
            else throw fail("comando");
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
        }
    }

    // VarDecl → Type IDENTIFIER InitOpt SEMICOLON
    private void parseVarDecl() {
        try {
            parseType();
            match(IDENTIFIER);
            parseInitOpt();
            match(SEMICOLON);
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
        }
    }

    // AssignOrCall → IDENTIFIER AssignOrCallTail SEMICOLON
    private void parseAssignOrCall() {
        try {
            match(IDENTIFIER);
            parseAssignOrCallTail();
            match(SEMICOLON);
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
        }
    }

    // AssignOrCallTail → OP_ASSIGN Expr | LPAREN ArgListOpt RPAREN
    private void parseAssignOrCallTail() {
        try {
            if (check(OP_ASSIGN)) {
                match(OP_ASSIGN);
                parseExpr();
            } else if (check(LPAREN)) {
                match(LPAREN);
                parseArgListOpt();
                match(RPAREN);
            } else {
                throw fail("'=' (atribuição) ou '(' (chamada)");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
        }
    }

    // IfStmt → KW_IF LPAREN Expr RPAREN Block ElsePart
    private void parseIfStmt() {
        try {
            match(KW_IF);
            match(LPAREN);
            parseExpr();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return;
        }
        parseBlock();
        parseElsePart();
    }

    // ElsePart → KW_ELSE ElseBody | ε       (FOLLOW(ElsePart) = FOLLOW(Stmt), que NÃO contém KW_ELSE)
    private void parseElsePart() {
        try {
            if (check(KW_ELSE)) {
                match(KW_ELSE);
                parseElseBody();
            } else if (!FOLLOW_STMT.contains(peekType())) {
                throw fail("'else', comando ou '}'");
            }
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
        }
    }

    // ElseBody → IfStmt | Block
    private void parseElseBody() {
        try {
            if (check(KW_IF)) parseIfStmt();
            else if (check(LBRACE)) parseBlock();
            else throw fail("'if' ou '{'");
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
        }
    }

    // WhileStmt → KW_WHILE LPAREN Expr RPAREN Block
    private void parseWhileStmt() {
        try {
            match(KW_WHILE);
            match(LPAREN);
            parseExpr();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return;
        }
        parseBlock();
    }

    // ForStmt → KW_FOR LPAREN ForInit SEMICOLON ForCond SEMICOLON ForStep RPAREN Block
    private void parseForStmt() {
        try {
            match(KW_FOR);
            match(LPAREN);
            parseForInit();
            match(SEMICOLON);
            parseForCond();
            match(SEMICOLON);
            parseForStep();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return;
        }
        parseBlock();
    }

    // ForInit → Type IDENTIFIER InitOpt | IDENTIFIER OP_ASSIGN Expr | ε
    private void parseForInit() {
        try {
            TokenType t = peekType();
            if (FIRST_TYPE.contains(t)) {
                parseType();
                match(IDENTIFIER);
                parseInitOpt();
            } else if (t == IDENTIFIER) {
                match(IDENTIFIER);
                match(OP_ASSIGN);
                parseExpr();
            } else if (!FOLLOW_SEMI.contains(t)) {
                throw fail("declaração, atribuição ou ';'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
        }
    }

    // ForCond → Expr | ε
    private void parseForCond() {
        try {
            if (FIRST_EXPR.contains(peekType())) {
                parseExpr();
            } else if (!FOLLOW_SEMI.contains(peekType())) {
                throw fail("condição ou ';'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
        }
    }

    // ForStep → IDENTIFIER OP_ASSIGN Expr | ε
    private void parseForStep() {
        try {
            if (check(IDENTIFIER)) {
                match(IDENTIFIER);
                match(OP_ASSIGN);
                parseExpr();
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("atribuição ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    // ReturnStmt → KW_RETURN ReturnVal SEMICOLON
    private void parseReturnStmt() {
        try {
            match(KW_RETURN);
            parseReturnVal();
            match(SEMICOLON);
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
        }
    }

    // ReturnVal → Expr | ε
    private void parseReturnVal() {
        try {
            if (FIRST_EXPR.contains(peekType())) {
                parseExpr();
            } else if (!FOLLOW_SEMI.contains(peekType())) {
                throw fail("expressão ou ';'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
        }
    }

    // ============================ expressões ============================
    // (os NTs que só delegam não lançam Abort: os filhos já se recuperam sozinhos)

    // Expr → OrExpr
    private void parseExpr() { parseOrExpr(); }

    // OrExpr → AndExpr OrRest
    private void parseOrExpr() { parseAndExpr(); parseOrRest(); }

    // OrRest → OP_OR AndExpr OrRest | ε
    private void parseOrRest() {
        if (check(OP_OR)) {
            advance();
            parseAndExpr();
            parseOrRest();
        } else if (!FOLLOW_EXPR.contains(peekType())) {
            restError(FOLLOW_EXPR);
        }
    }

    // AndExpr → EqExpr AndRest
    private void parseAndExpr() { parseEqExpr(); parseAndRest(); }

    // AndRest → OP_AND EqExpr AndRest | ε
    private void parseAndRest() {
        if (check(OP_AND)) {
            advance();
            parseEqExpr();
            parseAndRest();
        } else if (!FOLLOW_AND.contains(peekType())) {
            restError(FOLLOW_AND);
        }
    }

    // EqExpr → RelExpr EqRest
    private void parseEqExpr() { parseRelExpr(); parseEqRest(); }

    // EqRest → OP_EQ RelExpr EqRest | OP_NE RelExpr EqRest | ε
    private void parseEqRest() {
        TokenType t = peekType();
        if (t == OP_EQ || t == OP_NE) {
            advance();
            parseRelExpr();
            parseEqRest();
        } else if (!FOLLOW_EQ.contains(t)) {
            restError(FOLLOW_EQ);
        }
    }

    // RelExpr → AddExpr RelRest
    private void parseRelExpr() { parseAddExpr(); parseRelRest(); }

    // RelRest → (OP_LT | OP_LE | OP_GT | OP_GE) AddExpr RelRest | ε
    private void parseRelRest() {
        TokenType t = peekType();
        if (t == OP_LT || t == OP_LE || t == OP_GT || t == OP_GE) {
            advance();
            parseAddExpr();
            parseRelRest();
        } else if (!FOLLOW_REL.contains(t)) {
            restError(FOLLOW_REL);
        }
    }

    // AddExpr → MulExpr AddRest
    private void parseAddExpr() { parseMulExpr(); parseAddRest(); }

    // AddRest → (OP_PLUS | OP_MINUS) MulExpr AddRest | ε
    private void parseAddRest() {
        TokenType t = peekType();
        if (t == OP_PLUS || t == OP_MINUS) {
            advance();
            parseMulExpr();
            parseAddRest();
        } else if (!FOLLOW_ADD.contains(t)) {
            restError(FOLLOW_ADD);
        }
    }

    // MulExpr → Unary MulRest
    private void parseMulExpr() { parseUnary(); parseMulRest(); }

    // MulRest → (OP_MULT | OP_DIV | OP_MOD) Unary MulRest | ε
    private void parseMulRest() {
        TokenType t = peekType();
        if (t == OP_MULT || t == OP_DIV || t == OP_MOD) {
            advance();
            parseUnary();
            parseMulRest();
        } else if (!FOLLOW_MUL.contains(t)) {
            restError(FOLLOW_MUL);
        }
    }

    // Unary → OP_NOT Unary | OP_MINUS Unary | Primary
    private void parseUnary() {
        try {
            TokenType t = peekType();
            if (t == OP_NOT || t == OP_MINUS) {
                advance();
                parseUnary();
            } else if (FIRST_PRIMARY.contains(t)) {
                parsePrimary();
            } else {
                throw fail("expressão");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_UNARY);
        }
    }

    // Primary → LIT_INT | LIT_DOUBLE | LIT_STRING | LIT_CHAR | KW_TRUE | KW_FALSE
    //         | LPAREN Expr RPAREN | IDENTIFIER PrimaryTail
    private void parsePrimary() {
        try {
            switch (peekType()) {
                case LIT_INT: case LIT_DOUBLE: case LIT_STRING: case LIT_CHAR:
                case KW_TRUE: case KW_FALSE:
                    advance();
                    break;
                case LPAREN:
                    match(LPAREN);
                    parseExpr();
                    match(RPAREN);
                    break;
                case IDENTIFIER:
                    match(IDENTIFIER);
                    parsePrimaryTail();
                    break;
                default:
                    throw fail("expressão");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_UNARY);
        }
    }

    // PrimaryTail → LPAREN ArgListOpt RPAREN | ε
    private void parsePrimaryTail() {
        try {
            if (check(LPAREN)) {
                match(LPAREN);
                parseArgListOpt();
                match(RPAREN);
            } else if (!FOLLOW_UNARY.contains(peekType())) {
                throw fail("'(' (chamada), " + MSG_AFTER_OPERAND);
            }
        } catch (Abort a) {
            synchronize(FOLLOW_UNARY);
        }
    }

    // ArgListOpt → ArgList | ε
    private void parseArgListOpt() {
        try {
            if (FIRST_EXPR.contains(peekType())) {
                parseArgList();
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("argumento ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    // ArgList → Expr ArgListTail
    private void parseArgList() {
        parseExpr();
        parseArgListTail();
    }

    // ArgListTail → COMMA Expr ArgListTail | ε
    private void parseArgListTail() {
        try {
            if (check(COMMA)) {
                match(COMMA);
                parseExpr();
                parseArgListTail();
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("',' ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    // ============================ infraestrutura ============================

    private Token peek() { return tokens.get(pos); }
    private TokenType peekType() { return tokens.get(pos).getType(); }
    private boolean check(TokenType t) { return peekType() == t; }

    /** Consome o token atual (nunca passa do EOF). */
    private Token advance() {
        Token t = tokens.get(pos);
        if (t.getType() != EOF) pos++;
        return t;
    }

    private Token match(TokenType expected) {
        if (check(expected)) return advance();
        throw fail(display(expected));
    }

    /** Registra o erro e devolve o sinal a ser lançado (throw fail(...)). */
    private Abort fail(String expected) {
        report(expected);
        return ABORT;
    }

    /** Registra um erro sintático na posição do token atual (sem repetir o mesmo ponto). */
    private void report(String expected) {
        if (pos == lastErrorPos) return;
        lastErrorPos = pos;
        Token t = peek();
        errors.add(new SyntaxError("esperado " + expected + ", encontrado " + found(t),
                t.getLine(), t.getColumn()));
    }

    /** Erro detectado num *Rest* (que termina a produção): registra e sincroniza. */
    private void restError(Set<TokenType> follow) {
        report(MSG_AFTER_OPERAND);
        synchronize(follow);
    }

    /** Modo pânico: descarta tokens até FOLLOW ∪ âncoras (ou EOF). Não consome o token de parada. */
    private void synchronize(Set<TokenType> follow) {
        while (!check(EOF) && !follow.contains(peekType()) && !ANCHORS.contains(peekType())) {
            advance();
        }
    }

    /** Sincronização de comando: como synchronize, mas consome um ';' final (encerra o comando quebrado). */
    private void synchronizeStmt(Set<TokenType> follow) {
        synchronize(follow);
        if (check(SEMICOLON)) advance();
    }

    /** Descarta tokens até um token de 'stop' (ou EOF), sem âncoras. */
    private void skipTo(Set<TokenType> stop) {
        while (!check(EOF) && !stop.contains(peekType())) advance();
    }

    /** Cabeçalho de função/if/while/for quebrado: procura o '{' do corpo. true se achou. */
    private boolean recoverHeader() {
        synchronize(FIRST_BLOCK);
        if (check(LBRACE)) return true;
        if (check(SEMICOLON)) advance();
        return false;
    }

    private static String found(Token t) {
        if (t.getType() == EOF) return "fim do arquivo";
        if (t.getType() == ERROR) return "token inválido '" + t.getLexeme() + "'";
        return "'" + t.getLexeme() + "'";
    }

    private static String display(TokenType t) {
        switch (t) {
            case LPAREN: return "'('";      case RPAREN: return "')'";
            case LBRACE: return "'{'";      case RBRACE: return "'}'";
            case SEMICOLON: return "';'";   case COMMA: return "','";
            case OP_ASSIGN: return "'='";   case IDENTIFIER: return "identificador";
            case KW_IF: return "'if'";      case KW_WHILE: return "'while'";
            case KW_FOR: return "'for'";    case KW_RETURN: return "'return'";
            case KW_VOID: return "'void'";  case EOF: return "fim do arquivo";
            default: return t.name();
        }
    }

    private static Set<TokenType> setOf(TokenType first, TokenType... rest) {
        return EnumSet.of(first, rest);
    }

    @SafeVarargs
    private static Set<TokenType> union(Set<TokenType>... sets) {
        EnumSet<TokenType> r = EnumSet.noneOf(TokenType.class);
        for (Set<TokenType> s : sets) r.addAll(s);
        return r;
    }
}
