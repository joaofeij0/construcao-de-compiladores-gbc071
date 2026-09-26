## Especificação Léxica da Linguagem

**Disciplina:** Construção de Compiladores - GBC071
**Grupo:** João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

---

## 1. Visão geral da linguagem

Definimos uma linguagem imperativa e estaticamente tipada, a ideia foi manter o escopo enxuto o suficiente para dar pra implementar o analisador léxico sem complicação demais, mas ainda assim cobrindo o que normalmente se pede num compilador básico. Ficou definido o seguinte:

- **Tipos básicos:** `int`, `double`, `bool`, `char` e `string`.
- **Estruturas de controle:** condicional com `if`/`else` e repetição com `while` e `for`.
- **Procedimentos e funções:** podem ter tipo de retorno (`void` ou um tipo primitivo), recebem parâmetros por valor e usam `return` para devolver (ou não) um valor.

Abaixo resumimos as principais categorias de tokens que o analisador léxico precisa reconhecer, junto com as decisões que tomamos pra cada uma:

| Token | Notação (regex/EBNF) | Exemplos válidos | O que precisa decidir / Decisões adotadas |
| :--- | :--- | :--- | :--- |
| **Identificador** | `letra (letra \| digito \| "_")*`<br><br>*Regex:* `[a-zA-Z_][a-zA-Z0-9_]*` | `total`, `x1`, `contaItens`, `_temp`, `val_max` | • **Case-sensitive?** Sim, diferencia maiúsculas de minúsculas (`total` ≠ `Total`).<br>• **"_" permitido?** Sim, tanto no início quanto no corpo do identificador.<br>• **Tamanho máximo?** 64 caracteres. |
| **Palavra reservada** | `mesmo padrão do identificador + tabela de busca`<br><br>*Padrão:* `int \| double \| bool \| char \| string \| void \| if \| else \| while \| for \| return \| true \| false` | `if`, `while`, `int`, `return`, `bool`, `void` | • **Lista fechada de palavras-chave:** Exatamente 13 palavras reservadas (`int`, `double`, `bool`, `char`, `string`, `void`, `if`, `else`, `while`, `for`, `return`, `true`, `false`). Minúsculas. |
| **String** | `" (qualquer caractere ≠ " e ≠ \n e ≠ \r)* "`<br><br>*Regex:* `" ( [^"\\\n\r] \| \\["nt\\] )* "` | `"ok"`, `"linha 1"`, `"com \"escape\""`, `""` | • **Escapes permitidos:** `\"`, `\n`, `\t`, `\\`.<br>• **Não fechar até EOL:** Dispara erro léxico e recupera na próxima linha.<br>• **Não fechar até EOF:** Dispara erro léxico apontando a linha/coluna de abertura. |
| **Operador** | `1 ou 2 caracteres`<br><br>*Regex:* `= \| == \| < \| <= \| > \| >= \| ! \| != \| + \| - \| * \| / \| % \| && \| \|\|` | `=`, `==`, `<=`, `+`, `&&`, `!=`, `<`, `>=` | • **Operadores existentes:** Atribuição (`=`), Aritméticos (`+`, `-`, `*`, `/`, `%`), Relacionais (`==`, `!=`, `<`, `<=`, `>`, `>=`), Lógicos (`&&`, `\|\|`, `!`).<br>• **Formas compostas:** `==`, `<=`, `>=`, `!=`, `&&`, `\|\|`. Desambiguação por Maximal Munch. |
| **Literal numérico** | `digito+ ("." digito+)?`<br><br>*Inteiro:* `[0-9]+`<br>*Double:* `[0-9]+ \. [0-9]+` | `10`, `3.14`, `0`, `42`, `100.0` | • **Inteiro:** Sequência decimal pura (`[0-9]+`).<br>• **Ponto flutuante:** Exige dígito antes e depois do ponto (`dígito+ \. dígito+`).<br>• |

---

## 2. Questões obrigatórias

### 2.1. Qual é o alfabeto de entrada?

Decidimos usar como alfabeto de entrada o conjunto de caracteres ASCII imprimíveis (do código 32 ao 126), mais os caracteres de espaçamento que servem pra separar os tokens:

| Caractere | Descrição | Código ASCII |
|---|---|---|
| ` ` | Espaço em branco | 32 |
| `\t` | Tabulação horizontal | 9 |
| `\n` | Quebra de linha (LF) | 10 |
| `\r` | Retorno de carro (CR) | 13 |

