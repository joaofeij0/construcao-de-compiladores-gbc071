package test;

import src.Parser;
import src.Scanner;
import src.SyntaxError;
import src.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ParserTest {

    private static class Result {
        final boolean accepted;
        final List<SyntaxError> errors;
        Result(boolean accepted, List<SyntaxError> errors) { this.accepted = accepted; this.errors = errors; }
    }

    public static void main(String[] args) {
        System.out.println("=== EXECUTANDO TESTES DO PARSER ===");

        // Programas válidos
        testValidoFatorial();
        testValidoVoidForElseIfGlobais();
        testValidoExpressoes();
        testValidoVazioEConstrucoesComEpsilon();
        testDanglingElse();

        // Programas inválidos (um erro sintático diferente em cada um)
        testErroFaltaPontoEVirgula();
        testErroFaltaParentese();
        testErroBlocoNaoFechado();
        testErroOperandoFaltando();
        testErroElseSolto();
        testErroIfSemChaves();
        testEpsilonRobusto();
        testTokenErroDoScanner();

        // Recuperação em modo pânico: vários erros em uma única análise
        testMultiplosErros();

        // Robustez: nunca travar
        testNaoTravaFuzz();
        testEntradaGrande();

        System.out.println("\n>>> TODOS OS TESTES DO PARSER PASSARAM COM SUCESSO! <<<");
    }

    // ----------------------------- VÁLIDOS -----------------------------

    private static void testValidoFatorial() {
        System.out.print("Teste 1 (válido): função com parâmetro, while, if e return com valor... ");
        Result r = analyze(
            "int fatorial(int n) {\n" +
            "    int resultado = 1;\n" +
            "    double taxa = 1.05;\n" +
            "    string status = \"Calculando...\";\n" +
            "    if (n <= 1) {\n" +
            "        return 1;\n" +
            "    }\n" +
            "    while (n > 1) {\n" +
            "        resultado = resultado * n;\n" +
            "        n = n - 1;\n" +
            "    }\n" +
            "    return resultado;\n" +
            "}\n");
        assertTrue(r.accepted, "Programa deveria ser aceito: " + r.errors);
        assertEquals(0, r.errors.size(), "Nenhum erro sintático");
        System.out.println("OK");
    }

    private static void testValidoVoidForElseIfGlobais() {
        System.out.print("Teste 2 (válido): globais, void, for, else if/else, chamada como comando... ");
        Result r = analyze(
            "int total = 0;\n" +
            "bool ativo = true;\n" +
            "char letra;\n" +
            "void imprime(string s, char c) {\n" +
            "    return;\n" +
            "}\n" +
            "void principal() {\n" +
            "    for (int i = 0; i < 10; i = i + 1) {\n" +
            "        if (i % 2 == 0) {\n" +
            "            total = total + i;\n" +
            "        } else if (i == 7) {\n" +
            "            imprime(\"sete\", 'x');\n" +
            "        } else {\n" +
            "            ativo = !ativo;\n" +
            "        }\n" +
            "    }\n" +
            "}\n");
        assertTrue(r.accepted, "Programa deveria ser aceito: " + r.errors);
        System.out.println("OK");
    }

    private static void testValidoExpressoes() {
        System.out.print("Teste 3 (válido): expressões com todos os operadores, unários, parênteses e chamadas... ");
        Result r = analyze(
            "double calcula(double x, int y) {\n" +
            "    double z = -(x + 1.5) * 2 / y % 3;\n" +
            "    bool b = !(z >= 1.0) && z != 0.0 || y <= 3 && x > 2 && y < 9 && x == 1;\n" +
            "    return calcula(z, outra(1, 2 + 3));\n" +
            "}\n");
        assertTrue(r.accepted, "Programa deveria ser aceito: " + r.errors);
        System.out.println("OK");
    }

    private static void testValidoVazioEConstrucoesComEpsilon() {
        System.out.print("Teste 4 (válido): programa vazio, função sem parâmetros, bloco vazio, return; ... ");
        assertTrue(analyze("").accepted, "Programa vazio (DeclList → ε)");
        assertTrue(analyze("// só um comentário\n").accepted, "Só comentário");
        assertTrue(analyze("void f() { }").accepted, "Bloco vazio (StmtList → ε) e params vazios");
        assertTrue(analyze("void f() { return; }").accepted, "return sem valor (ReturnVal → ε)");
        assertTrue(analyze("void f() { g(); }").accepted, "Chamada sem argumentos (ArgListOpt → ε)");
        assertTrue(analyze("void f() { for (;;) { } }").accepted, "for com todas as partes vazias");
        assertTrue(analyze("int f() { int x; return x; }").accepted, "Declaração sem inicializador (InitOpt → ε)");
        System.out.println("OK");
    }

    private static void testDanglingElse() {
        System.out.print("Teste 5 (válido): else associado sem ambiguidade (chaves obrigatórias)... ");
        assertTrue(analyze("void f() { if (a) { if (b) { x = 1; } } else { x = 2; } }").accepted, "else do if externo");
        assertTrue(analyze("void f() { if (a) { if (b) { x = 1; } else { x = 2; } } }").accepted, "else do if interno");
        System.out.println("OK");
    }

    // ----------------------------- INVÁLIDOS -----------------------------

    private static void testErroFaltaPontoEVirgula() {
        System.out.print("Teste 6 (erro): falta ';' ao fim de uma declaração... ");
        Result r = analyze("void f() {\n    int x = 5\n    int y = 6;\n}\n");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertEquals(1, r.errors.size(), "Exatamente 1 erro (a declaração seguinte não é perdida)");
        assertEquals(3, r.errors.get(0).getLine(), "Erro é detectado no 'int' da linha seguinte");
        assertTrue(r.errors.get(0).getMessage().contains("encontrado 'int'"), "Mensagem cita o token encontrado");
        System.out.println("OK");
    }

    private static void testErroFaltaParentese() {
        System.out.print("Teste 7 (erro): falta ')' na condição do if... ");
        Result r = analyze("void f() {\n    if (x > 1 {\n        x = 1;\n    }\n}\n");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertEquals(1, r.errors.size(), "1 erro: o corpo do if é recuperado pelo '{'");
        assertEquals(2, r.errors.get(0).getLine(), "Linha do erro");
        assertEquals(15, r.errors.get(0).getColumn(), "Coluna do '{' inesperado");
        System.out.println("OK");
    }

    private static void testErroBlocoNaoFechado() {
        System.out.print("Teste 8 (erro): bloco '{' sem o '}' correspondente (EOF)... ");
        Result r = analyze("void f() {\n    int x = 1;\n");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertEquals(1, r.errors.size(), "Exatamente 1 erro");
        assertTrue(r.errors.get(0).getMessage().contains("'}'"), "Menciona o '}' esperado");
        assertTrue(r.errors.get(0).getMessage().contains("fim do arquivo"), "Encontrou o fim do arquivo");
        System.out.println("OK");
    }

    private static void testErroOperandoFaltando() {
        System.out.print("Teste 9 (erro): operando faltando após '+'... ");
        Result r = analyze("void f() { x = 5 + ; }");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertEquals(1, r.errors.size(), "Exatamente 1 erro");
        assertTrue(r.errors.get(0).getMessage().contains("esperado expressão"), "Esperava uma expressão");
        assertEquals(20, r.errors.get(0).getColumn(), "Coluna do ';'");
        System.out.println("OK");
    }

    private static void testErroElseSolto() {
        System.out.print("Teste 10 (erro): 'else' sem 'if'... ");
        Result r = analyze("void f() { else { x = 1; } }");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertEquals(1, r.errors.size(), "Exatamente 1 erro");
        assertTrue(r.errors.get(0).getMessage().contains("encontrado 'else'"), "O token ofensor é o else");
        System.out.println("OK");
    }

    private static void testErroIfSemChaves() {
        System.out.print("Teste 11 (erro): corpo do if sem '{' '}' (chaves são obrigatórias)... ");
        Result r = analyze("void f() { if (x) return 1; }");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertEquals(1, r.errors.size(), "Exatamente 1 erro: o return é aproveitado como comando");
        assertTrue(r.errors.get(0).getMessage().contains("esperado '{'"), "Esperava '{'");
        System.out.println("OK");
    }

    private static void testEpsilonRobusto() {
        System.out.print("Teste 12 (erro): ε só é aceito se o lookahead está em FOLLOW (argumento sem vírgula)... ");
        Result r = analyze("void f() { g(1 2); }");
        mostra(r);
        assertFalse(r.accepted, "Se ε fosse aceito 'às cegas', o erro só apareceria depois, e pior localizado");
        assertEquals(1, r.errors.size(), "Exatamente 1 erro");
        assertEquals(1, r.errors.get(0).getLine(), "Linha");
        assertEquals(16, r.errors.get(0).getColumn(), "Coluna do '2'");
        System.out.println("OK");
    }

    private static void testTokenErroDoScanner() {
        System.out.print("Teste 13 (erro): token ERROR do scanner (12.) não derruba o parser... ");
        Result r = analyze("void f() { x = 12.; }");
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        assertTrue(r.errors.size() >= 1, "Reporta erro sintático");
        System.out.println("OK");
    }

    // ----------------------- MÚLTIPLOS ERROS / PÂNICO -----------------------

    private static void testMultiplosErros() {
        System.out.print("Teste 14 (pânico): 5 erros em um único programa, uma única rodada... ");
        String src =
            "int a = ;\n" +                       // linha 1: expressão faltando
            "void f(int x) {\n" +
            "    int y = 5\n" +                   // linha 3: falta ';'  (detectado no 'if' da linha 4)
            "    if (x > 1 {\n" +                 // linha 4: falta ')'
            "        y = y + ;\n" +               // linha 5: operando faltando
            "    }\n" +
            "    return y;\n" +                   // linha 7: válido (não pode ser perdido)
            "}\n" +
            "int g() { return 1 }\n";             // linha 9: falta ';'
        Result r = analyze(src);
        mostra(r);
        assertFalse(r.accepted, "Deve rejeitar");
        int[] linhasEsperadas = {1, 4, 4, 5, 9};
        assertEquals(linhasEsperadas.length, r.errors.size(), "Total de erros reportados");
        for (int i = 0; i < linhasEsperadas.length; i++) {
            assertEquals(linhasEsperadas[i], r.errors.get(i).getLine(), "Linha do erro #" + (i + 1));
        }
        System.out.println("OK");
    }

    // ----------------------------- ROBUSTEZ -----------------------------

    private static void testNaoTravaFuzz() {
        System.out.print("Teste 15 (robustez): 30.000 entradas aleatórias/mutiladas, nenhuma trava... ");
        final boolean[] terminou = {false};
        Thread t = new Thread(() -> {
            String[] v = {"int","double","bool","char","string","void","if","else","while","for","return","true","false",
                "x","f","1","2.5","\"s\"","'c'","=","==","!=","<","<=",">",">=","+","-","*","/","%","&&","||","!",
                "(",")","{","}",";",",","[","]"};
            Random rnd = new Random(42);
            for (int i = 0; i < 15000; i++) {
                StringBuilder sb = new StringBuilder();
                int len = rnd.nextInt(60);
                for (int k = 0; k < len; k++) sb.append(v[rnd.nextInt(v.length)]).append(' ');
                new Parser(new Scanner(sb.toString()).scanTokens()).parse();
            }
            String base = "int g = 1; int fat(int n) { int r = 1; while (n > 1) { r = r * n; n = n - 1; } "
                + "if (r >= 2 && !(n == 0)) { return r; } else { return f(1, 2); } } "
                + "void p() { for (int i = 0; i < 3; i = i + 1) { g = g + i; } return; }";
            List<Token> base0 = new Scanner(base).scanTokens();
            for (int i = 0; i < 15000; i++) {
                List<Token> ts = new ArrayList<>(base0);
                for (int k = 0, m = 1 + rnd.nextInt(3); k < m; k++) {
                    int idx = rnd.nextInt(ts.size() - 1);
                    int op = rnd.nextInt(3);
                    if (op == 0) ts.remove(idx);
                    else if (op == 1) ts.add(idx, ts.get(rnd.nextInt(ts.size())));
                    else Collections.swap(ts, idx, rnd.nextInt(ts.size() - 1));
                }
                new Parser(ts).parse();
            }
            terminou[0] = true;
        });
        t.setDaemon(true);
        t.start();
        join(t, 60000);
        assertTrue(terminou[0], "O parser não pode entrar em loop infinito");
        System.out.println("OK");
    }

    private static void testEntradaGrande() {
        System.out.print("Teste 16 (robustez): programa grande (3000 comandos, expressão de 3000 termos)... ");
        StringBuilder s = new StringBuilder("void f() {\n");
        for (int i = 0; i < 3000; i++) s.append("    x = 1;\n");
        s.append("    y = 1");
        for (int i = 0; i < 3000; i++) s.append(" + 1");
        s.append(";\n}\n");
        assertTrue(analyze(s.toString()).accepted, "Programa grande válido deve ser aceito");
        System.out.println("OK");
    }

    // ----------------------------- infraestrutura -----------------------------

    /** Roda scanner + parser numa thread com timeout (um loop infinito vira falha, não trava o teste). */
    private static Result analyze(String src) {
        final Result[] out = new Result[1];
        Thread t = new Thread(() -> {
            Scanner sc = new Scanner(src);
            List<Token> toks = sc.scanTokens();
            Parser p = new Parser(toks);
            boolean ok = p.parse();
            out[0] = new Result(ok, p.getErrors());
        });
        t.setDaemon(true);
        t.start();
        join(t, 10000);
        if (out[0] == null) throw new AssertionError("FALHA: parser não terminou (possível loop infinito)");
        return out[0];
    }

    private static void join(Thread t, long ms) {
        try { t.join(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void mostra(Result r) {
        System.out.println();
        for (SyntaxError e : r.errors) System.out.println("      " + e);
        System.out.print("      -> ");
    }

    private static void assertEquals(Object esperado, Object obtido, String msg) {
        if (esperado == null && obtido == null) return;
        if (esperado != null && esperado.equals(obtido)) return;
        throw new AssertionError("FALHA: " + msg + " (Esperado: " + esperado + ", Obtido: " + obtido + ")");
    }

    private static void assertTrue(boolean condicao, String msg) {
        if (!condicao) throw new AssertionError("FALHA: " + msg);
    }

    private static void assertFalse(boolean condicao, String msg) {
        if (condicao) throw new AssertionError("FALHA: " + msg);
    }
}
