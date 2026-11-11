package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import src.ast.*;

import static src.TokenType.*;

public class Parser {
    static final Set<TokenType> FIRST_TYPE    = setOf(KW_INT, KW_DOUBLE, KW_BOOL, KW_CHAR, KW_STRING);
    static final Set<TokenType> FIRST_PRIMARY = setOf(LIT_INT, LIT_DOUBLE, LIT_STRING, LIT_CHAR,
                                                      KW_TRUE, KW_FALSE, LPAREN, IDENTIFIER);
    static final Set<TokenType> FIRST_EXPR    = union(FIRST_PRIMARY, setOf(OP_NOT, OP_MINUS));
    static final Set<TokenType> FIRST_STMT    = union(FIRST_TYPE,
                                                      setOf(KW_IF, KW_WHILE, KW_FOR, KW_RETURN, LBRACE, IDENTIFIER));
    static final Set<TokenType> FIRST_DECL    = union(FIRST_TYPE, setOf(KW_VOID));
    static final Set<TokenType> FIRST_BLOCK   = setOf(LBRACE);
    static final Set<TokenType> FIRST_VARTAIL = setOf(OP_ASSIGN, SEMICOLON);

    static final Set<TokenType> FOLLOW_DECLLIST = setOf(EOF);
    static final Set<TokenType> FOLLOW_DECL     = union(FIRST_DECL, setOf(EOF));
    static final Set<TokenType> FOLLOW_STMTLIST = setOf(RBRACE);
    static final Set<TokenType> FOLLOW_STMT     = union(FIRST_STMT, setOf(RBRACE));
    static final Set<TokenType> FOLLOW_BLOCK    = union(FOLLOW_DECL, FOLLOW_STMT, setOf(KW_ELSE));
    static final Set<TokenType> FOLLOW_TYPE     = setOf(IDENTIFIER);
    static final Set<TokenType> FOLLOW_SEMI     = setOf(SEMICOLON);
    static final Set<TokenType> FOLLOW_RPAREN   = setOf(RPAREN);
    static final Set<TokenType> FOLLOW_PARAM    = setOf(COMMA, RPAREN);
    static final Set<TokenType> FOLLOW_EXPR     = setOf(SEMICOLON, RPAREN, COMMA);
    static final Set<TokenType> FOLLOW_AND      = union(setOf(OP_OR), FOLLOW_EXPR);
    static final Set<TokenType> FOLLOW_EQ       = union(setOf(OP_AND), FOLLOW_AND);
    static final Set<TokenType> FOLLOW_REL      = union(setOf(OP_EQ, OP_NE), FOLLOW_EQ);
    static final Set<TokenType> FOLLOW_ADD      = union(setOf(OP_LT, OP_LE, OP_GT, OP_GE), FOLLOW_REL);
    static final Set<TokenType> FOLLOW_MUL      = union(setOf(OP_PLUS, OP_MINUS), FOLLOW_ADD);
    static final Set<TokenType> FOLLOW_UNARY    = union(setOf(OP_MULT, OP_DIV, OP_MOD), FOLLOW_MUL);

    private static final Set<TokenType> ANCHORS = setOf(SEMICOLON, LBRACE, RBRACE);

    private static final String MSG_AFTER_OPERAND = "operador, ')', ',' ou ';'";

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

    public ParseResult parse() {
        pos = 0;
        lastErrorPos = -1;
        errors.clear();
        Program program = parseProgram();
        return new ParseResult(program, errors);
    }

