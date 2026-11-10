# Gramática da Linguagem: FIRST, FOLLOW e verificação LL(1)

**Disciplina:** Construção de Compiladores - GBC071 (Checkpoint 2)
**Grupo:** João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

Este documento complementa a especificação léxica do CP1: os **terminais** são exatamente os `TokenType` produzidos pelo scanner (nomes em maiúsculas, como `IDENTIFIER`, `OP_ASSIGN`, `KW_IF`), mais o token `EOF`. `ε` é a produção vazia e `$` o fim da entrada.

---

## 0. Decisões de projeto da sintaxe

- **Programa:** sequência de declarações de nível superior: variáveis globais e funções/procedimentos.
- **Função:** `tipo nome(parâmetros) { ... }`. **Procedimento:** `void nome(parâmetros) { ... }`. Parâmetros são `tipo nome`, separados por vírgula. `return` pode ter valor (`return x;`) ou não (`return;`); a coerência entre o tipo de retorno e o `return` é verificação semântica, fora deste checkpoint.
- **Corpo de `if`, `else`, `while` e `for` é sempre um bloco `{ ... }`** (chaves obrigatórias). Isso é o que torna a gramática LL(1) (ver 1.2c). `else if` é permitido.
- **Atribuição e chamada são comandos** terminados por `;`. A chamada também pode aparecer dentro de expressões. A atribuição não é expressão.
- **`for (init; condição; passo) bloco`**, com as três partes opcionais.
- Variáveis locais podem ser declaradas em qualquer ponto de um bloco.
- Os tokens `[` e `]` são reconhecidos pelo scanner, mas não são usados pela gramática (não há vetores no escopo).

| Precedência (menor → maior) | Operadores | Associatividade |
|---|---|---|
| 1 | `\|\|` | esquerda |
| 2 | `&&` | esquerda |
| 3 | `==` `!=` | esquerda |
| 4 | `<` `<=` `>` `>=` | esquerda |
| 5 | `+` `-` | esquerda |
| 6 | `*` `/` `%` | esquerda |
| 7 | `!` `-` (unários) | direita |

A forma sem recursão à esquerda (Seção 1.2a) não muda a associatividade pretendida; ela será aplicada na construção da AST, no próximo ciclo.

---

## 1. Gramática final

### 1.1 Produções (BNF, sem recursão à esquerda, fatorada)

**Programa e declarações**

```
Program → DeclList EOF
DeclList → Declaration DeclList
         | ε
Declaration → Type IDENTIFIER DeclTail
            | KW_VOID IDENTIFIER FuncTail
DeclTail → FuncTail
         | VarTail
FuncTail → LPAREN ParamListOpt RPAREN Block
VarTail → InitOpt SEMICOLON
InitOpt → OP_ASSIGN Expr
        | ε
ParamListOpt → ParamList
             | ε
ParamList → Param ParamListTail
ParamListTail → COMMA Param ParamListTail
              | ε
Param → Type IDENTIFIER
Type → KW_INT
     | KW_DOUBLE
     | KW_BOOL
     | KW_CHAR
     | KW_STRING
```

**Blocos e comandos**

```
Block → LBRACE StmtList RBRACE
StmtList → Stmt StmtList
         | ε
Stmt → VarDecl
     | IfStmt
     | WhileStmt
     | ForStmt
     | ReturnStmt
     | Block
     | AssignOrCall
VarDecl → Type IDENTIFIER InitOpt SEMICOLON
AssignOrCall → IDENTIFIER AssignOrCallTail SEMICOLON
AssignOrCallTail → OP_ASSIGN Expr
                 | LPAREN ArgListOpt RPAREN
IfStmt → KW_IF LPAREN Expr RPAREN Block ElsePart
ElsePart → KW_ELSE ElseBody
         | ε
ElseBody → IfStmt
         | Block
WhileStmt → KW_WHILE LPAREN Expr RPAREN Block
ForStmt → KW_FOR LPAREN ForInit SEMICOLON ForCond SEMICOLON ForStep RPAREN Block
ForInit → Type IDENTIFIER InitOpt
        | IDENTIFIER OP_ASSIGN Expr
        | ε
ForCond → Expr
        | ε
ForStep → IDENTIFIER OP_ASSIGN Expr
        | ε
ReturnStmt → KW_RETURN ReturnVal SEMICOLON
ReturnVal → Expr
          | ε
```

