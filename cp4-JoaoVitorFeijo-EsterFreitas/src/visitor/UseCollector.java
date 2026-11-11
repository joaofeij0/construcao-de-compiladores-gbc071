package src.visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import src.ast.Assign;
import src.ast.Call;
import src.ast.Id;
import src.ast.Node;
import src.ast.Program;
import src.symbols.Symbol;

public final class UseCollector extends DepthFirstVisitor {
    public static final class Use {
        public final Node node;
        public final String name;
        public final Symbol decl;

        Use(Node node, String name, Symbol decl) {
            this.node = node;
            this.name = name;
            this.decl = decl;
        }

        @Override
        public String toString() {
            return "linha " + node.getLine() + ": " + name + " -> "
                    + (decl == null ? "não declarado" : decl.toString());
        }
    }

    private final List<Use> uses = new ArrayList<>();

    public static List<Use> collect(Program tree) {
        UseCollector c = new UseCollector();
        if (tree != null) tree.accept(c);
        return Collections.unmodifiableList(c.uses);
    }

    @Override
    public Void visitId(Id n) {
        uses.add(new Use(n, n.getName(), n.getDecl()));
        return null;
    }

    @Override
    public Void visitAssign(Assign n) {
        uses.add(new Use(n, n.getTarget(), n.getDecl()));
        return super.visitAssign(n);
    }

    @Override
    public Void visitCall(Call n) {
        uses.add(new Use(n, n.getName(), n.getDecl()));
        return super.visitCall(n);
    }
}