    public List<SyntaxError> getErrors() { return Collections.unmodifiableList(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    private Program parseProgram() {
        Token first = peek();
        List<Decl> decls = new ArrayList<>();
        try {
            parseDeclList(decls);
            match(EOF);
        } catch (Abort a) {
        }
        return new Program(decls, first.getLine(), first.getColumn());
    }

    private void parseDeclList(List<Decl> out) {
        TokenType t = peekType();
        if (FIRST_DECL.contains(t)) {
            addIfPresent(out, parseDeclaration());
            parseDeclList(out);
        } else if (FOLLOW_DECLLIST.contains(t)) {
        } else {
            report("declaração (tipo ou 'void') ou fim do arquivo");
            skipTo(FOLLOW_DECL);
            if (FIRST_DECL.contains(peekType())) parseDeclList(out);
        }
    }

    private Decl parseDeclaration() {
        Token first = peek();
        try {
            if (FIRST_TYPE.contains(peekType())) {
                BasicType type = parseType();
                Token name = match(IDENTIFIER);
                return parseDeclTail(first, type, name);
            } else if (check(KW_VOID)) {
                match(KW_VOID);
                Token name = match(IDENTIFIER);
                return parseFuncTail(first, null, name);
            } else {
                throw fail("declaração (tipo ou 'void')");
            }
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_DECL);
            return null;
        }
    }

    private Decl parseDeclTail(Token first, BasicType type, Token name) {
        try {
            if (check(LPAREN)) {
                return parseFuncTail(first, type, name);
            } else if (FIRST_VARTAIL.contains(peekType())) {
                return parseVarTail(first, type, name);
            } else {
                throw fail("'(' (função), '=' ou ';' (variável global)");
            }
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_DECL);
            return null;
        }
    }

    private RoutineDecl parseFuncTail(Token first, BasicType returnType, Token name) {
        List<Param> params = new ArrayList<>();
        try {
            match(LPAREN);
            parseParamListOpt(params);
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return null;
        }
        Block body = parseBlock();
        if (returnType == null) {
            return new ProcDecl(name.getLexeme(), params, body, first.getLine(), first.getColumn());
        }
        return new FuncDecl(returnType, name.getLexeme(), params, body, first.getLine(), first.getColumn());
    }