**Expressões (da menor para a maior precedência: ||, &&, ==/!=, relacionais, +/-, */%, unários)**

```
Expr → OrExpr
OrExpr → AndExpr OrRest
OrRest → OP_OR AndExpr OrRest
       | ε
AndExpr → EqExpr AndRest
AndRest → OP_AND EqExpr AndRest
        | ε
EqExpr → RelExpr EqRest
EqRest → OP_EQ RelExpr EqRest
       | OP_NE RelExpr EqRest
       | ε
RelExpr → AddExpr RelRest
RelRest → OP_LT AddExpr RelRest
        | OP_LE AddExpr RelRest
        | OP_GT AddExpr RelRest
        | OP_GE AddExpr RelRest
        | ε
AddExpr → MulExpr AddRest
AddRest → OP_PLUS MulExpr AddRest
        | OP_MINUS MulExpr AddRest
        | ε
MulExpr → Unary MulRest
MulRest → OP_MULT Unary MulRest
        | OP_DIV Unary MulRest
        | OP_MOD Unary MulRest
        | ε
Unary → OP_NOT Unary
      | OP_MINUS Unary
      | Primary
Primary → LIT_INT
        | LIT_DOUBLE
        | LIT_STRING
        | LIT_CHAR
        | KW_TRUE
        | KW_FALSE
        | LPAREN Expr RPAREN
        | IDENTIFIER PrimaryTail
PrimaryTail → LPAREN ArgListOpt RPAREN
            | ε
ArgListOpt → ArgList
           | ε
ArgList → Expr ArgListTail
ArgListTail → COMMA Expr ArgListTail
            | ε
```
Não há recursão à esquerda, direta ou indireta (um não-terminal só chama a si mesmo, ou a um ciclo, depois de consumir um terminal).

### 1.2 Transformações aplicadas na construção da gramática

**(a) Recursão à esquerda nas expressões.** A forma natural das expressões binárias é recursiva à esquerda e faria um parser descendente recursivo entrar em recursão infinita:

```
ANTES                                            DEPOIS
OrExpr  → OrExpr OP_OR AndExpr | AndExpr         OrExpr  → AndExpr OrRest
AddExpr → AddExpr OP_PLUS MulExpr                OrRest  → OP_OR AndExpr OrRest | ε
        | AddExpr OP_MINUS MulExpr | MulExpr     AddExpr → MulExpr AddRest
                                                 AddRest → OP_PLUS MulExpr AddRest
                                                         | OP_MINUS MulExpr AddRest | ε
```

O mesmo esquema vale para `AndExpr`, `EqExpr`, `RelExpr` e `MulExpr` (cada nível ganha um não-terminal `...Rest`).

**(b) Fatoração à esquerda.** Havia prefixos comuns entre alternativas:

```
ANTES
Declaration → Type IDENTIFIER LPAREN ParamListOpt RPAREN Block      (função)
            | Type IDENTIFIER InitOpt SEMICOLON                     (variável global)
            | KW_VOID IDENTIFIER LPAREN ParamListOpt RPAREN Block
Stmt        → IDENTIFIER OP_ASSIGN Expr SEMICOLON | IDENTIFIER LPAREN ArgListOpt RPAREN SEMICOLON | ...
Primary     → IDENTIFIER | IDENTIFIER LPAREN ArgListOpt RPAREN | ...

DEPOIS
Declaration  → Type IDENTIFIER DeclTail | KW_VOID IDENTIFIER FuncTail
DeclTail     → FuncTail | VarTail
AssignOrCall → IDENTIFIER AssignOrCallTail SEMICOLON
AssignOrCallTail → OP_ASSIGN Expr | LPAREN ArgListOpt RPAREN
Primary      → ... | IDENTIFIER PrimaryTail          PrimaryTail → LPAREN ArgListOpt RPAREN | ε
```

**(c) Violação da condição LL(1) encontrada e corrigida: o *dangling else*.** A primeira forma natural do `if` deixava o corpo ser um comando qualquer:

