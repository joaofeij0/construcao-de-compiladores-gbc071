package src.ast;

public enum BasicType {
    INT("int"),
    DOUBLE("double"),
    BOOL("bool"),
    CHAR("char"),
    STRING("string");

    private final String keyword;

    BasicType(String keyword) {
        this.keyword = keyword;
    }

    public String keyword() { return keyword; }
}
