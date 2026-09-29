# Especificação Léxica da Linguagem

**Disciplina:** Construção de Compiladores - GBC071
**Grupo:** João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

---

## 1. Visão geral

A linguagem é imperativa e estaticamente tipada. O escopo é:

- **Tipos básicos:** `int`, `double`, `bool`, `char` e `string`.
- **Controle:** `if`/`else`, `while` e `for`.
- **Sub-rotinas:** procedimentos e funções (`void` ou tipo básico), com parâmetros, chamada e `return` com ou sem valor.

O scanner produz tokens de sete categorias. Espaços e comentários não geram token. No fim da entrada é sempre emitido um token `EOF`.

---

## 2. Categorias de token

### 2.1 Identificador (`IDENTIFIER`)

```
identificador = ( letra | "_" ) { letra | digito | "_" }
letra         = "a".."z" | "A".."Z"
digito        = "0".."9"
Regex:  [a-zA-Z_][a-zA-Z0-9_]*
```

- **Exemplos válidos:** `total`, `x1`, `contaItens`, `_temp`, `val_max`
- **Case-sensitive:** sim (`total` ≠ `Total`).
- **`_`:** permitido no início e no corpo.
- **Tamanho máximo:** não há limite.
- **Maximal munch:** consome enquanto o próximo caractere for letra, dígito ou `_`. Só depois de ler o lexema completo consulta a tabela de palavras reservadas.

### 2.2 Palavra reservada

Mesmo padrão do identificador; após reconhecer o lexema, ele é buscado na tabela abaixo (comparação exata, apenas minúsculas). Se não estiver na tabela, o token é `IDENTIFIER`.

Lista fechada com 13 palavras:

| Palavra | TokenType | Categoria |
|---|---|---|
| `int` | `KW_INT` | Tipo básico |
| `double` | `KW_DOUBLE` | Tipo básico |
| `bool` | `KW_BOOL` | Tipo básico |
| `char` | `KW_CHAR` | Tipo básico |
| `string` | `KW_STRING` | Tipo básico |
| `void` | `KW_VOID` | Ausência de retorno |
| `if` | `KW_IF` | Controle |
| `else` | `KW_ELSE` | Controle |
| `while` | `KW_WHILE` | Controle |
| `for` | `KW_FOR` | Controle |
| `return` | `KW_RETURN` | Retorno de sub-rotina |
| `true` | `KW_TRUE` | Literal booleano |
| `false` | `KW_FALSE` | Literal booleano |

`IF`, `While` ou `Int` não são palavras reservadas e viram `IDENTIFIER`.

### 2.3 Literal numérico (`LIT_INT`, `LIT_DOUBLE`)

```
inteiro = digito+                    Regex: [0-9]+
double  = digito+ "." digito+        Regex: [0-9]+\.[0-9]+
```

- **Exemplos válidos:** `10`, `0`, `42` (`LIT_INT`); `3.14`, `100.0` (`LIT_DOUBLE`).
- O ponto exige dígito antes e depois. Zeros à esquerda são aceitos (`007`).
- Não há notação científica nem hexadecimal. O sinal `-` é sempre o operador `OP_MINUS`; não faz parte do literal.
- Valor numérico: `LIT_INT` é convertido para inteiro de 64 bits com sinal; `LIT_DOUBLE` para `double`. Um inteiro fora desse intervalo é erro léxico (Seção 6).
- **Casos de borda:**
  - `12.` (ponto sem dígito depois): o ponto é consumido, é reportado erro e é emitido um token `ERROR` com lexema `12.`.
  - `3.14.15`: gera `3.14`, depois erro de caractere inválido para `.` e depois `15`.
  - `.5`: `.` fora de número é caractere inválido; depois `5`.
  - `123abc`: gera `LIT_INT` `123` seguido de `IDENTIFIER` `abc`, sem erro (o literal termina onde deixa de haver dígito).

### 2.4 String (`LIT_STRING`)

```
string = '"' { [^"\\\n\r] | escape } '"'
escape = "\" ( '"' | "n" | "t" | "\" )
Regex:  "( [^"\\\n\r] | \\["nt\\] )*"
```

