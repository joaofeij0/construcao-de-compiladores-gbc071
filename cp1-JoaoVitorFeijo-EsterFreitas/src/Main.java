package src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println(" COMPILADORES - Analisador Léxico ");
        System.out.println("==================================================================");

        String sourceCode;

        if (args.length > 0) {
            String filePath = args[0];
            System.out.println("Lendo arquivo fonte: " + filePath);
            try {
                sourceCode = readFile(filePath);
            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("Nenhum arquivo fornecido. Executando código de demonstração realista:");
            sourceCode =
                "// Exemplo realista de código da linguagem\n" +
                "int calculaFatorial(int n) {\n" +
                "    int resultado = 1;\n" +
                "    double taxa = 1.05;\n" +
                "    bool ativo = true;\n" +
                "    string status = \"Calculando...\";\n" +
                "\n" +
                "    if (n <= 1) {\n" +
                "        return 1;\n" +
                "    }\n" +
                "\n" +
                "    while (n > 1) {\n" +
                "        resultado = resultado * n;\n" +
                "        n = n - 1;\n" +
                "    }\n" +
                "    /* Retorno final do calculo */\n" +
                "    return resultado;\n" +
                "}\n";
        }

        System.out.println("\n--- CÓDIGO FONTE ---");
        System.out.println(sourceCode);
        System.out.println("--------------------\n");

        Scanner scanner = new Scanner(sourceCode);
        List<Token> tokens = scanner.scanTokens();

        System.out.println("--- TOKENS GERADOS (" + tokens.size() + ") ---");
        System.out.printf("%-4s | %-16s | %-10s | %-25s%n", "#", "TIPO", "LINHA:COL", "LEXEMA");
        System.out.println("------------------------------------------------------------------");

        int index = 1;
        for (Token t : tokens) {
            System.out.printf("%-4d | %-16s | %-10s | %-25s%n",
                    index++,
                    t.getType(),
                    t.getLine() + ":" + t.getColumn(),
                    escapeLexeme(t.getLexeme()));
        }

        System.out.println("------------------------------------------------------------------");

        if (scanner.hasErrors()) {
            System.out.println("\n--- RELATÓRIO DE ERROS LÉXICOS (" + scanner.getErrors().size() + ") ---");
            for (LexicalError err : scanner.getErrors()) {
                System.out.println(err);
            }
        } else {
            System.out.println("\n[SUCESSO] Análise léxica concluída sem erros!");
        }
    }

    private static String readFile(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static String escapeLexeme(String lexeme) {
        return lexeme.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