Qualquer caractere fora desse conjunto (tipo `@`, `$`, `~`, `^` ou algum caractere binário estranho) é considerado inválido. Quando isso acontece, o analisador léxico não trava: ele reporta o erro dizendo exatamente em que linha e coluna aconteceu, descarta aquele caractere e continua lendo o resto do arquivo normalmente.

### 2.2. A linguagem é case-sensitive?

Sim, optamos por deixar a linguagem case-sensitive, ou seja, ela diferencia maiúsculas de minúsculas. Isso quer dizer que `total`, `Total` são dois identificadores diferentes para o compilador.

- **Palavras reservadas:** só valem em minúsculo (`int`, `if`, `while`, `return` etc.). Se escrever `IF` ou `While`, o scanner não reconhece como palavra-chave e trata como um identificador comum.
- **Identificadores:** seguem essa mesma lógica de diferenciar caixa alta, podendo misturar letras maiúsculas e minúsculas (`[a-zA-Z]`).

### 2.3. Como ficam delimitados os espaços em branco e os comentários?

- **Espaços em branco:** os caracteres `' '`, `'\t'`, `'\r'` e `'\n'` servem só pra separar os tokens durante a análise léxica. Eles não viram token pro analisador sintático, mas são usados internamente pra atualizar os contadores de linha e coluna (que são usados para reportar erros depois).
- **Comentários de linha:** começam com `//` e vão até encontrar um `\n` ou até acabar o arquivo (EOF) e não geram token.
- **Comentários de bloco:** começam com `/*` e terminam com `*/`, podendo ocupar várias linhas. Decidimos não permitir aninhamento: o primeiro `*/` que aparece já fecha o comentário. Se o arquivo acabar antes de fechar o comentário, é gerado um erro léxico apontando a linha e a coluna onde o `/*` começou.

### 2.4. Regra de desambiguação (Maximal Munch)

Pra decidir entre operadores simples e compostos (e também pra diferenciar comentário de divisão), usamos a regra do Maximal Munch, o scanner sempre tenta consumir o maior prefixo válido possível antes de decidir qual token gerar.

### 2.5. Lista fechada de palavras reservadas

Fechamos a lista de palavras-chave da linguagem em 13 no total, mostradas na tabela abaixo:

| Palavra Reservada | Categoria | Descrição no Escopo |
|---|---|---|
| `int` | Tipo básico | Inteiro |
| `double` | Tipo básico | Ponto flutuante |
| `bool` | Tipo básico | Tipo lógico (`true` ou `false`) |
| `char` | Tipo básico | Caractere único |
| `string` | Tipo básico | Cadeia de caracteres |
| `void` | Procedimento | Ausência de retorno |
| `if` | Controle | Comando condicional (se) |
| `else` | Controle | Ramo alternativo condicional (senão) |
| `while` | Controle | Laço de repetição com pré-teste |
| `for` | Controle | Laço de repetição com inicialização, teste e passo |
| `return` | Sub-rotina | Retorno de função |
| `true` | Literal booleano | Constante lógica verdadeira |
| `false` | Literal booleano | Constante lógica falsa |

---

## 3. Como o analisador trata os erros

Decidimos seguir uma filosofia de não interromper a análise, ou seja, ao invés de parar tudo no primeiro erro, o analisador registra o problema (com linha e coluna de onde começou) e tenta se recuperar pra continuar lendo o resto do arquivo. Os casos que mapeamos foram:

- **Caractere fora do alfabeto:** mostra a mensagem `Caractere inválido '@'`, descarta esse caractere e volta pro estado inicial pra seguir lendo.
- **String não fechada até o fim da linha (EOL):** mostra `String não fechada antes do fim da linha`, para de ler ali e retoma a análise já na linha seguinte.
- **String não fechada até o fim do arquivo (EOF):** mostra `String não fechada até o fim do arquivo (EOF)`, indicando onde as aspas foram abertas.
- **Comentário de bloco não fechado até o EOF:** mostra `Comentário de bloco '/*' não fechado até o fim do arquivo (EOF)`, apontando a linha e coluna onde o `/*` começou.
- **Operador malformado:** mostra `Caractere inesperado '&', esperado '&&'`.

Achamos que essa abordagem é melhor porque em vez de parar assim que aparece o primeiro erro, o usuário consegue ver de uma vez só todos os problemas léxicos do arquivo.