```
ANTES                                            DEPOIS
IfStmt   → KW_IF LPAREN Expr RPAREN Stmt         IfStmt   → KW_IF LPAREN Expr RPAREN Block ElsePart
           ElsePart                              ElsePart → KW_ELSE ElseBody | ε
ElsePart → KW_ELSE Stmt | ε                      ElseBody → IfStmt | Block
```

Com a forma "ANTES", como `Stmt` pode ser ele próprio um `IfStmt`, um `else` pode seguir um `Stmt` interno, logo `KW_ELSE ∈ FOLLOW(ElsePart)`. Mas `KW_ELSE` também é o FIRST da alternativa não anulável, e `FIRST(KW_ELSE Stmt) ∩ FOLLOW(ElsePart) = { KW_ELSE } ≠ ∅`. **A cláusula (c) é violada** (e a gramática é ambígua: a qual `if` pertence o `else`?). A correção foi exigir **bloco** como corpo (e `else if` via `ElseBody`). Como `Block` termina em `}`, `FOLLOW(ElsePart) = FOLLOW(Stmt)` deixa de conter `KW_ELSE`, e a interseção vira vazia (Seção 3).

### 1.3 Exemplo de programa da linguagem

```
int total = 0;

int fatorial(int n) {
    int r = 1;
    while (n > 1) { r = r * n; n = n - 1; }
    return r;
}

void principal() {
    for (int i = 0; i < 5; i = i + 1) {
        if (i % 2 == 0) { total = total + fatorial(i); } else { total = total - 1; }
    }
}
```

---

## 2. FIRST e FOLLOW de todo não-terminal

Abreviações usadas nas tabelas (conjuntos que se repetem):

- `INICIO_STMT` = { IDENTIFIER, KW_BOOL, KW_CHAR, KW_DOUBLE, KW_FOR, KW_IF, KW_INT, KW_RETURN, KW_STRING, KW_WHILE, LBRACE }
- `INICIO_EXPR` = { IDENTIFIER, KW_FALSE, KW_TRUE, LIT_CHAR, LIT_DOUBLE, LIT_INT, LIT_STRING, LPAREN, OP_MINUS, OP_NOT }
- `PRIMARIOS` = { IDENTIFIER, KW_FALSE, KW_TRUE, LIT_CHAR, LIT_DOUBLE, LIT_INT, LIT_STRING, LPAREN }
- `TIPOS` = { KW_BOOL, KW_CHAR, KW_DOUBLE, KW_INT, KW_STRING }
- `FIM_EXPR` = { COMMA, RPAREN, SEMICOLON }
- `OPS_MUL` = { OP_DIV, OP_MOD, OP_MULT }
- `OPS_ADD` = { OP_MINUS, OP_PLUS }
- `OPS_REL` = { OP_GE, OP_GT, OP_LE, OP_LT }
- `OPS_IGUAL` = { OP_EQ, OP_NE }

