## Especificação Léxica 

**Disciplina:** Construção de Compiladores -GBC071
**Autores:** João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

---

## 1. Visão Geral e Escopo da Linguagem

- **Tipos básicos:** `int`, `double`, `bool`, `char` e `string`.
- **Estruturas de controle:** condicional (`if` / `else`) e repetição (`while` e `for`).
- **Funções:** declaração com tipos de retorno (`void` ou outros tipos), parâmetros por valor, chamadas e retorno (`return`) com ou sem expressão.

---

## 2. Questões Obrigatórias

### 2.1. Qual é o alfabeto de entrada?

O alfabeto de entrada da linguagem é composto pelos caracteres do conjunto ASCII imprimível (códigos de 32 a 126 inclusive), acrescido dos caracteres de controle de espaçamento:

| Caractere | Descrição | Código ASCII |
|---|---|---|
| ` ` | Espaço em branco | 32 |
| `\t` | Tabulação horizontal | 9 |
| `\n` | Quebra de linha (LF) | 10 |
| `\r` | Retorno de carro (CR) | 13 |

**Nota de Tratamento:** Caracteres fora deste domínio (ex.: `@`, `$`, `~`, `^`, caracteres binários) disparam erro léxico reportado com linha e coluna exatas, sendo descartados na recuperação para que o restante do arquivo continue a ser analisado.

### 2.2. A linguagem é case-sensitive?

**Sim.** A linguagem é estritamente sensível a maiúsculas e minúsculas (*case-sensitive*). Por exemplo, os identificadores `total`, `Total` e `TOTAL` representam símbolos distintos.

- **Palavras reservadas:** Devem ser grafadas obrigatoriamente em minúsculas (ex.: `int`, `if`, `while`, `return`). Escrever `IF` ou `While` fará com que sejam interpretados como identificadores comuns e não como palavras-chave.
- **Identificadores:** Seguem a mesma sensibilidade de caixa, permitindo letras maiúsculas e minúsculas (`[a-zA-Z]`).

### 2.3. Como são delimitados espaços em branco e comentários?

- **Espaços em branco:** Sequências de `' '`, `'\t'`, `'\r'` e `'\n'` funcionam exclusivamente como separadores de tokens e delimitadores léxicos. Não geram tokens para o analisador sintático, mas atualizam os contadores de linha e coluna do cursor.
- **Comentários de linha:** Iniciam com a sequência `//` e estendem-se até o primeiro caractere `\n` encontrado ou até o fim de arquivo (EOF). Não geram tokens.
- **Comentários de bloco:** Iniciam com `/*` e terminam com `*/`. Podem abranger múltiplas linhas. Não é permitido o aninhamento de comentários de bloco (o primeiro `*/` fecha o bloco corrente, seguindo a semântica de C e Java). Se o arquivo terminar (EOF) antes do fechamento com `*/`, o analisador reporta erro léxico, informando a linha e coluna de início do comentário.

### 2.4. Regra de desambiguação ("Maximal Munch")

Para operadores simples vs. compostos e comentários, aplica-se estritamente a regra do **Maximal Munch** (*longest match*):

> O scanner consome sempre o prefixo válido mais longo possível antes de emitir o token.

**Exemplos práticos:**

- Ao encontrar `=`, o autômato consulta o próximo caractere (`peek()`):
  - Se for `=`, consome e produz o token `OP_EQ` (`==`).
  - Se for qualquer outro caractere, produz `OP_ASSIGN` (`=`).
- Ao encontrar `<`, se seguido de `=`, produz `OP_LE` (`<=`); senão, `OP_LT` (`<`).
- Ao encontrar `>`, se seguido de `=`, produz `OP_GE` (`>=`); senão, `OP_GT` (`>`).
- Ao encontrar `!`, se seguido de `=`, produz `OP_NE` (`!=`); senão, `OP_NOT` (`!`).
- Ao encontrar `/`:
  - Se o próximo for `/`, entra no estado de comentário de linha.
  - Se o próximo for `*`, entra no estado de comentário de bloco.
  - Se for qualquer outro caractere, produz o operador de divisão `OP_DIV` (`/`).
- Ao encontrar `&`, se seguido de `&`, produz `OP_AND` (`&&`). Caso isolado, dispara erro léxico.
- Ao encontrar `|`, se seguido de `|`, produz `OP_OR` (`||`). Caso isolado, dispara erro léxico.

### 2.5. Lista Fechada de Palavras Reservadas

A tabela a seguir apresenta a lista fechada e exaustiva de palavras reservadas da linguagem (13 palavras-chave):

