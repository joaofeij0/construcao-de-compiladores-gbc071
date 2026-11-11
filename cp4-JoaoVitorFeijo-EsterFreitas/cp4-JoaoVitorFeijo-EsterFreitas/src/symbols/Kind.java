package src.symbols;

public enum Kind {
    VAR("var"),
    PARAM("param"),
    PROC("proc"),
    FUNC("func");

    private final String label;

    Kind(String label) { this.label = label; }

    public String label() { return label; }
}
