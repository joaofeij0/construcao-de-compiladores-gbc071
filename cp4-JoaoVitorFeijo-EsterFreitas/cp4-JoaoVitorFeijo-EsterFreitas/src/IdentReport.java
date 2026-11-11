package src;

import java.util.List;

import src.ast.Program;
import src.symbols.SemanticError;
import src.symbols.StackSymbolTable;
import src.symbols.SymbolTable;
import src.symbols.TracingSymbolTable;
import src.visitor.IdentificationVisitor;
import src.visitor.UseCollector;

public final class IdentReport {
    private IdentReport() {}

    public static String run(String source) {
        return run(source, new StackSymbolTable());
    }

    public static String run(String source, SymbolTable base) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        ParseResult result = new Parser(tokens).parse();

        StringBuilder out = new StringBuilder();
        if (scanner.hasErrors() || !result.isValid()) {
            out.append("== erros léxicos/sintáticos ==\n");
            for (LexicalError e : scanner.getErrors()) out.append(e).append('\n');
            for (SyntaxError e : result.getErrors()) out.append(e).append('\n');
            out.append("passada de identificação não executada: não há AST válida\n");
            return out.toString();
        }

        Program tree = result.getTree();
        TracingSymbolTable table = new TracingSymbolTable(base);
        IdentificationVisitor pass = new IdentificationVisitor(table);
        pass.run(tree);

        out.append("== ambiente ==\n").append(table.getLog());

        out.append("== usos ==\n");
        List<UseCollector.Use> uses = UseCollector.collect(tree);
        if (uses.isEmpty()) out.append("(nenhum)\n");
        for (UseCollector.Use u : uses) out.append(u).append('\n');

        out.append("== erros ==\n");
        List<SemanticError> errors = pass.getErrors();
        if (errors.isEmpty()) out.append("(nenhum)\n");
        for (SemanticError e : errors) out.append(e).append('\n');
        return out.toString();
    }
}
