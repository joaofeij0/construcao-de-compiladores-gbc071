package test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import src.Scanner;
import src.Token;
import src.TokenType;
import src.ast.*;
import src.symbols.Type;
import src.types.TypeReport;
import src.types.TypeRules;
import src.visitor.DepthFirstVisitor;

import static src.symbols.Type.*;

public class TypeTest {
    private static Path root = Paths.get(".");
    private static int passed = 0;

    private static final Type[] BASIC = { INT, DOUBLE, BOOL, CHAR, STRING };

    private static final String[] TABELA_BINARIOS = {
        "+ int int int", "+ int double double", "+ double int double", "+ double double double",
        "- int int int", "- int double double", "- double int double", "- double double double",
        "* int int int", "* int double double", "* double int double", "* double double double",
        "/ int int int", "/ int double double", "/ double int double", "/ double double double",
        "% int int int",
        "< int int bool", "< int double bool", "< double int bool", "< double double bool",
        "<= int int bool", "<= int double bool", "<= double int bool", "<= double double bool",
        "> int int bool", "> int double bool", "> double int bool", "> double double bool",
        ">= int int bool", ">= int double bool", ">= double int bool", ">= double double bool",
        "== int int bool", "== double double bool", "== bool bool bool", "== char char bool", "== string string bool",
        "== int double bool", "== double int bool",
        "!= int int bool", "!= double double bool", "!= bool bool bool", "!= char char bool", "!= string string bool",
        "!= int double bool", "!= double int bool",
        "&& bool bool bool",
        "|| bool bool bool",
    };

    private static final String[] TABELA_UNARIOS = { "! bool bool", "- int int", "- double double" };

    public static void main(String[] args) throws IOException {
        if (args.length > 0) root = Paths.get(args[0]);
        System.out.println("=== TESTES DO CP5: VERIFICADOR DE TIPOS ===\n");

        System.out.println("-- 1. Regras isoladas (tabela de operadores, sem passar pelo parser)");
        testTabelaBinarios();
        testTabelaUnarios();
        testErroSemCascata();
        testArgumentoAceito();

        System.out.println("\n-- 2. Programas x saídas esperadas (test/programas/*.txt e test/esperado/*.txt)");
        List<String> faltando = testProgramas();

        System.out.println("\n-- 3. Toda expressão de programa sem erro recebe um tipo");
        testTodaExpressaoTipada();

        System.out.println("\n-- 4. Nenhuma entrada lança exceção");
        testSemExcecoes();

        System.out.println("\n-- 5. Regressão: suítes do CP3 e do CP4");
        AstTest.main(new String[] { root.toString() });
        ok("Suíte do CP3 (AstTest)");
        IdentTest.main(new String[] { root.toString() });
        ok("Suíte do CP4 (IdentTest)");

        if (!divergentes.isEmpty() || !faltando.isEmpty()) {
            System.out.println("\n>>> " + passed + " TESTES PASSARAM, MAS HÁ PENDÊNCIAS: <<<");
            if (!divergentes.isEmpty()) {
                System.out.println("  " + divergentes.size() + " programa(s) com saída diferente da esperada:");
                for (String f : divergentes) System.out.println("    " + f);
            }
            if (!faltando.isEmpty()) {
                System.out.println("  " + faltando.size() + " arquivo(s) esperado(s) ainda não escrito(s):");
                for (String f : faltando) System.out.println("    " + f);
            }
            System.exit(1);
        }
        System.out.println("\n>>> TODOS OS " + passed + " TESTES DO CP5 PASSARAM COM SUCESSO! <<<");
    }

    private static BinOp binOp(String symbol) {
        for (BinOp op : BinOp.values()) if (op.symbol().equals(symbol)) return op;
        throw new IllegalArgumentException(symbol);
    }

    private static Type type(String label) {
        for (Type t : Type.values()) if (t.label().equals(label)) return t;
        throw new IllegalArgumentException(label);
    }

    private static void testTabelaBinarios() {
        Map<String, Type> aceitos = new HashMap<>();
        for (String linha : TABELA_BINARIOS) {
            String[] p = linha.split(" ");
            aceitos.put(p[0] + " " + p[1] + " " + p[2], type(p[3]));
        }
        int aceitas = 0;
        int recusadas = 0;
        for (BinOp op : BinOp.values()) {
            for (Type l : BASIC) {
                for (Type r : BASIC) {
                    String chave = op.symbol() + " " + l + " " + r;
                    Type esperado = aceitos.get(chave);
                    Type obtido = TypeRules.binario(op, l, r);
                    assertEquals(esperado, obtido, "tabela: " + chave);
                    if (esperado == null) recusadas++; else aceitas++;
                }
            }
        }
        ok("Operadores binários: " + aceitas + " combinações aceitas e " + recusadas + " recusadas, como na tabela");
    }

