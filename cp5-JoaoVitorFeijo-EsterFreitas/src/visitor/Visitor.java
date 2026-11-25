package src.visitor;

import src.ast.*;

public interface Visitor<T> {
    T visitProgram(Program n);
    T visitVarDecl(VarDecl n);
    T visitProcDecl(ProcDecl n);
    T visitFuncDecl(FuncDecl n);
    T visitParam(Param n);

    T visitBlock(Block n);
    T visitAssign(Assign n);
    T visitIf(If n);
    T visitWhile(While n);
    T visitFor(For n);
    T visitReturn(Return n);
    T visitCallStmt(CallStmt n);

    T visitIntLit(IntLit n);
    T visitDoubleLit(DoubleLit n);
    T visitBoolLit(BoolLit n);
    T visitCharLit(CharLit n);
    T visitStringLit(StringLit n);
    T visitId(Id n);
    T visitBin(Bin n);
    T visitUnary(Unary n);
    T visitCall(Call n);
}
