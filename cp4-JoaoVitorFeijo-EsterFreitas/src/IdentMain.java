package src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IdentMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("uso: java -cp . src.IdentMain <arquivo>");
            return;
        }
        String source;
        try {
            source = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo: " + e.getMessage());
            return;
        }
        System.out.print(IdentReport.run(source));
    }
}