    private VarDecl parseVarTail(Token first, BasicType type, Token name) {
        try {
            Expr init = parseInitOpt();
            match(SEMICOLON);
            return new VarDecl(type, name.getLexeme(), init, first.getLine(), first.getColumn());
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_DECL);
            return null;
        }
    }

    private Expr parseInitOpt() {
        try {
            if (check(OP_ASSIGN)) {
                match(OP_ASSIGN);
                return parseExpr();
            } else if (!FOLLOW_SEMI.contains(peekType())) {
                throw fail("'=' ou ';'");
            }
            return null;
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
            return null;
        }
    }

    private void parseParamListOpt(List<Param> out) {
        try {
            if (FIRST_TYPE.contains(peekType())) {
                parseParamList(out);
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("tipo de parâmetro ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    private void parseParamList(List<Param> out) {
        addIfPresent(out, parseParam());
        parseParamListTail(out);
    }

    private void parseParamListTail(List<Param> out) {
        try {
            if (check(COMMA)) {
                match(COMMA);
                addIfPresent(out, parseParam());
                parseParamListTail(out);
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("',' ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    private Param parseParam() {
        Token first = peek();
        try {
            if (!FIRST_TYPE.contains(peekType())) throw fail("tipo do parâmetro");
            BasicType type = parseType();
            Token name = match(IDENTIFIER);
            return new Param(type, name.getLexeme(), first.getLine(), first.getColumn());
        } catch (Abort a) {
            synchronize(FOLLOW_PARAM);
            return null;
        }
    }

    private BasicType parseType() {
        try {
            if (FIRST_TYPE.contains(peekType())) return basicType(advance().getType());
            else throw fail("tipo (int, double, bool, char ou string)");
        } catch (Abort a) {
            synchronize(FOLLOW_TYPE);
            return null;
        }
    }

    private Block parseBlock() {
        Token first = peek();
        try {
            match(LBRACE);
            List<Stmt> stmts = new ArrayList<>();
            parseStmtList(stmts);
            match(RBRACE);
            return new Block(stmts, first.getLine(), first.getColumn());
        } catch (Abort a) {
            synchronize(FOLLOW_BLOCK);
            return null;
        }
    }

    private void parseStmtList(List<Stmt> out) {
        TokenType t = peekType();
        if (FIRST_STMT.contains(t)) {
            addIfPresent(out, parseStmt());
            parseStmtList(out);
        } else if (FOLLOW_STMTLIST.contains(t)) {
        } else {
            report("comando ou '}'");
            skipTo(union(FIRST_STMT, FOLLOW_STMTLIST));
            if (FIRST_STMT.contains(peekType())) parseStmtList(out);
        }
    }

    private Stmt parseStmt() {
        try {
            TokenType t = peekType();
            if (FIRST_TYPE.contains(t))      return parseVarDecl();
            else if (t == KW_IF)             return parseIfStmt();
            else if (t == KW_WHILE)          return parseWhileStmt();
            else if (t == KW_FOR)            return parseForStmt();
            else if (t == KW_RETURN)         return parseReturnStmt();
            else if (t == LBRACE)            return parseBlock();
            else if (t == IDENTIFIER)        return parseAssignOrCall();
            else throw fail("comando");
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
            return null;
        }
    }

    private VarDecl parseVarDecl() {
        Token first = peek();
        try {
            BasicType type = parseType();
            Token name = match(IDENTIFIER);
            Expr init = parseInitOpt();
            match(SEMICOLON);
            return new VarDecl(type, name.getLexeme(), init, first.getLine(), first.getColumn());
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
            return null;
        }
    }

    private Stmt parseAssignOrCall() {
        try {
            Token id = match(IDENTIFIER);
            Stmt stmt = parseAssignOrCallTail(id);
            match(SEMICOLON);
            return stmt;
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
            return null;
        }
    }

    private Stmt parseAssignOrCallTail(Token id) {
        try {
            if (check(OP_ASSIGN)) {
                match(OP_ASSIGN);
                Expr value = parseExpr();
                return new Assign(id.getLexeme(), value, id.getLine(), id.getColumn());
            } else if (check(LPAREN)) {
                match(LPAREN);
                List<Expr> args = new ArrayList<>();
                parseArgListOpt(args);
                match(RPAREN);
                Call call = new Call(id.getLexeme(), args, id.getLine(), id.getColumn());
                return new CallStmt(call, id.getLine(), id.getColumn());
            } else {
                throw fail("'=' (atribuição) ou '(' (chamada)");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
            return null;
        }
    }

    private If parseIfStmt() {
        Token first = peek();
        Expr cond = null;
        try {
            match(KW_IF);
            match(LPAREN);
            cond = parseExpr();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return null;
        }
        Block thenBranch = parseBlock();
        Stmt elseBranch = parseElsePart();
        return new If(cond, thenBranch, elseBranch, first.getLine(), first.getColumn());
    }

    private Stmt parseElsePart() {
        try {
            if (check(KW_ELSE)) {
                match(KW_ELSE);
                return parseElseBody();
            } else if (!FOLLOW_STMT.contains(peekType())) {
                throw fail("'else', comando ou '}'");
            }
            return null;
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
            return null;
        }
    }

    private Stmt parseElseBody() {
        try {
            if (check(KW_IF)) return parseIfStmt();
            else if (check(LBRACE)) return parseBlock();
            else throw fail("'if' ou '{'");
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
            return null;
        }
    }

    private While parseWhileStmt() {
        Token first = peek();
        Expr cond = null;
        try {
            match(KW_WHILE);
            match(LPAREN);
            cond = parseExpr();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return null;
        }
        Block body = parseBlock();
        return new While(cond, body, first.getLine(), first.getColumn());
    }

    private For parseForStmt() {
        Token first = peek();
        Stmt init = null;
        Expr cond = null;
        Assign step = null;
        try {
            match(KW_FOR);
            match(LPAREN);
            init = parseForInit();
            match(SEMICOLON);
            cond = parseForCond();
            match(SEMICOLON);
            step = parseForStep();
            match(RPAREN);
        } catch (Abort a) {
            if (!recoverHeader()) return null;
        }
        Block body = parseBlock();
        return new For(init, cond, step, body, first.getLine(), first.getColumn());
    }

    private Stmt parseForInit() {
        Token first = peek();
        try {
            TokenType t = peekType();
            if (FIRST_TYPE.contains(t)) {
                BasicType type = parseType();
                Token name = match(IDENTIFIER);
                Expr init = parseInitOpt();
                return new VarDecl(type, name.getLexeme(), init, first.getLine(), first.getColumn());
            } else if (t == IDENTIFIER) {
                Token id = match(IDENTIFIER);
                match(OP_ASSIGN);
                Expr value = parseExpr();
                return new Assign(id.getLexeme(), value, id.getLine(), id.getColumn());
            } else if (!FOLLOW_SEMI.contains(t)) {
                throw fail("declaração, atribuição ou ';'");
            }
            return null;
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
            return null;
        }
    }

    private Expr parseForCond() {
        try {
            if (FIRST_EXPR.contains(peekType())) {
                return parseExpr();
            } else if (!FOLLOW_SEMI.contains(peekType())) {
                throw fail("condição ou ';'");
            }
            return null;
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
            return null;
        }
    }

    private Assign parseForStep() {
        try {
            if (check(IDENTIFIER)) {
                Token id = match(IDENTIFIER);
                match(OP_ASSIGN);
                Expr value = parseExpr();
                return new Assign(id.getLexeme(), value, id.getLine(), id.getColumn());
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("atribuição ou ')'");
            }
            return null;
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
            return null;
        }
    }

    private Return parseReturnStmt() {
        Token first = peek();
        try {
            match(KW_RETURN);
            Expr value = parseReturnVal();
            match(SEMICOLON);
            return new Return(value, first.getLine(), first.getColumn());
        } catch (Abort a) {
            synchronizeStmt(FOLLOW_STMT);
            return null;
        }
    }

    private Expr parseReturnVal() {
        try {
            if (FIRST_EXPR.contains(peekType())) {
                return parseExpr();
            } else if (!FOLLOW_SEMI.contains(peekType())) {
                throw fail("expressão ou ';'");
            }
            return null;
        } catch (Abort a) {
            synchronize(FOLLOW_SEMI);
            return null;
        }
    }

    private Expr parseExpr() { return parseOrExpr(); }

    private Expr parseOrExpr() {
        Token first = peek();
        Expr left = parseAndExpr();
        return parseOrRest(left, first);
    }

    private Expr parseOrRest(Expr left, Token first) {
        if (check(OP_OR)) {
            advance();
            Expr right = parseAndExpr();
            return parseOrRest(bin(BinOp.OR, left, right, first), first);
        } else if (!FOLLOW_EXPR.contains(peekType())) {
            restError(FOLLOW_EXPR);
        }
        return left;
    }

    private Expr parseAndExpr() {
        Token first = peek();
        Expr left = parseEqExpr();
        return parseAndRest(left, first);
    }

    private Expr parseAndRest(Expr left, Token first) {
        if (check(OP_AND)) {
            advance();
            Expr right = parseEqExpr();
            return parseAndRest(bin(BinOp.AND, left, right, first), first);
        } else if (!FOLLOW_AND.contains(peekType())) {
            restError(FOLLOW_AND);
        }
        return left;
    }

    private Expr parseEqExpr() {
        Token first = peek();
        Expr left = parseRelExpr();
        return parseEqRest(left, first);
    }

    private Expr parseEqRest(Expr left, Token first) {
        TokenType t = peekType();
        if (t == OP_EQ || t == OP_NE) {
            BinOp op = binOp(advance().getType());
            Expr right = parseRelExpr();
            return parseEqRest(bin(op, left, right, first), first);
        } else if (!FOLLOW_EQ.contains(t)) {
            restError(FOLLOW_EQ);
        }
        return left;
    }

    private Expr parseRelExpr() {
        Token first = peek();
        Expr left = parseAddExpr();
        return parseRelRest(left, first);
    }

    private Expr parseRelRest(Expr left, Token first) {
        TokenType t = peekType();
        if (t == OP_LT || t == OP_LE || t == OP_GT || t == OP_GE) {
            BinOp op = binOp(advance().getType());
            Expr right = parseAddExpr();
            return parseRelRest(bin(op, left, right, first), first);
        } else if (!FOLLOW_REL.contains(t)) {
            restError(FOLLOW_REL);
        }
        return left;
    }

    private Expr parseAddExpr() {
        Token first = peek();
        Expr left = parseMulExpr();
        return parseAddRest(left, first);
    }

    private Expr parseAddRest(Expr left, Token first) {
        TokenType t = peekType();
        if (t == OP_PLUS || t == OP_MINUS) {
            BinOp op = binOp(advance().getType());
            Expr right = parseMulExpr();
            return parseAddRest(bin(op, left, right, first), first);
        } else if (!FOLLOW_ADD.contains(t)) {
            restError(FOLLOW_ADD);
        }
        return left;
    }

    private Expr parseMulExpr() {
        Token first = peek();
        Expr left = parseUnary();
        return parseMulRest(left, first);
    }

    private Expr parseMulRest(Expr left, Token first) {
        TokenType t = peekType();
        if (t == OP_MULT || t == OP_DIV || t == OP_MOD) {
            BinOp op = binOp(advance().getType());
            Expr right = parseUnary();
            return parseMulRest(bin(op, left, right, first), first);
        } else if (!FOLLOW_MUL.contains(t)) {
            restError(FOLLOW_MUL);
        }
        return left;
    }

    private Expr parseUnary() {
        try {
            TokenType t = peekType();
            if (t == OP_NOT || t == OP_MINUS) {
                Token op = advance();
                Expr operand = parseUnary();
                return new Unary(t == OP_NOT ? UnOp.NOT : UnOp.NEG, operand, op.getLine(), op.getColumn());
            } else if (FIRST_PRIMARY.contains(t)) {
                return parsePrimary();
            } else {
                throw fail("expressão");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_UNARY);
            return null;
        }
    }

    private Expr parsePrimary() {
        try {
            switch (peekType()) {
                case LIT_INT: case LIT_DOUBLE: case LIT_STRING: case LIT_CHAR:
                case KW_TRUE: case KW_FALSE:
                    return literal(advance());
                case LPAREN: {
                    match(LPAREN);
                    Expr inner = parseExpr();
                    match(RPAREN);
                    return inner;
                }
                case IDENTIFIER: {
                    Token id = match(IDENTIFIER);
                    return parsePrimaryTail(id);
                }
                default:
                    throw fail("expressão");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_UNARY);
            return null;
        }
    }

    private Expr parsePrimaryTail(Token id) {
        try {
            if (check(LPAREN)) {
                match(LPAREN);
                List<Expr> args = new ArrayList<>();
                parseArgListOpt(args);
                match(RPAREN);
                return new Call(id.getLexeme(), args, id.getLine(), id.getColumn());
            } else if (!FOLLOW_UNARY.contains(peekType())) {
                throw fail("'(' (chamada), " + MSG_AFTER_OPERAND);
            }
            return new Id(id.getLexeme(), id.getLine(), id.getColumn());
        } catch (Abort a) {
            synchronize(FOLLOW_UNARY);
            return null;
        }
    }

    private void parseArgListOpt(List<Expr> out) {
        try {
            if (FIRST_EXPR.contains(peekType())) {
                parseArgList(out);
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("argumento ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    private void parseArgList(List<Expr> out) {
        addIfPresent(out, parseExpr());
        parseArgListTail(out);
    }

    private void parseArgListTail(List<Expr> out) {
        try {
            if (check(COMMA)) {
                match(COMMA);
                addIfPresent(out, parseExpr());
                parseArgListTail(out);
            } else if (!FOLLOW_RPAREN.contains(peekType())) {
                throw fail("',' ou ')'");
            }
        } catch (Abort a) {
            synchronize(FOLLOW_RPAREN);
        }
    }

    private static Bin bin(BinOp op, Expr left, Expr right, Token first) {
        return new Bin(op, left, right, first.getLine(), first.getColumn());
    }

    private static Expr literal(Token t) {
        int line = t.getLine();
        int col = t.getColumn();
        Object v = t.getLiteral();
        switch (t.getType()) {
            case LIT_INT:
                return new IntLit(v != null ? (Long) v : Long.parseLong(t.getLexeme()), line, col);
            case LIT_DOUBLE:
                return new DoubleLit(v != null ? (Double) v : Double.parseDouble(t.getLexeme()),
                        t.getLexeme(), line, col);
            case LIT_STRING:
                return new StringLit(v != null ? (String) v : stripDelimiters(t.getLexeme()), line, col);
            case LIT_CHAR:
                return new CharLit(v != null ? (Character) v : stripDelimiters(t.getLexeme()).charAt(0), line, col);
            case KW_TRUE:
                return new BoolLit(true, line, col);
            default:
                return new BoolLit(false, line, col);
        }
    }

    private static String stripDelimiters(String lexeme) {
        return lexeme.length() >= 2 ? lexeme.substring(1, lexeme.length() - 1) : "";
    }

    private static BasicType basicType(TokenType t) {
        switch (t) {
            case KW_INT:    return BasicType.INT;
            case KW_DOUBLE: return BasicType.DOUBLE;
            case KW_BOOL:   return BasicType.BOOL;
            case KW_CHAR:   return BasicType.CHAR;
            default:        return BasicType.STRING;
        }
    }

    private static BinOp binOp(TokenType t) {
        switch (t) {
            case OP_EQ:    return BinOp.EQ;
            case OP_NE:    return BinOp.NE;
            case OP_LT:    return BinOp.LT;
            case OP_LE:    return BinOp.LE;
            case OP_GT:    return BinOp.GT;
            case OP_GE:    return BinOp.GE;
            case OP_PLUS:  return BinOp.ADD;
            case OP_MINUS: return BinOp.SUB;
            case OP_MULT:  return BinOp.MUL;
            case OP_DIV:   return BinOp.DIV;
            default:       return BinOp.MOD;
        }
    }

    private static <N> void addIfPresent(List<N> list, N node) {
        if (node != null) list.add(node);
    }

    private Token peek() { return tokens.get(pos); }
    private TokenType peekType() { return tokens.get(pos).getType(); }
    private boolean check(TokenType t) { return peekType() == t; }

    private Token advance() {
        Token t = tokens.get(pos);
        if (t.getType() != EOF) pos++;
        return t;
    }

    private Token match(TokenType expected) {
        if (check(expected)) return advance();
        throw fail(display(expected));
    }

    private Abort fail(String expected) {
        report(expected);
        return ABORT;
    }

    private void report(String expected) {
        if (pos == lastErrorPos) return;
        lastErrorPos = pos;
        Token t = peek();
        errors.add(new SyntaxError("esperado " + expected + ", encontrado " + found(t),
                t.getLine(), t.getColumn()));
    }

    private void restError(Set<TokenType> follow) {
        report(MSG_AFTER_OPERAND);
        synchronize(follow);
    }

    private void synchronize(Set<TokenType> follow) {
        while (!check(EOF) && !follow.contains(peekType()) && !ANCHORS.contains(peekType())) {
            advance();
        }
    }

    private void synchronizeStmt(Set<TokenType> follow) {
        synchronize(follow);
        if (check(SEMICOLON)) advance();
    }

    private void skipTo(Set<TokenType> stop) {
        while (!check(EOF) && !stop.contains(peekType())) advance();
    }

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
