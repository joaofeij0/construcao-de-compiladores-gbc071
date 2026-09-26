package test;

import src.LexicalError;
import src.Scanner;
import src.Token;
import src.TokenType;

import java.util.List;

public class ScannerTest {
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println(" EXECUÇÃO DA SUÍTE DE TESTES DO ANALISADOR LÉXICO ");
        System.out.println("==================================================================");

        testIdentifiers();
        testKeywords();
        testNumericLiterals();
        testStringLiterals();
        testOperatorsAndMaximalMunch();
        testDelimiters();

        testErrorStringUnclosedEOF();
        testErrorStringUnclosedEOL();
        testErrorInvalidCharacterOutsideAlphabet();
        testErrorBlockCommentUnclosedEOF();

        testRealisticCode();

        System.out.println("\n==================================================================");
        System.out.println(" RESUMO DA SUÍTE DE TESTES:");
        System.out.printf(" Total de asserções executadas: %d%n", totalTests);
        System.out.printf(" [OK] Asserções bem-sucedidas:  %d%n", passedTests);
        System.out.printf(" [FALHA] Asserções falhadas:   %d%n", failedTests);
        System.out.println("==================================================================");

        if (failedTests == 0) {
            System.out.println(">>> TODOS OS TESTES PASSARAM COM SUCESSO! <<<");
        } else {
            System.err.println(">>> Houve falhas em testes! <<<");
            System.exit(1);
        }
    }

    private static void testIdentifiers() {
        System.out.println("\n[TESTE 1.1] Categoria Válida: Identificadores");
        String input = "total x1 contaItens _temp valor_max";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertEquals(6, tokens.size(), "Quantidade de tokens para identificadores (incluindo EOF)");
        assertEquals(TokenType.IDENTIFIER, tokens.get(0).getType(), "Tipo do token 0");
        assertEquals("total", tokens.get(0).getLexeme(), "Lexema do token 0");
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType(), "Tipo do token 1");
        assertEquals("x1", tokens.get(1).getLexeme(), "Lexema do token 1");
        assertEquals(TokenType.IDENTIFIER, tokens.get(2).getType(), "Tipo do token 2");
        assertEquals("contaItens", tokens.get(2).getLexeme(), "Lexema do token 2");
        assertEquals(TokenType.IDENTIFIER, tokens.get(3).getType(), "Tipo do token 3");
        assertEquals("_temp", tokens.get(3).getLexeme(), "Lexema do token 3");
        assertEquals(TokenType.IDENTIFIER, tokens.get(4).getType(), "Tipo do token 4");
        assertEquals("valor_max", tokens.get(4).getLexeme(), "Lexema do token 4");
        assertFalse(scanner.hasErrors(), "Sem erros léxicos em identificadores");
    }

    private static void testKeywords() {
        System.out.println("\n[TESTE 1.2] Categoria Válida: Palavras Reservadas (Lista Fechada)");
        String input = "int double bool char string void if else while for return true false";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        TokenType[] expectedTypes = {
                TokenType.KW_INT, TokenType.KW_DOUBLE, TokenType.KW_BOOL, TokenType.KW_CHAR,
                TokenType.KW_STRING, TokenType.KW_VOID, TokenType.KW_IF, TokenType.KW_ELSE,
                TokenType.KW_WHILE, TokenType.KW_FOR, TokenType.KW_RETURN, TokenType.KW_TRUE,
                TokenType.KW_FALSE
        };

        for (int i = 0; i < expectedTypes.length; i++) {
            assertEquals(expectedTypes[i], tokens.get(i).getType(), "Palavra reservada na posição " + i);
        }
        assertFalse(scanner.hasErrors(), "Sem erros léxicos em palavras reservadas");
    }

    private static void testNumericLiterals() {
        System.out.println("\n[TESTE 1.3] Categoria Válida: Literais Numéricos (Inteiro e Ponto Flutuante)");
        String input = "0 42 1000 3.1415 0.5 100.0";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertEquals(TokenType.LIT_INT, tokens.get(0).getType(), "0 é literal int");
        assertEquals("0", tokens.get(0).getLexeme(), "Lexema 0");
        assertEquals(TokenType.LIT_INT, tokens.get(1).getType(), "42 é literal int");
        assertEquals("42", tokens.get(1).getLexeme(), "Lexema 42");
        assertEquals(TokenType.LIT_INT, tokens.get(2).getType(), "1000 é literal int");
        assertEquals(TokenType.LIT_DOUBLE, tokens.get(3).getType(), "3.1415 é literal double");
        assertEquals("3.1415", tokens.get(3).getLexeme(), "Lexema 3.1415");
        assertEquals(TokenType.LIT_DOUBLE, tokens.get(4).getType(), "0.5 é literal double");
        assertEquals(TokenType.LIT_DOUBLE, tokens.get(5).getType(), "100.0 é literal double");
        assertFalse(scanner.hasErrors(), "Sem erros léxicos em números válidos");
    }

    private static void testStringLiterals() {
        System.out.println("\n[TESTE 1.4] Categoria Válida: Strings e Escapes");
        String input = "\"ola mundo\" \"linha 1\\nlinha 2\" \"com \\\"aspas\\\" e tab\\t\" \"\"";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertEquals(5, tokens.size(), "Quantidade de tokens para strings + EOF");
        assertEquals(TokenType.LIT_STRING, tokens.get(0).getType(), "Token 0 é string");
        assertEquals("\"ola mundo\"", tokens.get(0).getLexeme(), "Lexema string 0");
        assertEquals(TokenType.LIT_STRING, tokens.get(1).getType(), "Token 1 é string com escape \\n");
        assertEquals(TokenType.LIT_STRING, tokens.get(2).getType(), "Token 2 é string com escape \\\" e \\t");
        assertEquals(TokenType.LIT_STRING, tokens.get(3).getType(), "Token 3 é string vazia \"\"");
        assertFalse(scanner.hasErrors(), "Sem erros léxicos em strings bem formatadas");
    }

    private static void testOperatorsAndMaximalMunch() {
        System.out.println("\n[TESTE 1.5] Categoria Válida: Operadores e Desambiguação Maximal Munch");
        String input = "= == < <= > >= ! != + - * / % && ||";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        TokenType[] expected = {
                TokenType.OP_ASSIGN, TokenType.OP_EQ,
                TokenType.OP_LT, TokenType.OP_LE,
                TokenType.OP_GT, TokenType.OP_GE,
                TokenType.OP_NOT, TokenType.OP_NE,
                TokenType.OP_PLUS, TokenType.OP_MINUS, TokenType.OP_MULT, TokenType.OP_DIV, TokenType.OP_MOD,
                TokenType.OP_AND, TokenType.OP_OR
        };

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).getType(), "Operador desambiguado na posição " + i);
        }
        assertFalse(scanner.hasErrors(), "Sem erros léxicos em operadores válidos");
    }

    private static void testDelimiters() {
        System.out.println("\n[TESTE 1.6] Categoria Válida: Delimitadores e Pontuação");
        String input = "( ) { } [ ] ; ,";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        TokenType[] expected = {
                TokenType.LPAREN, TokenType.RPAREN,
                TokenType.LBRACE, TokenType.RBRACE,
                TokenType.LBRACKET, TokenType.RBRACKET,
                TokenType.SEMICOLON, TokenType.COMMA
        };

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).getType(), "Delimitador " + i);
        }
        assertFalse(scanner.hasErrors(), "Sem erros léxicos em delimitadores");
    }

    private static void testErrorStringUnclosedEOF() {
        System.out.println("\n[TESTE 2.1 - ERRO OBRIGATÓRIO] String não fechada até EOF");
        String input = "int x = 10;\nstring msg = \"esta string nunca fecha";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve registrar erro de string não fechada até EOF");
        assertEquals(1, scanner.getErrors().size(), "Exatamente 1 erro léxico");
        LexicalError err = scanner.getErrors().get(0);
        assertEquals(2, err.getLine(), "Linha do erro da string aberta");
        assertEquals(14, err.getColumn(), "Coluna de abertura das aspas");
        assertTrue(err.getMessage().contains("EOF"), "Mensagem deve mencionar EOF");
        assertEquals(TokenType.KW_INT, tokens.get(0).getType(), "Recuperação: token int reconhecido");
        assertEquals(TokenType.IDENTIFIER, tokens.get(1).getType(), "Recuperação: token x reconhecido");
    }

    private static void testErrorStringUnclosedEOL() {
        System.out.println("\n[TESTE 2.2 - ERRO OBRIGATÓRIO] String não fechada até fim de linha (EOL)");
        String input = "string s = \"string sem fechar na mesma linha\nint y = 20;";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve registrar erro de string não fechada até EOL");
        LexicalError err = scanner.getErrors().get(0);
        assertEquals(1, err.getLine(), "Erro na linha 1");
        assertTrue(err.getMessage().contains("fim da linha"), "Mensagem deve acusar quebra de linha");
        boolean foundIntInLine2 = tokens.stream().anyMatch(t -> t.getType() == TokenType.KW_INT && t.getLine() == 2);
        assertTrue(foundIntInLine2, "Recuperação: scanner deve continuar na linha seguinte e reconhecer 'int'");
    }

    private static void testErrorInvalidCharacterOutsideAlphabet() {
        System.out.println("\n[TESTE 2.3 - ERRO OBRIGATÓRIO] Caracteres fora do alfabeto (@, $, ~)");
        String input = "int @var = $100 + ~valor;";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve registrar erros de caracteres fora do alfabeto");
        assertEquals(3, scanner.getErrors().size(), "Deve conter 3 erros léxicos (@, $ e ~)");
        assertEquals("@", scanner.getErrors().get(0).getOffendingText(), "Primeiro caractere inválido é @");
        assertEquals("$", scanner.getErrors().get(1).getOffendingText(), "Segundo caractere inválido é $");
        assertEquals("~", scanner.getErrors().get(2).getOffendingText(), "Terceiro caractere inválido é ~");

        assertTrue(tokens.stream().anyMatch(t -> t.getLexeme().equals("var")), "Recuperou e reconheceu 'var'");
        assertTrue(tokens.stream().anyMatch(t -> t.getLexeme().equals("100")), "Recuperou e reconheceu '100'");
        assertTrue(tokens.stream().anyMatch(t -> t.getLexeme().equals("valor")), "Recuperou e reconheceu 'valor'");
    }

    private static void testErrorBlockCommentUnclosedEOF() {
        System.out.println("\n[TESTE 2.4 - ERRO EXTRA] Comentário de bloco não fechado até EOF");
        String input = "int a = 5; /* inicio do comentario que nunca fecha";
        Scanner scanner = new Scanner(input);
        List<Token> tokens = scanner.scanTokens();

        assertTrue(scanner.hasErrors(), "Deve acusar comentário de bloco não fechado até EOF");
        LexicalError err = scanner.getErrors().get(0);
        assertTrue(err.getMessage().contains("/*"), "Mensagem referencia o comentário de bloco");
        assertEquals(TokenType.KW_INT, tokens.get(0).getType(), "Tokens prévios foram gerados");
    }

    private static void testRealisticCode() {
        System.out.println("\n[TESTE 3.1] Trecho de Código Realista (Múltiplas Declarações e Comentários)");
        String code = "int x = 10; double y = 3.14; if (x >= 5) { return \"ok\"; } // teste final";
        Scanner scanner = new Scanner(code);
        List<Token> tokens = scanner.scanTokens();

        assertFalse(scanner.hasErrors(), "Código realista não deve conter erros léxicos");

        TokenType[] expectedTokens = {
                TokenType.KW_INT, TokenType.IDENTIFIER, TokenType.OP_ASSIGN, TokenType.LIT_INT, TokenType.SEMICOLON,
                TokenType.KW_DOUBLE, TokenType.IDENTIFIER, TokenType.OP_ASSIGN, TokenType.LIT_DOUBLE, TokenType.SEMICOLON,
                TokenType.KW_IF, TokenType.LPAREN, TokenType.IDENTIFIER, TokenType.OP_GE, TokenType.LIT_INT, TokenType.RPAREN,
                TokenType.LBRACE, TokenType.KW_RETURN, TokenType.LIT_STRING, TokenType.SEMICOLON, TokenType.RBRACE,
                TokenType.EOF
        };

        assertEquals(expectedTokens.length, tokens.size(), "Total de tokens do código realista");
        for (int i = 0; i < expectedTokens.length; i++) {
            assertEquals(expectedTokens[i], tokens.get(i).getType(),
                    String.format("Token [%d] '%s' confere com o esperado", i, tokens.get(i).getLexeme()));
        }
        System.out.println("  -> Código realista completamente tokenizado com sucesso!");
    }

    private static void assertEquals(Object expected, Object actual, String description) {
        totalTests++;
        if (expected == null && actual == null) {
            passedTests++;
            System.out.println("  ✓ PASS: " + description);
        } else if (expected != null && expected.equals(actual)) {
            passedTests++;
            System.out.println("  ✓ PASS: " + description);
        } else {
            failedTests++;
            System.err.println("  ✗ FAIL: " + description + " | Esperado: [" + expected + "], mas obteve: [" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String description) {
        assertEquals(true, condition, description);
    }

    private static void assertFalse(boolean condition, String description) {
        assertEquals(false, condition, description);
    }
}