- **Exemplos válidos:** `"ok"`, `"linha 1"`, `"com \"escape\""`, `""`
- **Escapes válidos:** `\"`, `\n`, `\t`, `\\`. O lexema guarda o texto original; o valor guarda a string já com os escapes resolvidos.
- **Strings não são multilinha:** `\n` e `\r` literais não podem aparecer dentro da string. Uma barra invertida seguida de quebra de linha também não continua a string; conta como string não fechada antes do fim da linha.
- **Escape inválido** (ex.: `\q`): erro reportado, mas a string continua e o token `LIT_STRING` é emitido; o valor mantém o caractere depois da barra, sem a barra.
- O conteúdo aceita qualquer caractere além dos excluídos acima, inclusive acentuados e tabulação.
- **Não fechada até o fim da linha** ou **até o EOF:** erro léxico apontando a posição da aspa de abertura. Nenhum token é emitido; o scanner retoma no fim da linha (ou termina, no EOF).

### 2.5 Literal de caractere (`LIT_CHAR`)

```
char   = "'" ( [^\\\n] | escape_c ) "'"
escape_c = "\" ( "'" | "n" | "t" | "\" )
```

- **Exemplos válidos:** `'a'`, `'7'`, `'\n'`, `'\''`, `'\\'`
- **Escapes válidos:** `\'`, `\n`, `\t`, `\\`.
- Exatamente um caractere (ou um escape) entre apóstrofos. O valor do token é o caractere já resolvido.
- **Erros:**
  - `''` (vazio), `'ab'` e apóstrofo não fechado: erro "não fechado com apóstrofo"; nenhum token é emitido e o scanner continua a partir do caractere seguinte ao último consumido.
  - `'` seguido de fim de linha ou EOF: erro "vazio ou não fechado".
  - Escape inválido (ex.: `'\q'`): erro reportado, mas o token `LIT_CHAR` é emitido, com o caractere depois da barra como valor.

### 2.6 Operadores

Todos têm 1 ou 2 caracteres.

| Categoria | Lexema | TokenType |
|---|---|---|
| Atribuição | `=` | `OP_ASSIGN` |
| Aritméticos | `+` `-` `*` `/` `%` | `OP_PLUS` `OP_MINUS` `OP_MULT` `OP_DIV` `OP_MOD` |
| Relacionais | `==` `!=` `<` `<=` `>` `>=` | `OP_EQ` `OP_NE` `OP_LT` `OP_LE` `OP_GT` `OP_GE` |
| Lógicos | `&&` `\|\|` `!` | `OP_AND` `OP_OR` `OP_NOT` |

- **Formas compostas:** `==`, `!=`, `<=`, `>=`, `&&`, `||`. Desambiguação por maximal munch (ex.: `<=` é um token, não `<` seguido de `=`).
- `&` e `|` sozinhos **não** são operadores da linguagem: geram erro léxico (Seção 6).

### 2.7 Delimitadores

| Lexema | TokenType | | Lexema | TokenType |
|---|---|---|---|---|
| `(` | `LPAREN` | | `;` | `SEMICOLON` |
| `)` | `RPAREN` | | `,` | `COMMA` |
| `{` | `LBRACE` | | `[` | `LBRACKET` |
| `}` | `RBRACE` | | `]` | `RBRACKET` |

`[` e `]` são reconhecidos pelo scanner, mas o uso sintático (ex.: vetores) será definido na gramática.

---

## 3. Alfabeto de entrada

**Fora de strings, chars e comentários**, os caracteres válidos são:

| Grupo | Caracteres |
|---|---|
| Letras | `a`–`z`, `A`–`Z` |
| Dígitos | `0`–`9` |
| Sublinhado | `_` |
| Espaçamento | espaço, `\t`, `\r`, `\n` |
| Operadores | `+ - * / % = ! < > & \|` |
| Delimitadores | `( ) { } [ ] ; ,` |
| Delimitadores de literal | `"` (string) e `'` (char) |
| Ponto | `.`, somente dentro de literal `double` |
| Barra invertida | `\`, somente dentro de string ou char |

**Dentro de strings, chars e comentários**, qualquer caractere é aceito como conteúdo (inclusive acentuados), respeitadas as restrições das Seções 2.4, 2.5 e 5.

Qualquer outro caractere é inválido (ex.: `@ $ # ~ ^ ? : `` ` ``, `.` ou `\` soltos, caracteres não ASCII fora de string/char/comentário). Ele é reportado como erro léxico, **descartado**, e a análise continua no caractere seguinte.

