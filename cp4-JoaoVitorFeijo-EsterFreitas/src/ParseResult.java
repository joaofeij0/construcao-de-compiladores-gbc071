package src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import src.ast.Program;

public final class ParseResult {
    private final Program tree;
    private final List<SyntaxError> errors;

    ParseResult(Program tree, List<SyntaxError> errors) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.tree = this.errors.isEmpty() ? tree : null;
    }

    public Program getTree() { return tree; }

    public List<SyntaxError> getErrors() { return errors; }

    public boolean isValid() { return tree != null; }

    public boolean hasErrors() { return !errors.isEmpty(); }
}
