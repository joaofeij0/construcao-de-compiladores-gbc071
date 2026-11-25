package test;

import src.Scanner;
import src.Token;
import src.TokenType;

import java.util.List;

public class ScannerTest {

    public static void main(String[] args) {
        System.out.println("=== EXECUTANDO TESTES ===");

        testCincoCategoriasValidas();

        testErroStringNaoFechadaEOF();
        testErroStringNaoFechadaFimDeLinha();
        testErroCaractereForaDoAlfabeto();

        testCodigoRealista();

        System.out.println("\n>>> TODOS OS TESTES PASSARAM COM SUCESSO! <<<");
    }

    private static void testCincoCategoriasValidas() {
        System.out.print("Teste 1: Casos válidos das 5 categorias... ");
        String input = "total if \"ok\" == 42 3.14";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertFalse(scanner.hasErrors(), "Nao deve conter erros");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType(), "Identificador");
        assertEquals("total", tokens.get(0).getLexeme(), "Lexema identificador");

        assertEquals(TokenType.KW_IF, tokens.get(1).getType(), "Palavra reservada");
        assertEquals("if", tokens.get(1).getLexeme(), "Lexema palavra reservada");

        assertEquals(TokenType.LIT_STRING, tokens.get(2).getType(), "String");
        assertEquals("\"ok\"", tokens.get(2).getLexeme(), "Lexema string");

        assertEquals(TokenType.OP_EQ, tokens.get(3).getType(), "Operador");
        assertEquals("==", tokens.get(3).getLexeme(), "Lexema operador");

        assertEquals(TokenType.LIT_INT, tokens.get(4).getType(), "Literal numerico inteiro");
        assertEquals("42", tokens.get(4).getLexeme(), "Lexema inteiro");

        assertEquals(TokenType.LIT_DOUBLE, tokens.get(5).getType(), "Literal numerico double");
        assertEquals("3.14", tokens.get(5).getLexeme(), "Lexema double");

        System.out.println("OK");
    }

    private static void testErroStringNaoFechadaEOF() {
        System.out.print("Teste 2: Erro - String nao fechada ate EOF... ");
        String input = "string s = \"texto sem fechar";
        Scanner scanner = new Scanner(input);
        scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve registrar erro de string aberta ate EOF");
        assertEquals(1, scanner.getErrors().size(), "Deve conter 1 erro");
        assertEquals(1, scanner.getErrors().get(0).getLine(), "Linha do erro");
        assertEquals(12, scanner.getErrors().get(0).getColumn(), "Coluna da abertura das aspas");
        System.out.println("OK");
    }

    private static void testErroStringNaoFechadaFimDeLinha() {
        System.out.print("Teste 3: Erro - String nao fechada ate fim de linha... ");
        String input = "string s = \"linha quebrada\nint x = 10;";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve registrar erro de quebra de linha em string");
        boolean leuIntNaLinha2 = tokens.stream().anyMatch(t -> t.getType() == TokenType.KW_INT && t.getLine() == 2);
        assertTrue(leuIntNaLinha2, "Scanner deve se recuperar e tokenizar a linha seguinte");
        System.out.println("OK");
    }

    private static void testErroCaractereForaDoAlfabeto() {
        System.out.print("Teste 4: Erro - Caractere fora do alfabeto... ");
        String input = "int total = @ + 10;";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve registrar erro para '@'");
        assertEquals("@", scanner.getErrors().get(0).getOffendingText(), "Caractere invalido deve ser '@'");
        assertEquals(TokenType.KW_INT, tokens.get(0).getType(), "Recuperou int");
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType(), "Recuperou total");
        System.out.println("OK");
    }

    private static void testCodigoRealista() {
        System.out.print("Teste 5: Codigo realista em uma linha com declaracoes, expressoes e comentarios... ");
        String input = "int x = 10; double y = 3.14; /* comentario */ if (x >= 5) { return \"ok\"; } // fim";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertFalse(scanner.hasErrors(), "Codigo realista nao deve ter erros lexicos");

        TokenType[] esperados = {
            TokenType.KW_INT, TokenType.IDENTIFIER, TokenType.OP_ASSIGN, TokenType.LIT_INT, TokenType.SEMICOLON,
            TokenType.KW_DOUBLE, TokenType.IDENTIFIER, TokenType.OP_ASSIGN, TokenType.LIT_DOUBLE, TokenType.SEMICOLON,
            TokenType.KW_IF, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.OP_GE, TokenType.LIT_INT, TokenType.RPAREN,
            TokenType.LBRACE, TokenType.KW_RETURN, TokenType.LIT_STRING, TokenType.SEMICOLON, TokenType.RBRACE,
            TokenType.EOF
        };

        assertEquals(esperados.length, tokens.size(), "Quantidade total de tokens");
        for (int i = 0; i < esperados.length; i++) {
            assertEquals(esperados[i], tokens.get(i).getType(), "Token na posicao " + i);
        }
        System.out.println("OK");
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
