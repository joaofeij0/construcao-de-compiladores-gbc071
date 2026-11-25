package src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import src.types.TypeReport;

public class TypeMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("uso: java -cp out src.TypeMain <arquivo>");
            return;
        }
        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }
        TypeReport report = TypeReport.analyze(source);
        System.out.print(report.text());
        System.out.println(report.canGenerateCode()
                ? "[ACEITO] programa bem tipado: pode seguir para a geração de código"
                : "[RECUSADO] programa com erro: a geração de código não é executada");
    }
}
