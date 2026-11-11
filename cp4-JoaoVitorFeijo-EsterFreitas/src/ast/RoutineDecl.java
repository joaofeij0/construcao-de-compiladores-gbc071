package src.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class RoutineDecl extends Node implements Decl {
    private final String name;
    private final List<Param> params;
    private final Block body;

    protected RoutineDecl(String name, List<Param> params, Block body, int line, int column) {
        super(line, column);
        this.name = name;
        this.params = Collections.unmodifiableList(new ArrayList<>(params));
        this.body = body;
    }

    public String getName() { return name; }
    public List<Param> getParams() { return params; }
    public Block getBody() { return body; }
}
