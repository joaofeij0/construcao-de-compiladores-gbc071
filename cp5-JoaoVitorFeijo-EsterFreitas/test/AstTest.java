package test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import src.ParseResult;
import src.Parser;
import src.Scanner;
import src.SyntaxError;
import src.Token;
import src.ast.Id;
import src.ast.Program;
import src.ast.VarDecl;
import src.visitor.AstPrinter;
import src.visitor.SourcePrinter;

public class AstTest {
    private static Path root = Paths.get(".");
    private static int passed = 0;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) root = Paths.get(args[0]);
        System.out.println("=== TESTES DO CP3: AST E VISITOR ===\n");

        System.out.println("-- 1. Programas válidos x árvores esperadas (escritas à mão)");
        testProgramaValido("p1_fatorial");
        testProgramaValido("p2_tipos");
        testProgramaValido("p3_mdc");

        System.out.println("\n-- 2. Precedência e associatividade");
        testPrecedencia();

        System.out.println("\n-- 3. A AST não é a árvore de derivação (parênteses redundantes)");
        testParentesesRedundantes();

        System.out.println("\n-- 4. Posição de cada nó");
        testPosicoes();

        System.out.println("\n-- 5. Ida e volta (round-trip)");
        testIdaEVolta();

        System.out.println("\n-- 6. Regressão do CP2 (programas inválidos)");
        testRegressaoCp2();

        System.out.println("\n-- 7. Nenhuma exceção escapa de parse()");
        testSemExcecoes();

        System.out.println("\n>>> TODOS OS " + passed + " TESTES PASSARAM COM SUCESSO! <<<");
    }

    private static void testProgramaValido(String nome) throws IOException {
        String src = read("test/programas/validos/" + nome + ".txt");
        ParseResult r = parseSemErroLexico(src, nome);
        assertTrue(r.isValid(), nome + ": deve ser aceito (erros: " + r.getErrors() + ")");
        assertTrue(r.getErrors().isEmpty(), nome + ": não deve ter erros sintáticos");
        assertSameText(read("test/esperado/" + nome + ".txt"), AstPrinter.print(r.getTree()),
                nome + ": árvore impressa x test/esperado/" + nome + ".txt");
        ok("Programa válido " + nome + ": árvore igual à esperada");
    }

    private static void testPrecedencia() throws IOException {
        String t1 = arvoreDe("int x = a + b * c;");
        String t2 = arvoreDe("int x = a - b - c;");
        String t3 = arvoreDe("int x = a - (b - c);");

        assertSameText(read("test/esperado/prec_a_mais_b_vezes_c.txt"), t1, "a + b * c");
        ok("a + b * c  =>  a + (b * c)  (precedências diferentes)");

        assertSameText(read("test/esperado/prec_a_menos_b_menos_c.txt"), t2, "a - b - c");
        ok("a - b - c  =>  (a - b) - c  (associatividade à esquerda)");

        assertSameText(read("test/esperado/prec_a_menos_parenteses_b_menos_c.txt"), t3, "a - (b - c)");
        ok("a - (b - c)  =>  a - (b - c)  (parênteses mudam a árvore)");

        assertTrue(!t1.equals(t2) && !t2.equals(t3) && !t1.equals(t3), "as três árvores devem ser diferentes");
        ok("As três árvores de precedência são distintas entre si");

        String t4 = arvoreDe("int x = a - b - c * d;");
        assertSameText(read("test/esperado/prec_exemplo_enunciado.txt"), t4, "a - b - c * d");
        ok("a - b - c * d  =>  (a - b) - (c * d)  (exemplo do enunciado)");
    }

    private static void testParentesesRedundantes() throws IOException {
        String comParenteses = arvoreDe("int x = (((a)));");
        String semParenteses = arvoreDe("int x = a;");
        assertSameText(read("test/esperado/parenteses_redundantes.txt"), comParenteses, "(((a)))");
        assertSameText(semParenteses, comParenteses, "(((a))) x a");

        Program p = parseValido("int x = (((a)));");
        VarDecl decl = (VarDecl) p.getDecls().get(0);
        assertTrue(decl.getInit().getClass() == Id.class, "o filho de VarDecl deve ser um Id");
        ok("(((a))) produz o mesmo nó Id que a, sem nó de parênteses");

        assertSameText(arvoreDe("int x = (a + b) * c;"), arvoreDe("int x = ((((a + b))) * ((c)));"),
                "((((a + b))) * ((c))) x (a + b) * c");
        ok("((((a + b))) * ((c))) tem a mesma árvore que (a + b) * c");
    }

    private static void testPosicoes() throws IOException {
        Program p = parseValido("int f(int a) {\n    return a + 1;\n}\n");
        assertSameText(read("test/esperado/posicoes.txt"), AstPrinter.print(p, true), "posições");
        ok("Cada nó guarda linha e coluna do primeiro token que o originou");
    }

    private static void testIdaEVolta() throws IOException {
        List<String> programas = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        for (Path f : listar("test/programas/validos")) {
            programas.add(read("test/programas/validos/" + f.getFileName()));
            nomes.add(f.getFileName().toString());
        }
        String[] extras = {
            "int x = a + b * c;", "int x = a - b - c;", "int x = a - (b - c);",
            "int x = a - b - c * d;", "int x = (((a)));", ""
        };
        for (String e : extras) {
            programas.add(e);
            nomes.add(e.isEmpty() ? "(programa vazio)" : e);
        }

        for (int i = 0; i < programas.size(); i++) {
            String nome = nomes.get(i);
            Program arvore1 = parseValido(programas.get(i));
            String texto1 = SourcePrinter.print(arvore1);
            Program arvore2 = parseValido(texto1);
            String texto2 = SourcePrinter.print(arvore2);
            assertSameText(texto1, texto2, nome + ": segundo texto reimpresso deve ser igual ao primeiro");
            assertSameText(AstPrinter.print(arvore1), AstPrinter.print(arvore2),
                    nome + ": as duas árvores devem ter a mesma impressão");
            ok("Ida e volta: " + nome);
        }

        Random rnd = new Random(42L);
        for (int n = 0; n < 500; n++) {
            String src = new GeradorDeProgramas(rnd).programa();
            Program arvore1 = parseValido(src);
            String texto1 = SourcePrinter.print(arvore1);
            Program arvore2 = parseValido(texto1);
            assertSameText(texto1, SourcePrinter.print(arvore2), "aleatório #" + n + ":\n" + src);
            assertSameText(AstPrinter.print(arvore1), AstPrinter.print(arvore2), "aleatório #" + n + ":\n" + src);
        }
        ok("Ida e volta: 500 programas válidos gerados aleatoriamente");
    }

    private static final class GeradorDeProgramas {
        private static final String[] TIPOS = {"int", "double", "bool", "char", "string"};
        private static final String[] BIN = {"||", "&&", "==", "!=", "<", "<=", ">", ">=", "+", "-", "*", "/", "%"};
        private static final String[] IDS = {"a", "b", "x", "total", "_tmp", "v1"};
        private static final String[] ATOMOS = {
            "0", "7", "42", "3.14", "100.0", "007", "true", "false",
            "'a'", "'\\n'", "'\\''", "'\\\\'", "'\"'", "\"\"", "\"ola\"", "\"com \\\"aspas\\\"\\n\"", "\"tab\\t\\\\\""
        };
        private final Random rnd;

        GeradorDeProgramas(Random rnd) { this.rnd = rnd; }

        private String pick(String[] xs) { return xs[rnd.nextInt(xs.length)]; }

        String programa() {
            StringBuilder sb = new StringBuilder();
            int n = rnd.nextInt(4);
            for (int i = 0; i < n; i++) {
                if (rnd.nextBoolean()) sb.append(pick(TIPOS)).append(' ').append(pick(IDS)).append(" = ").append(expr(2)).append(";\n");
                else sb.append(pick(TIPOS)).append(' ').append(pick(IDS)).append(";\n");
            }
            int m = 1 + rnd.nextInt(3);
            for (int i = 0; i < m; i++) {
                sb.append(rnd.nextBoolean() ? "void" : pick(TIPOS)).append(" r").append(i).append('(');
                int np = rnd.nextInt(3);
                for (int p = 0; p < np; p++) sb.append(p > 0 ? ", " : "").append(pick(TIPOS)).append(" p").append(p);
                sb.append(") ").append(bloco(3)).append('\n');
            }
            return sb.toString();
        }

        private String bloco(int prof) {
            StringBuilder sb = new StringBuilder("{\n");
            int n = prof <= 0 ? rnd.nextInt(2) : rnd.nextInt(4);
            for (int i = 0; i < n; i++) sb.append(comando(prof - 1)).append('\n');
            return sb.append('}').toString();
        }

        private String comando(int prof) {
            int k = rnd.nextInt(prof <= 0 ? 4 : 9);
            switch (k) {
                case 0: return pick(TIPOS) + " " + pick(IDS) + (rnd.nextBoolean() ? " = " + expr(3) : "") + ";";
                case 1: return pick(IDS) + " = " + expr(3) + ";";
                case 2: return chamada(2) + ";";
                case 3: return "return" + (rnd.nextBoolean() ? " " + expr(3) : "") + ";";
                case 4: return se(prof);
                case 5: return "while (" + expr(3) + ") " + bloco(prof);
                case 6: {
                    String init = rnd.nextInt(3) == 0 ? "" : rnd.nextBoolean()
                            ? pick(TIPOS) + " " + pick(IDS) + (rnd.nextBoolean() ? " = " + expr(2) : "")
                            : pick(IDS) + " = " + expr(2);
                    String cond = rnd.nextInt(3) == 0 ? "" : expr(2);
                    String step = rnd.nextInt(3) == 0 ? "" : pick(IDS) + " = " + expr(2);
                    return "for (" + init + "; " + cond + "; " + step + ") " + bloco(prof);
                }
                case 7: return bloco(prof);
                default: return se(prof);
            }
        }

        private String se(int prof) {
            String s = "if (" + expr(3) + ") " + bloco(prof);
            int e = rnd.nextInt(3);
            if (e == 1) s += " else " + bloco(prof);
            if (e == 2) s += " else " + se(prof - 1);
            return s;
        }

        private String chamada(int prof) {
            StringBuilder sb = new StringBuilder(pick(IDS)).append('(');
            int n = rnd.nextInt(3);
            for (int i = 0; i < n; i++) sb.append(i > 0 ? ", " : "").append(expr(prof));
            return sb.append(')').toString();
        }

        private String expr(int prof) {
            if (prof <= 0) return rnd.nextBoolean() ? pick(IDS) : pick(ATOMOS);
            switch (rnd.nextInt(6)) {
                case 0: return pick(IDS);
                case 1: return pick(ATOMOS);
                case 2: return (rnd.nextBoolean() ? "!" : "-") + expr(prof - 1);
                case 3: return "(" + expr(prof - 1) + ")";
                case 4: return chamada(prof - 1);
                default: return expr(prof - 1) + " " + pick(BIN) + " " + expr(prof - 1);
            }
        }
    }

    private static void testRegressaoCp2() throws IOException {
        int multiplos = 0;
        for (Path f : listar("test/programas/invalidos")) {
            String nome = f.getFileName().toString();
            String src = read("test/programas/invalidos/" + nome);
            ParseResult r = parseSemExcecao(src, nome);

            assertTrue(!r.isValid(), nome + ": deve ser rejeitado");
            assertTrue(r.getTree() == null, nome + ": a árvore não pode ser apresentada como válida");

            StringBuilder obtido = new StringBuilder("REJEITADO\n");
            for (SyntaxError e : r.getErrors()) obtido.append(e).append('\n');
            assertSameText(read("test/esperado/cp2/" + nome), obtido.toString(),
                    nome + ": mesmos erros que o parser do CP2");
            if (r.getErrors().size() > 1) multiplos++;
            ok("Regressão " + nome + ": rejeitado com " + r.getErrors().size() + " erro(s), iguais aos do CP2");
        }
        assertTrue(multiplos > 0, "deve haver ao menos um caso com múltiplos erros");
        ok("Casos com múltiplos erros reportam todos os erros em uma única rodada (" + multiplos + " programas)");
    }

    private static void testSemExcecoes() throws IOException {
        int casos = 0;
        for (Path f : listar("test/programas/validos")) {
            String src = read("test/programas/validos/" + f.getFileName());
            for (int i = 0; i <= src.length(); i++) {
                parseSemExcecao(src.substring(0, i), f.getFileName() + " cortado em " + i);
                casos++;
            }
        }
        ok("Todos os prefixos dos programas válidos (" + casos + " entradas) sem exceção");

        String[] lexemas = {
            "int", "double", "bool", "char", "string", "void", "if", "else", "while", "for",
            "return", "true", "false", "x", "y", "f", "1", "2.5", "\"s\"", "'c'", "12.",
            "=", "+", "-", "*", "/", "%", "==", "!=", "<", "<=", ">", ">=", "&&", "||", "!",
            "(", ")", "{", "}", ";", ",", "[", "]"
        };
        Random rnd = new Random(20261030L);
        int aceitos = 0;
        for (int n = 0; n < 3000; n++) {
            StringBuilder sb = new StringBuilder();
            int tam = 1 + rnd.nextInt(40);
            for (int k = 0; k < tam; k++) sb.append(lexemas[rnd.nextInt(lexemas.length)]).append(' ');
            ParseResult r = parseSemExcecao(sb.toString(), "aleatório #" + n);
            assertTrue(r.isValid() == r.getErrors().isEmpty(), "árvore válida se e só se não há erros");
            if (r.isValid()) aceitos++;
        }
        ok("3000 entradas aleatórias sem exceção (" + aceitos + " aceitas)");
    }

    private static ParseResult parse(String src) {
        Scanner scanner = new Scanner(src);
        List<Token> tokens = scanner.scanTokens();
        return new Parser(tokens).parse();
    }

    private static ParseResult parseSemExcecao(String src, String nome) {
        try {
            return parse(src);
        } catch (RuntimeException | StackOverflowError e) {
            throw new AssertionError("FALHA: exceção escapou de parse() em " + nome + ": " + e, e);
        }
    }

    private static ParseResult parseSemErroLexico(String src, String nome) {
        Scanner scanner = new Scanner(src);
        List<Token> tokens = scanner.scanTokens();
        assertTrue(!scanner.hasErrors(), nome + ": não deve ter erros léxicos");
        return new Parser(tokens).parse();
    }

    private static Program parseValido(String src) {
        ParseResult r = parse(src);
        assertTrue(r.isValid(), "programa deveria ser aceito: " + src + " erros: " + r.getErrors());
        return r.getTree();
    }

    private static String arvoreDe(String src) {
        return AstPrinter.print(parseValido(src));
    }

    private static String read(String relativePath) throws IOException {
        String text = new String(Files.readAllBytes(root.resolve(relativePath)), StandardCharsets.UTF_8);
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static List<Path> listar(String dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root.resolve(dir), "*.txt")) {
            for (Path p : ds) files.add(p);
        }
        Collections.sort(files);
        return files;
    }

    private static void assertSameText(String esperado, String obtido, String msg) {
        String e = esperado.replace("\r", "").stripTrailing();
        String o = obtido.replace("\r", "").stripTrailing();
        if (e.equals(o)) return;
        throw new AssertionError("FALHA: " + msg + "\n--- esperado ---\n" + e + "\n--- obtido ---\n" + o + "\n---");
    }

    private static void assertTrue(boolean condicao, String msg) {
        if (!condicao) throw new AssertionError("FALHA: " + msg);
    }

    private static void ok(String descricao) {
        passed++;
        System.out.println("  [OK] " + descricao);
    }
}
