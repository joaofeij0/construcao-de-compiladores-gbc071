package src.types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import src.LexicalError;
import src.ParseResult;
import src.Parser;
import src.Scanner;
import src.SyntaxError;
import src.Token;
import src.ast.Program;
import src.symbols.SemanticError;
import src.visitor.IdentificationVisitor;

public final class TypeReport {
    private final boolean analyzed;
    private final List<String> frontEndErrors;
    private final List<SemanticError> messages;
    private final Program tree;

    private TypeReport(boolean analyzed, List<String> frontEndErrors, List<SemanticError> messages, Program tree) {
        this.analyzed = analyzed;
        this.frontEndErrors = frontEndErrors;
        this.messages = messages;
        this.tree = tree;
    }

    public static TypeReport analyze(String source) {
        Scanner scanner = new Scanner(source == null ? "" : source);
        List<Token> tokens = scanner.scanTokens();
        ParseResult result = new Parser(tokens).parse();

        if (scanner.hasErrors() || !result.isValid()) {
            List<String> fe = new ArrayList<>();
            for (LexicalError e : scanner.getErrors()) fe.add(e.toString());
            for (SyntaxError e : result.getErrors()) fe.add(e.toString());
            return new TypeReport(false, fe, Collections.emptyList(), null);
        }

        Program tree = result.getTree();
        IdentificationVisitor ident = new IdentificationVisitor();
        ident.run(tree);
        TypeChecker checker = new TypeChecker();
        checker.run(tree);

        List<SemanticError> all = new ArrayList<>(ident.getErrors());
        all.addAll(checker.getMessages());
        all.sort(Comparator.comparingInt(SemanticError::getLine).thenComparingInt(SemanticError::getColumn));
        return new TypeReport(true, Collections.emptyList(), Collections.unmodifiableList(all), tree);
    }

    public static String run(String source) {
        return analyze(source).text();
    }

    public boolean wasAnalyzed() { return analyzed; }

    public List<SemanticError> getMessages() { return messages; }

    public boolean hasErrors() {
        if (!analyzed) return true;
        for (SemanticError m : messages) if (!m.isWarning()) return true;
        return false;
    }

    public boolean canGenerateCode() { return !hasErrors(); }

    public Program getTree() { return canGenerateCode() ? tree : null; }

    public String text() {
        StringBuilder out = new StringBuilder();
        if (!analyzed) {
            for (String e : frontEndErrors) out.append(e).append('\n');
            out.append("verificação de tipos não executada: não há AST válida\n");
            return out.toString();
        }
        if (messages.isEmpty()) return "nenhum erro\n";
        for (SemanticError m : messages) out.append(m).append('\n');
        return out.toString();
    }
}