    private static void testTabelaUnarios() {
        Map<String, Type> aceitos = new HashMap<>();
        for (String linha : TABELA_UNARIOS) {
            String[] p = linha.split(" ");
            aceitos.put(p[0] + " " + p[1], type(p[2]));
        }
        int n = 0;
        for (UnOp op : UnOp.values()) {
            for (Type t : BASIC) {
                String chave = op.symbol() + " " + t;
                assertEquals(aceitos.get(chave), TypeRules.unario(op, t), "tabela: " + chave);
                n++;
            }
        }
        ok("Operadores unários: " + n + " combinações, como na tabela");
    }

    private static void testErroSemCascata() {
        for (BinOp op : BinOp.values()) {
            for (Type t : BASIC) {
                assertEquals(ERRO, TypeRules.binario(op, ERRO, t), "[ERRO] " + op.symbol() + " erro " + t);
                assertEquals(ERRO, TypeRules.binario(op, t, ERRO), "[ERRO] " + op.symbol() + " " + t + " erro");
            }
            assertEquals(ERRO, TypeRules.binario(op, ERRO, ERRO), "[ERRO] " + op.symbol() + " erro erro");
        }
        for (UnOp op : UnOp.values()) assertEquals(ERRO, TypeRules.unario(op, ERRO), "[ERRO] " + op.symbol());
        ok("[ERRO]: um operando erro dá erro, sem recusa nova");
    }

    private static void testArgumentoAceito() {
        for (Type veio : BASIC) {
            for (Type esperado : BASIC) {
                boolean regra = veio == esperado || (veio == INT && esperado == DOUBLE);
                assertEquals(regra, TypeRules.argumentoAceito(veio, esperado), "≼: " + veio + " " + esperado);
                assertEquals(veio == esperado, TypeRules.mesmoTipo(veio, esperado), "tipo igual: " + veio + " " + esperado);
            }
            assertTrue(TypeRules.argumentoAceito(ERRO, veio) && TypeRules.argumentoAceito(veio, ERRO), "≼ com erro");
        }
        ok("U ≼ T (argumentos aceitam int → double) e tipo igual (atribuição, inicialização e return)");
    }

    private static List<String> testProgramas() throws IOException {
        List<String> faltando = new ArrayList<>();
        for (Path f : listar("test/programas")) {
            String nome = f.getFileName().toString();
            Path esperado = root.resolve("test/esperado").resolve(nome);
            if (!Files.exists(esperado)) {
                faltando.add("test/esperado/" + nome);
                System.out.println("  [SEM ESPERADO] " + nome);
                continue;
            }
            String obtido = TypeReport.run(read(f));
            try {
                assertSameText(read(esperado), obtido, nome + ": saída do verificador x test/esperado/" + nome);
                ok(nome);
            } catch (AssertionError e) {
                divergentes.add(nome);
                System.out.println("  [FALHOU] " + e.getMessage().replace("\n", "\n    "));
            }
        }
        return faltando;
    }

    private static final List<String> divergentes = new ArrayList<>();

    private static final class TipoAusente extends DepthFirstVisitor {
        final List<String> semTipo = new ArrayList<>();

        private Void marca(Expr e, String nome) {
            if (e.getType() == null) semTipo.add(nome + " na linha " + e.getLine());
            return null;
        }

        @Override public Void visitIntLit(IntLit n) { return marca(n, "literal int"); }
        @Override public Void visitDoubleLit(DoubleLit n) { return marca(n, "literal double"); }
        @Override public Void visitBoolLit(BoolLit n) { return marca(n, "literal bool"); }
        @Override public Void visitCharLit(CharLit n) { return marca(n, "literal char"); }
        @Override public Void visitStringLit(StringLit n) { return marca(n, "literal string"); }
        @Override public Void visitId(Id n) { return marca(n, "Id " + n.getName()); }
        @Override public Void visitBin(Bin n) { super.visitBin(n); return marca(n, "Bin " + n.getOp().symbol()); }
        @Override public Void visitUnary(Unary n) { super.visitUnary(n); return marca(n, "Unary"); }
        @Override public Void visitCall(Call n) { super.visitCall(n); return marca(n, "Call " + n.getName()); }
    }

    private static void testTodaExpressaoTipada() throws IOException {
        int programas = 0;
        List<Path> arquivos = new ArrayList<>(listar("test/programas"));
        arquivos.addAll(listar("test/programas/validos"));
        arquivos.addAll(listar("test/programas/cp4"));
        for (Path f : arquivos) {
            TypeReport r = TypeReport.analyze(read(f));
            if (!r.canGenerateCode()) continue;
            TipoAusente v = new TipoAusente();
            r.getTree().accept(v);
            assertTrue(v.semTipo.isEmpty(), f.getFileName() + ": expressões sem tipo: " + v.semTipo);
            programas++;
        }
        ok("Em " + programas + " programas aceitos, toda expressão tem o tipo gravado no nó");
    }

