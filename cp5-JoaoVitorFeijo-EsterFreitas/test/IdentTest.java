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

import src.IdentReport;
import src.ParseResult;
import src.Parser;
import src.Scanner;
import src.ast.Block;
import src.ast.FuncDecl;
import src.ast.Program;
import src.ast.VarDecl;
import src.symbols.Kind;
import src.symbols.SemanticError;
import src.symbols.StackSymbolTable;
import src.symbols.Symbol;
import src.symbols.SymbolTable;
import src.visitor.IdentificationVisitor;
import src.visitor.UseCollector;

public class IdentTest {
    private static Path root = Paths.get(".");
    private static int passed = 0;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) root = Paths.get(args[0]);
        System.out.println("=== TESTES DO CP4: TABELA DE SÍMBOLOS E IDENTIFICAÇÃO ===\n");

        System.out.println("-- 1. Tabela de símbolos isolada (StackSymbolTable pela interface)");
        testDeclararEBuscar();
        testSombreamentoERestauracao();
        testRedeclaracao();
        testLookupLocalNaoVeExterno();
        testInexistenteDevolveNull();
        testLevel();
        testCloseScopeSemEscopo();
        testDeclareSemEscopo();
        testTabelaContraModeloIngenuo();

        System.out.println("\n-- 2. Programas obrigatórios (saída esperada escrita à mão)");
        testPrograma("obrig1_valido_sombreamento", "válido com sombreamento");
        testPrograma("obrig2_redeclaracao", "redeclaração");
        testPrograma("obrig3_nao_declarado", "nome não declarado");

        System.out.println("\n-- 3. Um programa por decisão de projeto");
        testPrograma("dec1_sombreamento", "decisão 1: sombreamento");
        testPrograma("dec2_declarar_antes_de_usar", "decisão 2: declarar antes de usar");
        testPrograma("dec3_mutua_recursao", "decisão 3: funções mutuamente recursivas");
        testPrograma("dec4_parametros_corpo", "decisão 4: parâmetros e corpo no mesmo escopo");
        testPrograma("dec5_espaco_de_nomes", "decisão 5: um só espaço de nomes");
        testPrograma("extra_for_escopo", "decisão extra: escopo do for");

        System.out.println("\n-- 4. Casos exigidos");
        testPrograma("tres_erros", "pelo menos três erros numa rodada, na ordem do texto");
        testOrdemDosErros();
        testPrograma("recursivo", "procedimento recursivo");
        testPrograma("uso_apos_bloco", "uso depois do fechamento do bloco");
        testTodosOsProgramasTemEsperado();

        System.out.println("\n-- 5. A AST decorada");
        testDeclApontaParaONoDaDeclaracao();
        testNivelDoSimboloIgualAoDaTabela();
        testSemanticaNaoDependeDaImplementacao();

        System.out.println("\n-- 6. Robustez");
        testErroSintaticoNaoChegaAPassada();
        testArvoreNulaRecusadaSemNPE();
        testDeterminismo();
        testProgramasAleatorios();

        System.out.println("\n>>> TODOS OS " + passed + " TESTES PASSARAM COM SUCESSO! <<<");
    }

    private static Symbol sym(SymbolTable t, String name) {
        return new Symbol(name, Kind.VAR, null, t.level());
    }

    private static void testDeclararEBuscar() {
        SymbolTable t = new StackSymbolTable();
        t.openScope();
        Symbol x = sym(t, "x");
        assertTrue(t.declare(x), "declare de nome novo devolve true");
        assertTrue(t.lookup("x") == x, "lookup acha o símbolo declarado");
        assertTrue(t.lookupLocal("x") == x, "lookupLocal acha no escopo do topo");
        ok("declarar e buscar");
    }

    private static void testSombreamentoERestauracao() {
        SymbolTable t = new StackSymbolTable();
        t.openScope();
        Symbol xGlobal = sym(t, "x");
        Symbol y = sym(t, "y");
        t.declare(xGlobal);
        t.declare(y);
        t.openScope();
        Symbol xInterno = sym(t, "x");
        assertTrue(t.declare(xInterno), "nome externo pode ser declarado no escopo interno");
        assertTrue(t.lookup("x") == xInterno, "o interno esconde o externo");
        assertTrue(t.lookup("y") == y, "nome não sombreado continua visível");
        t.closeScope();
        assertTrue(t.lookup("x") == xGlobal, "depois do closeScope o externo reaparece intacto");
        ok("sombreamento e restauração depois do closeScope");
    }

    private static void testRedeclaracao() {
        SymbolTable t = new StackSymbolTable();
        t.openScope();
        Symbol primeiro = sym(t, "a");
        Symbol segundo = sym(t, "a");
        assertTrue(t.declare(primeiro), "primeira declaração aceita");
        assertTrue(!t.declare(segundo), "redeclaração no mesmo escopo devolve false");
        assertTrue(t.lookup("a") == primeiro, "o primeiro símbolo permanece (lookup)");
        assertTrue(t.lookupLocal("a") == primeiro, "o primeiro símbolo permanece (lookupLocal)");
        ok("redeclaração: declare devolve false e o primeiro permanece");
    }

    private static void testLookupLocalNaoVeExterno() {
        SymbolTable t = new StackSymbolTable();
        t.openScope();
        t.declare(sym(t, "g"));
        t.openScope();
        assertTrue(t.lookupLocal("g") == null, "lookupLocal não enxerga o escopo externo");
        assertTrue(t.lookup("g") != null, "lookup enxerga o escopo externo");
        ok("lookupLocal não enxerga o escopo externo");
    }

    private static void testInexistenteDevolveNull() {
        SymbolTable t = new StackSymbolTable();
        assertTrue(t.lookup("z") == null, "lookup sem escopo aberto devolve null");
        assertTrue(t.lookupLocal("z") == null, "lookupLocal sem escopo aberto devolve null");
        t.openScope();
        assertTrue(t.lookup("z") == null, "lookup de nome inexistente devolve null");
        assertTrue(t.lookupLocal("z") == null, "lookupLocal de nome inexistente devolve null");
        assertTrue(t.lookup(null) == null && t.lookupLocal(null) == null, "nome null devolve null");
        ok("lookup de nome inexistente devolve null, sem exceção");
    }

    private static void testLevel() {
        SymbolTable t = new StackSymbolTable();
        assertTrue(t.level() == -1, "sem escopo: -1");
        t.openScope();
        assertTrue(t.level() == 0, "global: 0");
        t.openScope();
        t.openScope();
        assertTrue(t.level() == 2, "dois blocos dentro do global: 2");
        Symbol s = sym(t, "k");
        assertTrue(s.level == 2, "Symbol criado com o nível atual");
        t.closeScope();
        assertTrue(t.level() == 1, "closeScope volta um nível");
        ok("level()");
    }

    private static void testCloseScopeSemEscopo() {
        SymbolTable t = new StackSymbolTable();
        try {
            t.closeScope();
            throw new AssertionError("FALHA: closeScope sem escopo deveria lançar IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("closeScope sem escopo aberto"), "mensagem documentada");
        }
        t.openScope();
        assertTrue(t.level() == 0, "tabela utilizável depois do erro");
        ok("closeScope sem escopo: IllegalStateException documentada (nunca NPE)");
    }

    private static void testDeclareSemEscopo() {
        SymbolTable t = new StackSymbolTable();
        try {
            t.declare(new Symbol("x", Kind.VAR, null, 0));
            throw new AssertionError("FALHA: declare sem escopo deveria lançar IllegalStateException");
        } catch (IllegalStateException e) {
        }
        t.openScope();
        try {
            t.declare(null);
            throw new AssertionError("FALHA: declare(null) deveria lançar IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        ok("declare sem escopo / declare(null): exceções documentadas (nunca NPE)");
    }

    static final class NaiveSymbolTable implements SymbolTable {
        private final List<List<Symbol>> scopes = new ArrayList<>();
        public void openScope() { scopes.add(new ArrayList<>()); }
        public void closeScope() {
            if (scopes.isEmpty()) throw new IllegalStateException("closeScope sem escopo aberto");
            scopes.remove(scopes.size() - 1);
        }
        public int level() { return scopes.size() - 1; }
        public boolean declare(Symbol s) {
            if (scopes.isEmpty()) throw new IllegalStateException("declare sem escopo aberto");
            if (lookupLocal(s.name) != null) return false;
            scopes.get(scopes.size() - 1).add(s);
            return true;
        }
        public Symbol lookupLocal(String name) {
            if (scopes.isEmpty()) return null;
            for (Symbol s : scopes.get(scopes.size() - 1)) if (s.name.equals(name)) return s;
            return null;
        }
        public Symbol lookup(String name) {
            for (int i = scopes.size() - 1; i >= 0; i--)
                for (Symbol s : scopes.get(i)) if (s.name.equals(name)) return s;
            return null;
        }
    }

    private static void testTabelaContraModeloIngenuo() {
        Random rnd = new Random(4);
        String[] names = { "a", "b", "c", "d" };
        int ops = 0;
        for (int round = 0; round < 50; round++) {
            SymbolTable real = new StackSymbolTable();
            SymbolTable model = new NaiveSymbolTable();
            real.openScope();
            model.openScope();
            for (int i = 0; i < 400; i++, ops++) {
                int op = rnd.nextInt(10);
                String n = names[rnd.nextInt(names.length)];
                if (op < 2) {
                    real.openScope(); model.openScope();
                } else if (op < 3 && real.level() > 0) {
                    real.closeScope(); model.closeScope();
                } else if (op < 6) {
                    Symbol s = new Symbol(n, Kind.VAR, null, real.level());
                    assertTrue(real.declare(s) == model.declare(s), "declare diverge do modelo");
                } else if (op < 8) {
                    assertTrue(real.lookup(n) == model.lookup(n), "lookup diverge do modelo");
                } else {
                    assertTrue(real.lookupLocal(n) == model.lookupLocal(n), "lookupLocal diverge do modelo");
                }
                assertTrue(real.level() == model.level(), "level diverge do modelo");
            }
        }
        ok(ops + " operações aleatórias sem divergência do modelo ingênuo");
    }

    private static void testPrograma(String nome, String descricao) throws IOException {
        String src = read("test/programas/cp4/" + nome + ".txt");
        String esperado = read("test/esperado/cp4/" + nome + ".txt");
        assertSameText(esperado, IdentReport.run(src), nome);
        ok(nome + " (" + descricao + ")");
    }

    private static void testOrdemDosErros() throws IOException {
        Program p = parseValid(read("test/programas/cp4/tres_erros.txt"));
        IdentificationVisitor v = new IdentificationVisitor();
        v.run(p);
        List<SemanticError> errs = v.getErrors();
        assertTrue(errs.size() >= 3, "pelo menos três erros");
        for (int i = 1; i < errs.size(); i++) {
            assertTrue(errs.get(i - 1).compareTo(errs.get(i)) < 0, "erros fora da ordem do texto");
        }
        ok(errs.size() + " erros, em ordem de linha (o da linha 7 vem da passada prévia e mesmo assim fica no lugar)");
    }

    private static void testTodosOsProgramasTemEsperado() throws IOException {
        int n = 0;
        for (Path p : list("test/programas/cp4")) {
            String nome = p.getFileName().toString();
            assertTrue(Files.exists(root.resolve("test/esperado/cp4/" + nome)), "falta esperado para " + nome);
            assertSameText(read("test/esperado/cp4/" + nome),
                    IdentReport.run(read("test/programas/cp4/" + nome)), nome);
            n++;
        }
        ok("todos os " + n + " programas de test/programas/cp4 conferem com test/esperado/cp4");
    }

    private static void testDeclApontaParaONoDaDeclaracao() throws IOException {
        Program p = parseValid(read("test/programas/cp4/obrig1_valido_sombreamento.txt"));
        IdentificationVisitor.analyze(p);

        FuncDecl soma = (FuncDecl) p.getDecls().get(2);
        VarDecl r = (VarDecl) soma.getBody().getStmts().get(0);
        Block b1 = (Block) soma.getBody().getStmts().get(1);
        VarDecl xInterno = (VarDecl) b1.getStmts().get(0);
        VarDecl yInterno = (VarDecl) ((Block) b1.getStmts().get(1)).getStmts().get(0);
        VarDecl yGlobal = (VarDecl) p.getDecls().get(1);

        int checados = 0;
        for (UseCollector.Use u : UseCollector.collect(p)) {
            assertTrue(u.decl != null, "todo uso do programa válido deve ser decorado: " + u);
            assertTrue(u.decl.name.equals(u.name), "decl tem o mesmo nome do uso: " + u);
            int line = u.node.getLine();
            if (line == 9 && u.name.equals("y")) { assertTrue(u.decl.decl == yInterno, "y@9 -> y@8"); checados++; }
            if (line == 11 && u.name.equals("y")) { assertTrue(u.decl.decl == yGlobal, "y@11 -> y@2"); checados++; }
            if (line == 8 && u.name.equals("x")) { assertTrue(u.decl.decl == xInterno, "x@8 -> x@6"); checados++; }
            if (line == 13 && u.name.equals("r")) { assertTrue(u.decl.decl == r, "r@13 -> r@4"); checados++; }
            if (line == 16 && u.name.equals("soma")) { assertTrue(u.decl.decl == soma, "soma@16 -> FuncDecl"); checados++; }
        }
        assertTrue(checados == 5, "os cinco usos conferidos foram encontrados");
        ok("decl aponta para o próprio nó da declaração (identidade de objeto)");
    }

    private static void testNivelDoSimboloIgualAoDaTabela() throws IOException {
        final int[] declares = { 0 };
        SymbolTable spy = new SymbolTable() {
            final SymbolTable in = new StackSymbolTable();
            public void openScope() { in.openScope(); }
            public void closeScope() { in.closeScope(); }
            public boolean declare(Symbol s) {
                assertTrue(s.level == in.level(), "Symbol.level != table.level() para " + s);
                declares[0]++;
                return in.declare(s);
            }
            public Symbol lookupLocal(String n) { return in.lookupLocal(n); }
            public Symbol lookup(String n) { return in.lookup(n); }
            public int level() { return in.level(); }
        };
        for (Path f : list("test/programas/cp4")) {
            new IdentificationVisitor(spy).run(parseValid(read("test/programas/cp4/" + f.getFileName())));
            assertTrue(spy.level() == -1, "a passada fecha todos os escopos que abre");
        }
        ok(declares[0] + " declarações com Symbol.level == table.level(); todo escopo aberto é fechado");
    }

    private static void testSemanticaNaoDependeDaImplementacao() throws IOException {
        for (Path f : list("test/programas/cp4")) {
            String src = read("test/programas/cp4/" + f.getFileName());
            assertSameText(IdentReport.run(src, new StackSymbolTable()),
                    IdentReport.run(src, new NaiveSymbolTable()), "implementações divergem em " + f);
        }
        ok("a passada só depende da interface: mesma saída com o modelo ingênuo");
    }

    private static void testErroSintaticoNaoChegaAPassada() {
        String[] invalidos = {
            "int a = ;",
            "void f( { }",
            "int x = 1 int y = 2;",
            "void f() { x = ; y = 1 }",
            "int @ = 3;",
        };
        for (String s : invalidos) {
            String r = IdentReport.run(s);
            assertTrue(r.contains("passada de identificação não executada"), "não deveria rodar a passada: " + s);
        }
        ok("programa com erro léxico/sintático não chega à passada");
    }

    private static void testArvoreNulaRecusadaSemNPE() {
        try {
            new IdentificationVisitor().run(null);
            throw new AssertionError("FALHA: árvore nula deveria ser recusada");
        } catch (IllegalArgumentException e) {
        }
        ok("árvore nula: IllegalArgumentException com mensagem (nunca NPE)");
    }

    private static void testDeterminismo() throws IOException {
        for (Path f : list("test/programas/cp4")) {
            String src = read("test/programas/cp4/" + f.getFileName());
            Program p = parseValid(src);
            List<String> a = runPass(p);
            List<String> b = runPass(p);
            assertTrue(a.equals(b), "segunda rodada diferente em " + f);
            assertSameText(IdentReport.run(src), IdentReport.run(src), "relatório não determinístico");
        }
        ok("mesma entrada, mesma saída (também ao rodar duas vezes sobre a mesma árvore)");
    }

    private static List<String> runPass(Program p) {
        IdentificationVisitor v = new IdentificationVisitor();
        v.run(p);
        List<String> out = new ArrayList<>(v.getMessages());
        for (UseCollector.Use u : UseCollector.collect(p)) out.add(u.toString());
        return out;
    }

    private static void testProgramasAleatorios() {
        Random rnd = new Random(2026);
        int total = 2000;
        for (int n = 0; n < total; n++) {
            String src = new Gen(rnd).program();
            Program p;
            try {
                p = parseValid(src);
            } catch (AssertionError e) {
                throw new AssertionError("gerador produziu programa inválido:\n" + src, e);
            }
            IdentificationVisitor v = new IdentificationVisitor();
            try {
                v.run(p);
            } catch (RuntimeException e) {
                throw new AssertionError("exceção na passada:\n" + src, e);
            }
            List<String> semDecl = new ArrayList<>();
            for (UseCollector.Use u : UseCollector.collect(p)) {
                if (u.decl == null) semDecl.add("linha " + u.node.getLine() + ": '" + u.name + "' não declarado");
                else assertTrue(u.decl.name.equals(u.name), "decl com nome errado:\n" + src);
            }
            List<String> naoDecl = new ArrayList<>();
            for (String m : v.getMessages()) if (m.endsWith("não declarado")) naoDecl.add(m);
            Collections.sort(semDecl);
            Collections.sort(naoDecl);
            assertTrue(semDecl.equals(naoDecl), "usos sem decl != erros 'não declarado':\n" + src);
            assertSameText(IdentReport.run(src, new StackSymbolTable()),
                    IdentReport.run(src, new NaiveSymbolTable()), "implementações divergem:\n" + src);
        }
        ok(total + " programas válidos aleatórios: sem exceção, decoração e erros coerentes");
    }

    private static final class Gen {
        private static final String[] NAMES = { "a", "b", "c", "f", "g" };
        private final Random r;
        private final StringBuilder sb = new StringBuilder();
        Gen(Random r) { this.r = r; }

        String name() { return NAMES[r.nextInt(NAMES.length)]; }
        String type() { return r.nextBoolean() ? "int" : "bool"; }

        String program() {
            int n = 1 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                int k = r.nextInt(3);
                if (k == 0) sb.append(type()).append(' ').append(name()).append(maybeInit()).append(";\n");
                else {
                    sb.append(k == 1 ? "void" : type()).append(' ').append(name()).append('(');
                    int np = r.nextInt(3);
                    for (int j = 0; j < np; j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(type()).append(' ').append(name());
                    }
                    sb.append(") ");
                    block(0);
                    sb.append('\n');
                }
            }
            return sb.toString();
        }

        String maybeInit() { return r.nextBoolean() ? " = " + expr(0) : ""; }

        void block(int depth) {
            sb.append("{\n");
            int n = r.nextInt(4);
            for (int i = 0; i < n; i++) stmt(depth + 1);
            sb.append("}\n");
        }

        void stmt(int depth) {
            int k = r.nextInt(depth > 3 ? 4 : 8);
            switch (k) {
                case 0: sb.append(type()).append(' ').append(name()).append(maybeInit()).append(";\n"); break;
                case 1: sb.append(name()).append(" = ").append(expr(0)).append(";\n"); break;
                case 2: sb.append(call()).append(";\n"); break;
                case 3: sb.append("return").append(r.nextBoolean() ? " " + expr(0) : "").append(";\n"); break;
                case 4: block(depth); break;
                case 5:
                    sb.append("if (").append(expr(0)).append(") ");
                    block(depth);
                    if (r.nextBoolean()) { sb.append("else "); block(depth); }
                    break;
                case 6: sb.append("while (").append(expr(0)).append(") "); block(depth); break;
                default:
                    sb.append("for (");
                    int ini = r.nextInt(3);
                    if (ini == 1) sb.append("int ").append(name()).append(" = ").append(expr(0));
                    if (ini == 2) sb.append(name()).append(" = ").append(expr(0));
                    sb.append("; ");
                    if (r.nextBoolean()) sb.append(expr(0));
                    sb.append("; ");
                    if (r.nextBoolean()) sb.append(name()).append(" = ").append(expr(0));
                    sb.append(") ");
                    block(depth);
            }
        }

        String call() {
            StringBuilder c = new StringBuilder(name()).append('(');
            int n = r.nextInt(3);
            for (int i = 0; i < n; i++) {
                if (i > 0) c.append(", ");
                c.append(expr(2));
            }
            return c.append(')').toString();
        }

        String expr(int depth) {
            int k = r.nextInt(depth > 2 ? 3 : 6);
            switch (k) {
                case 0: return String.valueOf(r.nextInt(10));
                case 1: return name();
                case 2: return r.nextBoolean() ? "true" : "false";
                case 3: return expr(depth + 1) + (r.nextBoolean() ? " + " : " < ") + expr(depth + 1);
                case 4: return "!(" + expr(depth + 1) + ")";
                default: return call();
            }
        }
    }

    private static Program parseValid(String src) {
        Scanner scanner = new Scanner(src);
        ParseResult r = new Parser(scanner.scanTokens()).parse();
        assertTrue(!scanner.hasErrors() && r.isValid(), "programa deveria ser válido: " + r.getErrors());
        return r.getTree();
    }

    private static String read(String rel) throws IOException {
        return new String(Files.readAllBytes(root.resolve(rel)), StandardCharsets.UTF_8);
    }

    private static List<Path> list(String dir) throws IOException {
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