---

## 4. Case-sensitivity

A linguagem é case-sensitive. Palavras reservadas só valem em minúsculas; identificadores diferenciam maiúsculas de minúsculas (`total` ≠ `Total`). `IF` e `While` são identificadores comuns.

---

## 5. Espaços em branco, comentários e posições

- **Espaços:** ` `, `\t`, `\r` e `\n` separam tokens e não geram token.
- **Comentário de linha:** começa em `//` e vai até antes do próximo `\n` (ou até o EOF). Não gera token.
- **Comentário de bloco:** começa em `/*` e termina no primeiro `*/`. Pode ter várias linhas. **Não é aninhado:** o primeiro `*/` fecha o comentário. Em `/*/`, o `*` da abertura não é reaproveitado para fechar. Não gera token.
- **Bloco não fechado até o EOF:** erro léxico apontando a posição do `/*`; todo o texto restante é descartado.
- **Posição (linha, coluna):** ambas começam em 1. `\n` incrementa a linha e volta a coluna para 1; qualquer outro caractere (inclusive `\t` e `\r`) avança a coluna em 1. A posição de um token é a de seu primeiro caractere. O token `EOF` recebe a posição logo após o último caractere.
- **Leitura de arquivo:** o `Main` normaliza as quebras de linha para `\n` e garante um `\n` final.

---

## 6. Maximal munch

O scanner sempre consome o maior prefixo válido antes de decidir o token:

- Operadores: `==` antes de `=`, `<=` antes de `<`, `>=` antes de `>`, `!=` antes de `!`, `&&` e `||` só na forma dupla.
- Comentários: `//` e `/*` têm prioridade sobre `/` (divisão).
- Identificadores e números: consomem enquanto o próximo caractere continuar o padrão.

---

## 7. Tratamento de erros

O scanner **nunca interrompe a análise**: registra o erro (linha e coluna) e continua tokenizando o restante do arquivo. Todos os erros ficam disponíveis ao final, em ordem de ocorrência.

| Situação | Mensagem | Posição | Recuperação |
|---|---|---|---|
| Caractere fora do alfabeto | `Caractere inválido fora do alfabeto: 'X' (ASCII N)` | o caractere | descarta o caractere |
| `&` sozinho | `Caractere inesperado '&'. Esperado '&&' para operador lógico AND` | o `&` | descarta o `&` |
| `\|` sozinho | `Caractere inesperado '\|'. Esperado '\|\|' para operador lógico OR` | o `\|` | descarta o `\|` |
| Número `12.` | `Número decimal mal formatado: esperado ao menos um dígito após o ponto '.'` | início do número | emite token `ERROR` |
| Inteiro fora de 64 bits | `Literal inteiro fora do intervalo de 64 bits` | início do número | emite token `ERROR` |
| String não fechada (fim de linha) | `String não fechada antes do fim da linha` | aspa de abertura | sem token; retoma no fim da linha |
| String não fechada (EOF) | `String não fechada até o fim do arquivo (EOF)` | aspa de abertura | sem token; fim da análise |
| Escape inacabado em string (EOF) | `Sequência de escape inacabada até o fim do arquivo (EOF)` | aspa de abertura | sem token |
| Escape inválido em string | `Sequência de escape inválida '\X'` | aspa de abertura | mantém a string e emite o token |
| Char vazio ou sem fechar (fim de linha/EOF) | `Literal de char vazio ou não fechado` | apóstrofo de abertura | sem token |
| Escape em char inacabado (EOF) | `Escape em char não fechado até EOF` | apóstrofo de abertura | sem token |
| Char sem apóstrofo de fechamento | `Literal de char não fechado com apóstrofo` | apóstrofo de abertura | sem token |
| Escape inválido em char | `Sequência de escape inválida em char '\X'` | apóstrofo de abertura | mantém o char e emite o token |
| Comentário de bloco não fechado (EOF) | `Comentário de bloco '/*' não fechado até o fim do arquivo (EOF)` | o `/*` | descarta o restante |

Assim, o usuário vê todos os problemas léxicos do arquivo de uma vez, em vez de parar no primeiro.
