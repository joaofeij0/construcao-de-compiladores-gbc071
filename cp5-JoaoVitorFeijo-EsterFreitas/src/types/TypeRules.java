package src.types;

import src.ast.BinOp;
import src.ast.UnOp;
import src.symbols.Type;

import static src.symbols.Type.*;

public final class TypeRules {
    private TypeRules() {}

    public static boolean numerico(Type t) {
        return t == INT || t == DOUBLE || t == ERRO;
    }

    public static boolean argumentoAceito(Type veio, Type esperado) {
        if (veio == ERRO || esperado == ERRO) return true;
        return veio == esperado || (veio == INT && esperado == DOUBLE);
    }

    public static boolean mesmoTipo(Type veio, Type esperado) {
        return veio == ERRO || esperado == ERRO || veio == esperado;
    }

    public static Type binario(BinOp op, Type l, Type r) {
        switch (op) {
            case ADD: case SUB: case MUL: case DIV: case MOD:
                return aritmetica(op, l, r);
            case LT: case LE: case GT: case GE:
                return relacional(l, r);
            case EQ: case NE:
                return igualdade(l, r);
            default:
                return logico(l, r);
        }
    }

    public static Type unario(UnOp op, Type t) {
        return op == UnOp.NOT ? nao(t) : neg(t);
    }

    static Type aritmetica(BinOp op, Type l, Type r) {
        if (l == ERRO || r == ERRO) return ERRO;
        if (l == INT && r == INT) return INT;
        if (op != BinOp.MOD && numerico(l) && numerico(r)) return DOUBLE;
        return null;
    }

    static Type relacional(Type l, Type r) {
        if (l == ERRO || r == ERRO) return ERRO;
        if (numerico(l) && numerico(r)) return BOOL;
        return null;
    }

    static Type igualdade(Type l, Type r) {
        if (l == ERRO || r == ERRO) return ERRO;
        if (l == r && l != VOID) return BOOL;
        if (numerico(l) && numerico(r)) return BOOL;
        return null;
    }

    static Type logico(Type l, Type r) {
        if (l == ERRO || r == ERRO) return ERRO;
        if (l == BOOL && r == BOOL) return BOOL;
        return null;
    }

    static Type nao(Type t) {
        if (t == ERRO) return ERRO;
        return t == BOOL ? BOOL : null;
    }

    static Type neg(Type t) {
        if (t == ERRO) return ERRO;
        return numerico(t) ? t : null;
    }
}
