package src.ast;

import src.visitor.Visitor;

public interface Decl {
    int getLine();
    int getColumn();
    <T> T accept(Visitor<T> v);
}