| Não-terminal | FIRST | FOLLOW |
|---|---|---|
| `Program` | { TIPOS, EOF, KW_VOID } | { $ } |
| `DeclList` | { TIPOS, KW_VOID, ε } | { EOF } |
| `Declaration` | { TIPOS, KW_VOID } | { TIPOS, EOF, KW_VOID } |
| `DeclTail` | { LPAREN, OP_ASSIGN, SEMICOLON } | { TIPOS, EOF, KW_VOID } |
| `FuncTail` | { LPAREN } | { TIPOS, EOF, KW_VOID } |
| `VarTail` | { OP_ASSIGN, SEMICOLON } | { TIPOS, EOF, KW_VOID } |
| `InitOpt` | { OP_ASSIGN, ε } | { SEMICOLON } |
| `ParamListOpt` | { TIPOS, ε } | { RPAREN } |
| `ParamList` | { TIPOS } | { RPAREN } |
| `ParamListTail` | { COMMA, ε } | { RPAREN } |
| `Param` | { TIPOS } | { COMMA, RPAREN } |
| `Type` | { TIPOS } | { IDENTIFIER } |
| `Block` | { LBRACE } | { INICIO_STMT, EOF, KW_ELSE, KW_VOID, RBRACE } |
| `StmtList` | { INICIO_STMT, ε } | { RBRACE } |
| `Stmt` | { INICIO_STMT } | { INICIO_STMT, RBRACE } |
| `VarDecl` | { TIPOS } | { INICIO_STMT, RBRACE } |
| `AssignOrCall` | { IDENTIFIER } | { INICIO_STMT, RBRACE } |
| `AssignOrCallTail` | { LPAREN, OP_ASSIGN } | { SEMICOLON } |
| `IfStmt` | { KW_IF } | { INICIO_STMT, RBRACE } |
| `ElsePart` | { KW_ELSE, ε } | { INICIO_STMT, RBRACE } |
| `ElseBody` | { KW_IF, LBRACE } | { INICIO_STMT, RBRACE } |
| `WhileStmt` | { KW_WHILE } | { INICIO_STMT, RBRACE } |
| `ForStmt` | { KW_FOR } | { INICIO_STMT, RBRACE } |
| `ForInit` | { TIPOS, IDENTIFIER, ε } | { SEMICOLON } |
| `ForCond` | { INICIO_EXPR, ε } | { SEMICOLON } |
| `ForStep` | { IDENTIFIER, ε } | { RPAREN } |
| `ReturnStmt` | { KW_RETURN } | { INICIO_STMT, RBRACE } |
| `ReturnVal` | { INICIO_EXPR, ε } | { SEMICOLON } |
| `Expr` | { INICIO_EXPR } | { FIM_EXPR } |
| `OrExpr` | { INICIO_EXPR } | { FIM_EXPR } |
| `OrRest` | { OP_OR, ε } | { FIM_EXPR } |
| `AndExpr` | { INICIO_EXPR } | { FIM_EXPR, OP_OR } |
| `AndRest` | { OP_AND, ε } | { FIM_EXPR, OP_OR } |
| `EqExpr` | { INICIO_EXPR } | { FIM_EXPR, OP_AND, OP_OR } |
| `EqRest` | { OPS_IGUAL, ε } | { FIM_EXPR, OP_AND, OP_OR } |
| `RelExpr` | { INICIO_EXPR } | { FIM_EXPR, OPS_IGUAL, OP_AND, OP_OR } |
| `RelRest` | { OPS_REL, ε } | { FIM_EXPR, OPS_IGUAL, OP_AND, OP_OR } |
| `AddExpr` | { INICIO_EXPR } | { FIM_EXPR, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `AddRest` | { OPS_ADD, ε } | { FIM_EXPR, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `MulExpr` | { INICIO_EXPR } | { FIM_EXPR, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `MulRest` | { OPS_MUL, ε } | { FIM_EXPR, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `Unary` | { INICIO_EXPR } | { FIM_EXPR, OPS_MUL, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `Primary` | { PRIMARIOS } | { FIM_EXPR, OPS_MUL, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `PrimaryTail` | { LPAREN, ε } | { FIM_EXPR, OPS_MUL, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } |
| `ArgListOpt` | { INICIO_EXPR, ε } | { RPAREN } |
| `ArgList` | { INICIO_EXPR } | { RPAREN } |
| `ArgListTail` | { COMMA, ε } | { RPAREN } |

`FOLLOW(Program) = { $ }`. `EOF` é um terminal consumido por `Program`; por isso `FOLLOW(DeclList) = { EOF }`.

---

## 3. Verificação da condição LL(1), não-terminal por não-terminal

Para cada não-terminal A com alternativas α₁ | α₂ | … | αₖ:

- **(a)** os `FIRST(αᵢ)` são dois a dois disjuntos;
- **(b)** no máximo uma alternativa é anulável (deriva ε);
- **(c)** se alguma alternativa é anulável, `FIRST(αⱼ) ∩ FOLLOW(A) = ∅` para toda alternativa αⱼ não anulável.

A gramática tem **47 não-terminais**, dos quais 27 têm mais de uma alternativa e 19 são anuláveis. Nos de uma única alternativa, (a) e (c) valem trivialmente.

| Não-terminal | FIRST de cada alternativa | (a) FIRST disjuntos | (b) ≤ 1 anulável | (c) FIRST(não anuláveis) ∩ FOLLOW |
|---|---|---|---|---|
| `Program` | 1 alternativa: { TIPOS, EOF, KW_VOID } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `DeclList` | 1) { TIPOS, KW_VOID }<br>2) { ε } | ✓ | ✓ | { TIPOS, KW_VOID } ∩ { EOF } = ∅ ✓ |
| `Declaration` | 1) { TIPOS }<br>2) { KW_VOID } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `DeclTail` | 1) { LPAREN }<br>2) { OP_ASSIGN, SEMICOLON } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `FuncTail` | 1 alternativa: { LPAREN } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `VarTail` | 1 alternativa: { OP_ASSIGN, SEMICOLON } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `InitOpt` | 1) { OP_ASSIGN }<br>2) { ε } | ✓ | ✓ | { OP_ASSIGN } ∩ { SEMICOLON } = ∅ ✓ |
| `ParamListOpt` | 1) { TIPOS }<br>2) { ε } | ✓ | ✓ | { TIPOS } ∩ { RPAREN } = ∅ ✓ |
| `ParamList` | 1 alternativa: { TIPOS } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `ParamListTail` | 1) { COMMA }<br>2) { ε } | ✓ | ✓ | { COMMA } ∩ { RPAREN } = ∅ ✓ |
| `Param` | 1 alternativa: { TIPOS } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `Type` | 1) { KW_INT }<br>2) { KW_DOUBLE }<br>3) { KW_BOOL }<br>4) { KW_CHAR }<br>5) { KW_STRING } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `Block` | 1 alternativa: { LBRACE } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `StmtList` | 1) { INICIO_STMT }<br>2) { ε } | ✓ | ✓ | { INICIO_STMT } ∩ { RBRACE } = ∅ ✓ |
| `Stmt` | 1) { TIPOS }<br>2) { KW_IF }<br>3) { KW_WHILE }<br>4) { KW_FOR }<br>5) { KW_RETURN }<br>6) { LBRACE }<br>7) { IDENTIFIER } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `VarDecl` | 1 alternativa: { TIPOS } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `AssignOrCall` | 1 alternativa: { IDENTIFIER } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `AssignOrCallTail` | 1) { OP_ASSIGN }<br>2) { LPAREN } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `IfStmt` | 1 alternativa: { KW_IF } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `ElsePart` | 1) { KW_ELSE }<br>2) { ε } | ✓ | ✓ | { KW_ELSE } ∩ { INICIO_STMT, RBRACE } = ∅ ✓ |
| `ElseBody` | 1) { KW_IF }<br>2) { LBRACE } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `WhileStmt` | 1 alternativa: { KW_WHILE } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `ForStmt` | 1 alternativa: { KW_FOR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `ForInit` | 1) { TIPOS }<br>2) { IDENTIFIER }<br>3) { ε } | ✓ | ✓ | { TIPOS, IDENTIFIER } ∩ { SEMICOLON } = ∅ ✓ |
| `ForCond` | 1) { INICIO_EXPR }<br>2) { ε } | ✓ | ✓ | { INICIO_EXPR } ∩ { SEMICOLON } = ∅ ✓ |
| `ForStep` | 1) { IDENTIFIER }<br>2) { ε } | ✓ | ✓ | { IDENTIFIER } ∩ { RPAREN } = ∅ ✓ |
| `ReturnStmt` | 1 alternativa: { KW_RETURN } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `ReturnVal` | 1) { INICIO_EXPR }<br>2) { ε } | ✓ | ✓ | { INICIO_EXPR } ∩ { SEMICOLON } = ∅ ✓ |
| `Expr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `OrExpr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `OrRest` | 1) { OP_OR }<br>2) { ε } | ✓ | ✓ | { OP_OR } ∩ { FIM_EXPR } = ∅ ✓ |
| `AndExpr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `AndRest` | 1) { OP_AND }<br>2) { ε } | ✓ | ✓ | { OP_AND } ∩ { FIM_EXPR, OP_OR } = ∅ ✓ |
| `EqExpr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `EqRest` | 1) { OP_EQ }<br>2) { OP_NE }<br>3) { ε } | ✓ | ✓ | { OPS_IGUAL } ∩ { FIM_EXPR, OP_AND, OP_OR } = ∅ ✓ |
| `RelExpr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `RelRest` | 1) { OP_LT }<br>2) { OP_LE }<br>3) { OP_GT }<br>4) { OP_GE }<br>5) { ε } | ✓ | ✓ | { OPS_REL } ∩ { FIM_EXPR, OPS_IGUAL, OP_AND, OP_OR } = ∅ ✓ |
| `AddExpr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `AddRest` | 1) { OP_PLUS }<br>2) { OP_MINUS }<br>3) { ε } | ✓ | ✓ | { OPS_ADD } ∩ { FIM_EXPR, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } = ∅ ✓ |
| `MulExpr` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `MulRest` | 1) { OP_MULT }<br>2) { OP_DIV }<br>3) { OP_MOD }<br>4) { ε } | ✓ | ✓ | { OPS_MUL } ∩ { FIM_EXPR, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } = ∅ ✓ |
| `Unary` | 1) { OP_NOT }<br>2) { OP_MINUS }<br>3) { PRIMARIOS } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `Primary` | 1) { LIT_INT }<br>2) { LIT_DOUBLE }<br>3) { LIT_STRING }<br>4) { LIT_CHAR }<br>5) { KW_TRUE }<br>6) { KW_FALSE }<br>7) { LPAREN }<br>8) { IDENTIFIER } | ✓ | ✓ | ✓ (nenhuma anulável) |
| `PrimaryTail` | 1) { LPAREN }<br>2) { ε } | ✓ | ✓ | { LPAREN } ∩ { FIM_EXPR, OPS_MUL, OPS_ADD, OPS_REL, OPS_IGUAL, OP_AND, OP_OR } = ∅ ✓ |
| `ArgListOpt` | 1) { INICIO_EXPR }<br>2) { ε } | ✓ | ✓ | { INICIO_EXPR } ∩ { RPAREN } = ∅ ✓ |
| `ArgList` | 1 alternativa: { INICIO_EXPR } | ✓ (trivial) | ✓ | ✓ (trivial) |
| `ArgListTail` | 1) { COMMA }<br>2) { ε } | ✓ | ✓ | { COMMA } ∩ { RPAREN } = ∅ ✓ |

