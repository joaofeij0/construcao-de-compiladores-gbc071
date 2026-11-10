package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Executa scanner (CP1, inalterado) + parser (CP2) sobre um arquivo ou sobre um programa de demonstração. */
public class ParserMain {
    public static void main(String[] args) {
        String source;
        if (args.length > 0) {
            try {
                source = readFile(args[0]);
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
        Parser parser = new Parser(tokens);
        boolean accepted = parser.parse();

        System.out.println("Tokens: " + tokens.size()
                + " | erros léxicos: " + scanner.getErrors().size()
                + " | erros sintáticos: " + parser.getErrors().size());
        for (SyntaxError e : parser.getErrors()) {
            System.out.println(e);
        }
        System.out.println(accepted ? "\n[ACEITO] Programa sintaticamente válido."
                                    : "\n[REJEITADO] O programa contém erros sintáticos.");
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