| Palavra Reservada | Categoria | Descrição no Escopo |
|---|---|---|
| `int` | Tipo básico | Inteiro de 32 bits com sinal |
| `double` | Tipo básico | Ponto flutuante IEEE-754 dupla precisão |
| `bool` | Tipo básico | Tipo lógico (`true` ou `false`) |
| `char` | Tipo básico | Caractere único |
| `string` | Tipo básico | Cadeia de caracteres |
| `void` | Procedimento | Ausência de retorno em procedimentos/funções |
| `if` | Controle | Comando condicional (se) |
| `else` | Controle | Ramo alternativo condicional (senão) |
| `while` | Controle | Laço de repetição com pré-teste |
| `for` | Controle | Laço de repetição com inicialização, teste e passo |
| `return` | Sub-rotina | Retorno de função ou procedimento |
| `true` | Literal booleano | Constante lógica verdadeira |
| `false` | Literal booleano | Constante lógica falsa |

---

## 3. Categorias de Tokens da Linguagem

| Token | Notação (regex/EBNF) | Exemplos válidos | O que precisa decidir / Decisões adotadas |
| :--- | :--- | :--- | :--- |
| **Identificador** | `letra (letra \| digito \| "_")*`<br><br>*Regex:* `[a-zA-Z_][a-zA-Z0-9_]*` | `total`, `x1`, `contaItens`, `_temp`, `val_max` | • **Case-sensitive?** Sim, diferencia maiúsculas de minúsculas (`total` ≠ `Total`).<br>• **"_" permitido?** Sim, tanto no início quanto no corpo do identificador.<br>• **Tamanho máximo?** Limite prático de 64 caracteres. |
| **Palavra reservada** | `mesmo padrão do identificador + tabela de busca`<br><br>*Padrão:* `int \| double \| bool \| char \| string \| void \| if \| else \| while \| for \| return \| true \| false` | `if`, `while`, `int`, `return`, `bool`, `void` | • **Lista fechada de palavras-chave:** Exatamente 13 palavras reservadas (`int`, `double`, `bool`, `char`, `string`, `void`, `if`, `else`, `while`, `for`, `return`, `true`, `false`). Estritamente minúsculas. |
| **String** | `" (qualquer caractere ≠ " e ≠ \n e ≠ \r)* "`<br><br>*Regex:* `" ( [^"\\\n\r] \| \\["nt\\] )* "` | `"ok"`, `"linha 1"`, `"com \"escape\""`, `""` | • **Escapes permitidos:** `\"`, `\n`, `\t`, `\\`.<br>• **Não fechar até EOL (fim de linha):** Dispara erro léxico e recupera na próxima linha.<br>• **Não fechar até EOF:** Dispara erro léxico apontando a linha/coluna de abertura. |
| **Operador** | `1 ou 2 caracteres`<br><br>*Regex:* `= \| == \| < \| <= \| > \| >= \| ! \| != \| + \| - \| * \| / \| % \| && \| \|\|` | `=`, `==`, `<=`, `+`, `&&`, `!=`, `<`, `>=` | • **Operadores existentes:** Atribuição (`=`), Aritméticos (`+`, `-`, `*`, `/`, `%`), Relacionais (`==`, `!=`, `<`, `<=`, `>`, `>=`), Lógicos (`&&`, `\|\|`, `!`).<br>• **Formas compostas:** `==`, `<=`, `>=`, `!=`, `&&`, `\|\|`. Desambiguação por *Maximal Munch*. |
| **Literal numérico** | `digito+ ("." digito+)?`<br><br>*Inteiro:* `[0-9]+`<br>*Double:* `[0-9]+ \. [0-9]+` | `10`, `3.14`, `0`, `42`, `100.0` | • **Inteiro:** Sequência decimal pura (`[0-9]+`).<br>• **Ponto flutuante:** Exige dígito antes e após o ponto (`dígito+ \. dígito+`).<br>• **Notação científica / Hexadecimal:** Não adotados para simplificação do autômato determinístico. |

---

## 4. Política de Tratamento e Recuperação de Erros

O analisador léxico adota a **filosofia de não-interrupção**: ao encontrar um erro léxico, o analisador registra o erro com a linha e coluna exatas do início da anomalia, e em seguida aplica uma estratégia de recuperação por descarte pontual:

- **Caractere fora do alfabeto:** emite mensagem `Caractere inválido '@'`, consome o caractere e volta ao estado inicial para continuar o processamento.
- **String não fechada até fim de linha (EOL):** emite mensagem `String não fechada antes do fim da linha`, interrompe a leitura na linha atual e retoma no início da linha seguinte.
- **String não fechada até fim de arquivo (EOF):** emite mensagem `String não fechada até o fim do arquivo (EOF)`, marcando a posição onde as aspas foram abertas.
- **Comentário de bloco não fechado até EOF:** emite mensagem `Comentário de bloco '/*' não fechado até o fim do arquivo (EOF)`, referenciando a linha e coluna onde `/*` começou.
- **Operador malformado:** emite mensagem `Caractere inesperado '&', esperado '&&'`.

