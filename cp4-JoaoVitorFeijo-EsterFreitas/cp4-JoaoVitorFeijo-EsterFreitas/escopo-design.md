# Checkpoint 4 — Tabela de Símbolos e Passada de Identificação

**Grupo:** João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

## 1. Ambiente

Programa (`test/programas/cp4/obrig1_valido_sombreamento.txt`):

```
 1  int x = 1;
 2  int y = 2;
 3  int soma(int a) {
 4      int r = a + x;
 5      {
 6          int x = r * 2;
 7          {
 8              int y = x + a;
 9              r = y;
10          }
11          r = r + y;
12      }
13      return r + x;
14  }
15  void main() {
16      y = soma(y);
17  }
```

`G` = `{ soma@3, main@15, x@1, y@2 }`.

| Ponto | Ambiente (interno :: externo) |
|-|-|
| abre o global | `{ }` |
| declara `soma` (passada prévia) | `{ soma@3 }` |
| declara `main` (passada prévia) | `{ soma@3, main@15 }` |
| depois da linha 1 | `{ soma@3, main@15, x@1 }` |
| depois da linha 2 | `G` |
| linha 3, parâmetro `a` | `{ a@3 } :: G` |
| depois da linha 4 | `{ a@3, r@4 } :: G` |
| linha 5, abre bloco | `{ } :: { a@3, r@4 } :: G` |
| depois da linha 6 | `{ x@6 } :: { a@3, r@4 } :: G` |
| linha 7, abre bloco | `{ } :: { x@6 } :: { a@3, r@4 } :: G` |
| depois da linha 8 | `{ y@8 } :: { x@6 } :: { a@3, r@4 } :: G` |
| linha 10, fecha bloco | `{ x@6 } :: { a@3, r@4 } :: G` |
| linha 12, fecha bloco | `{ a@3, r@4 } :: G` |
| linha 14, fecha `soma` | `G` |
| linha 15, abre `main` | `{ } :: G` |
| linha 17, fecha `main` | `G` |
| fecha o global | (vazio) |

| Linha | Usos |
|-|-|
| 4 | `a` → a@3, `x` → x@1 |
| 6 | `r` → r@4 |
| 8 | `x` → x@6, `a` → a@3 |
| 9 | `r` → r@4, `y` → y@8 |
| 11 | `r` → r@4, `r` → r@4, `y` → y@2 |
| 13 | `r` → r@4, `x` → x@1 |
| 16 | `y` → y@2, `soma` → soma@3, `y` → y@2 |

Saída da passada conferida em `test/esperado/cp4/obrig1_valido_sombreamento.txt`.

## 2. Symbol

| Campo | Fase que pergunta |
|-|-|
| `name` | identificação (tabela, mensagens) |
| `kind` (`VAR`, `PARAM`, `PROC`, `FUNC`) | checagem de tipos (uso compatível com a categoria) |
| `decl` (`VarDecl`, `Param`, `ProcDecl`, `FuncDecl`) | identificação (linha da primeira declaração); checagem de tipos (tipo declarado, parâmetros, tipo de retorno) |
| `level` | identificação; geração de código (global × local) |
| `type` (reservado, `null`) | Aulas 12–19 |
| `offset` (reservado, `-1`) | Aulas 12–19 |

Nada acrescentado: a linguagem não tem `const` nem passagem por referência.

O campo `decl` foi acrescentado em `Id`, `Assign` e `Call`, porque na AST do CP3 esses são os três nós que guardam um identificador de uso: variável em expressão, alvo de atribuição e rotina chamada.

## 3. Implementação

Pilha de mapas (`StackSymbolTable`). Lookup proporcional à profundidade, que é pequena; open/close constantes; não precisamos guardar escopos fechados, pois a informação fica na AST decorada.

- `closeScope` sem escopo aberto: `IllegalStateException("closeScope sem escopo aberto")`.
- `declare` sem escopo aberto: `IllegalStateException`; `declare(null)`: `IllegalArgumentException`.
- `level()` sem escopo aberto: `-1`.

## 4. Decisões de projeto

### 4.1 Sombreamento
- **Escolha:** um nome interno pode ocultar um externo; redeclarar no mesmo escopo é sempre erro.
- **Descartada:** proibir sombreamento de locais (Java).
- **Motivo:** segue C.
- **Teste:** `dec1_sombreamento.txt`.

### 4.2 Declarar antes de usar
- **Escolha:** variáveis e parâmetros, sim, e a inicialização é visitada antes de declarar o nome. Procedimentos e funções globais são visíveis no programa inteiro (passada prévia).
- **Descartada:** todos os nomes visíveis no bloco inteiro.
- **Motivo:** a gramática não tem protótipos; para variáveis, a ordem do texto basta.
- **Teste:** `dec2_declarar_antes_de_usar.txt`.

### 4.3 Funções mutuamente recursivas
- **Escolha:** permitidas, pela passada prévia que declara todas as rotinas globais.
- **Descartada:** declaração antecipada (exigiria mudar a gramática).
- **Motivo:** é o único meio sem alterar o parser.
- **Teste:** `dec3_mutua_recursao.txt`; recursão direta em `recursivo.txt`.

### 4.4 Parâmetros e corpo
- **Escolha:** mesmo escopo; `void f(int a) { int a; }` é redeclaração.
- **Descartada:** corpo com escopo próprio.
- **Motivo:** assinatura e declarações iniciais são a mesma região (C).
- **Teste:** `dec4_parametros_corpo.txt`.

### 4.5 Espaços de nomes
- **Escolha:** um só; `int f; void f() {}` é redeclaração.
- **Descartada:** espaços separados para variáveis e rotinas.
- **Motivo:** uma tabela e uma regra, como C.
- **Teste:** `dec5_espaco_de_nomes.txt`.

## 5. Formato dos erros

```
linha L: 'x' já declarado neste escopo (linha M)
linha L: 'x' não declarado
```

Ordenados por linha e, na mesma linha, por coluna.