    private static void testSemExcecoes() throws IOException {
        int casos = 0;
        List<Path> arquivos = new ArrayList<>(listar("test/programas"));
        arquivos.addAll(listar("test/programas/validos"));
        arquivos.addAll(listar("test/programas/invalidos"));
        arquivos.addAll(listar("test/programas/cp4"));
        for (Path f : arquivos) {
            String src = read(f);
            for (int i = 0; i <= src.length(); i++) {
                semExcecao(src.substring(0, i), f.getFileName() + " cortado em " + i);
                casos++;
            }
        }
        ok("Todos os prefixos dos programas de teste (" + casos + " entradas) sem exceção");

        List<List<Token>> bases = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        for (Path f : arquivos) {
            List<Token> toks = new Scanner(read(f)).scanTokens();
            bases.add(toks);
            for (Token t : toks) if (t.getType() == TokenType.IDENTIFIER) nomes.add(t.getLexeme());
        }
        Random rnd = new Random(20261125L);
        int verificados = 0;
        int comErro = 0;
        for (int n = 0; n < 4000; n++) {
            List<Token> base = bases.get(rnd.nextInt(bases.size()));
            StringBuilder sb = new StringBuilder();
            for (Token t : base) {
                String lex = t.getLexeme();
                if (rnd.nextInt(6) == 0) lex = trocaNaCategoria(t, rnd, nomes);
                sb.append(lex).append(t.getType() == TokenType.SEMICOLON || t.getType() == TokenType.LBRACE
                        || t.getType() == TokenType.RBRACE ? "\n" : " ");
            }
            TypeReport r = semExcecao(sb.toString(), "mutação #" + n);
            if (r.wasAnalyzed()) {
                verificados++;
                if (r.hasErrors()) comErro++;
            }
        }
        ok("4000 mutações dos programas sem exceção (" + verificados + " chegaram à verificação de tipos, "
                + comErro + " delas com erro de tipo ou de identificação)");
    }

    private static final String[] TIPOS = { "int", "double", "bool", "char", "string" };
    private static final String[] LITERAIS = { "0", "7", "2.5", "true", "false", "'a'", "\"s\"" };
    private static final String[] BINARIOS = { "+", "-", "*", "/", "%", "<", "<=", ">", ">=", "==", "!=", "&&", "||" };

    private static String trocaNaCategoria(Token t, Random rnd, List<String> nomes) {
        switch (t.getType()) {
            case KW_INT: case KW_DOUBLE: case KW_BOOL: case KW_CHAR: case KW_STRING:
                return TIPOS[rnd.nextInt(TIPOS.length)];
            case LIT_INT: case LIT_DOUBLE: case LIT_STRING: case LIT_CHAR: case KW_TRUE: case KW_FALSE:
                return LITERAIS[rnd.nextInt(LITERAIS.length)];
            case IDENTIFIER:
                return nomes.isEmpty() ? t.getLexeme() : nomes.get(rnd.nextInt(nomes.size()));
            case OP_PLUS: case OP_MINUS: case OP_MULT: case OP_DIV: case OP_MOD: case OP_LT: case OP_LE:
            case OP_GT: case OP_GE: case OP_EQ: case OP_NE: case OP_AND: case OP_OR:
                return BINARIOS[rnd.nextInt(BINARIOS.length)];
            default:
                return t.getLexeme();
        }
    }

    private static TypeReport semExcecao(String src, String nome) {
        try {
            return TypeReport.analyze(src);
        } catch (RuntimeException | StackOverflowError e) {
            throw new AssertionError("FALHA: exceção em " + nome + ": " + e, e);
        }
    }

    private static String read(Path p) throws IOException {
        String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }

    private static List<Path> listar(String dir) throws IOException {
        List<Path> files = new ArrayList<>();
        Path d = root.resolve(dir);
        if (!Files.isDirectory(d)) return files;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(d, "*.txt")) {
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

    private static void assertEquals(Object esperado, Object obtido, String msg) {
        if (esperado == null ? obtido == null : esperado.equals(obtido)) return;
        throw new AssertionError("FALHA: " + msg + " (esperado: " + esperado + ", obtido: " + obtido + ")");
    }

    private static void assertTrue(boolean condicao, String msg) {
        if (!condicao) throw new AssertionError("FALHA: " + msg);
    }

    private static void ok(String descricao) {
        passed++;
        System.out.println("  [OK] " + descricao);
    }
}
