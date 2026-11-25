package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import src.ast.Program;
import src.visitor.AstPrinter;
import src.visitor.SourcePrinter;

public class ParserMain {
    public static void main(String[] args) {
        String path = null;
        boolean withPositions = false;
        for (String a : args) {
            if (a.equals("--pos")) withPositions = true;
            else path = a;
        }

        String source;
        if (path != null) {
            try {
                source = readFile(path);
            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo: " + e.getMessage());
                return;
            }
        } else {
            source =
                "int fatorial(int n) {\n" +
                "    int resultado = 1;\n" +
                "    while (n > 1) {\n" +
                "        resultado = resultado * n;\n" +
                "        n = n - 1;\n" +
                "    }\n" +
                "    return resultado;\n" +
                "}\n";
        }

        System.out.println("--- CÓDIGO FONTE ---");
        System.out.println(source);
        System.out.println("--------------------");

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        ParseResult result = new Parser(tokens).parse();

        System.out.println("Tokens: " + tokens.size()
                + " | erros léxicos: " + scanner.getErrors().size()
                + " | erros sintáticos: " + result.getErrors().size());
        for (SyntaxError e : result.getErrors()) {
            System.out.println(e);
        }

        if (!result.isValid()) {
            System.out.println("\n[REJEITADO] O programa contém erros sintáticos. Nenhuma árvore foi produzida.");
            return;
        }

        Program tree = result.getTree();
        System.out.println("\n[ACEITO] Programa sintaticamente válido.");
        System.out.println("\n--- ÁRVORE DE SINTAXE ABSTRATA ---");
        System.out.print(AstPrinter.print(tree, withPositions));
        System.out.println("\n--- CÓDIGO REIMPRESSO A PARTIR DA AST ---");
        System.out.print(SourcePrinter.print(tree));
    }

    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