**Resultado:** nenhum não-terminal viola (a), (b) ou (c). A gramática entregue é LL(1) de ponta a ponta. O caso que exige atenção é `ElsePart`, que só satisfaz (c) por causa da correção da Seção 1.2c.


---

## 4. Do documento ao código (`src/Parser.java`)

- **Um método por não-terminal:** `parseX()` para cada `X` da Seção 1.1, com o mesmo nome.
- **Escolha da alternativa:** pelo lookahead, usando os conjuntos `FIRST_*` (`FIRST_TYPE`, `FIRST_EXPR`, `FIRST_STMT`, …), que são os FIRST das tabelas acima.
- **ε robusto:** uma alternativa vazia só é aceita se o lookahead está em `FOLLOW(X)` (conjuntos `FOLLOW_*`); caso contrário é erro, detectado já no não-terminal anulável.
- **Recuperação em modo pânico:** ao detectar erro em `parseX()`, o erro é registrado (linha e coluna) e tokens são descartados até o conjunto de sincronização de `X`: **`FOLLOW(X)` ∪ âncoras `;`, `{`, `}`** (ou `EOF`). Particularidades:
  - nos comandos, um `;` onde a sincronização para é consumido, pois encerra o comando quebrado;
  - em cabeçalhos quebrados de função/`if`/`while`/`for`, o parser procura o `{` do corpo e continua dentro dele (`recoverHeader`);
  - em `DeclList` e `StmtList`, o descarte para em início de declaração/comando, permitindo continuar a lista;
  - dois erros no mesmo token não são reportados duas vezes.
- **Sem loop infinito:** toda recursão que forma laço (`DeclList`, `StmtList`, `ParamListTail`, `ArgListTail`, `*Rest`) só reentra após consumir um token ou após uma sincronização que descarta o token ofensor.
- **Sem AST:** `parse()` devolve apenas aceito/rejeitado e a lista de erros (`getErrors()`).
- **Scanner:** reaproveitado sem nenhuma alteração. Tokens `ERROR` emitidos pelo scanner (ex.: `12.`) aparecem ao parser como token inesperado.
