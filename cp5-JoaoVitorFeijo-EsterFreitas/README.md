# Checkpoint 5 — Verificador de Tipos

**Grupo:** João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

O verificador parte da AST decorada do CP4: depois da passada de identificação, cada `Id`, `Assign` e `Call` aponta para o seu `Symbol` (`getDecl()`). Ele calcula o tipo de cada expressão, grava esse tipo no nó, confere os comandos, os `return` e as chamadas e acumula todas as mensagens. As regras implementadas são as do papel (`tipos-a-mao.pdf`), com os mesmos nomes.

## Como executar

A partir desta pasta:

```
javac -encoding UTF-8 -d out src/*.java src/ast/*.java src/symbols/*.java src/visitor/*.java src/types/*.java test/*.java
java -cp out src.TypeMain test/programas/valido_todos.txt
java -cp out test.TypeTest
```

`TypeMain` imprime as mensagens e, na última linha, se o programa pode seguir para a geração de código. `TypeTest` roda a suíte do CP5 e, no fim, as suítes do CP3 (`AstTest`) e do CP4 (`IdentTest`).

## Como os tipos estão representados

- **`src/symbols/Type.java`**: enum com `INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING`, `VOID` (só como retorno de procedimento) e `ERRO`, o tipo interno que evita a cascata. `Type.of(BasicType)` converte o tipo escrito na declaração.
- **`src/ast/Expr.java`**: ganhou o campo `type` (`getType`/`setType`). É a única mudança na hierarquia de nós. Depois da verificação, toda expressão tem o seu tipo gravado; o gerador de código vai ler esse campo.
- **`src/symbols/Symbol.java`**: o campo `type`, reservado no CP4, agora guarda o tipo da variável ou do parâmetro, ou o tipo de retorno da rotina (`VOID` para procedimento). O novo campo `paramTypes` guarda os tipos dos parâmetros. Os dois são preenchidos pela passada de identificação (`IdentificationVisitor.declare`) no momento da declaração. Como a passada prévia do CP4 declara as rotinas antes dos corpos, a assinatura de toda rotina já existe quando o verificador começa.
- O verificador **não usa a tabela de símbolos**: não faz `declare`, `openScope` nem `closeScope`. Ele só lê `getDecl()` dos nós.

## Onde ficam as regras

`src/types/TypeRules.java` é a tabela de operadores e as relações auxiliares. `src/types/TypeChecker.java` é o Visitor que aplica as regras à AST, sem `instanceof` e sem `switch` sobre o tipo do nó.

| Regra do papel | Onde está |
|-|-|
| `num(T)` | `TypeRules.numerico` |
| `U ≼ T` (argumentos) | `TypeRules.argumentoAceito` |
| tipo igual (declaração, atribuição, return) | `TypeRules.mesmoTipo` |
| [LIT-INT], [LIT-DOUBLE], [LIT-BOOL], [LIT-CHAR], [LIT-STRING] | `TypeChecker.visitIntLit` … `visitStringLit` |
| [ID] | `TypeChecker.visitId` |
| [ARIT-INT], [ARIT-MISTO] | `TypeRules.aritmetica` (chamada por `TypeChecker.visitBin`) |
| [REL] | `TypeRules.relacional` |
| [IGUAL], [IGUAL-MISTO] | `TypeRules.igualdade` |
| [LOG] | `TypeRules.logico` |
| [NAO], [NEG] | `TypeRules.nao`, `TypeRules.neg` (chamadas por `TypeChecker.visitUnary`) |
| [CHAMADA] | `TypeChecker.visitCall` → `TypeChecker.call(n, false)` |
| [CHAMADA-CMD] | `TypeChecker.visitCallStmt` → `TypeChecker.call(n, true)` |
| [DECL], [DECL-SEM-VALOR] | `TypeChecker.visitVarDecl` |
| [ATRIB] | `TypeChecker.visitAssign` |
| [IF], [IF-ELSE], [WHILE], [FOR] | `TypeChecker.visitIf`, `visitWhile`, `visitFor`, com `TypeChecker.condition` |
| [BLOCO] | `TypeChecker.visitBlock` |
| [RETURN], [RETURN-VAZIO] | `TypeChecker.visitReturn` |
| [FUNC], [PROC] | `TypeChecker.visitFuncDecl`, `visitProcDecl`, com `TypeChecker.routine` |
| [ERRO] | primeira linha de cada método de `TypeRules`, e `TypeRules.mesmoTipo`/`argumentoAceito`, que aceitam `ERRO` |

### Contexto

O verificador guarda a rotina corrente (`routineName`, `routineReturn`), que [RETURN] e [FUNC] consultam, e se ela já teve um `return` com valor (`sawReturnWithValue`). A linguagem não tem `break`, então não há pilha de laços abertos.

### Sem cascata

Quando nenhuma regra se aplica, o verificador reporta **um** erro naquele nó e dá a ele o tipo `ERRO`. `ERRO` é aceito por todas as regras sem nova mensagem. Um nome não declarado (já reportado pelo CP4) tem tipo `ERRO` e não gera mensagem de tipo. Uma chamada com número errado de argumentos tem o tipo de retorno da rotina, para não estragar a expressão em volta. Um `return;` numa função conta como o return da regra [FUNC], porque o erro já foi reportado no próprio `return`.

### Aviso de variável sem valor

A análise é conservadora e fica em `TypeChecker` (campo `withoutValue`). No início de cada rotina, os parâmetros têm valor e as variáveis declaradas sem inicializador (locais ou globais) não têm. Uma variável passa a ter valor por atribuição em sequência, ou quando os dois ramos de um if/else atribuem. Atribuição dentro de if sem else, while ou for não conta. Ler uma variável sem valor gera um aviso, que não impede a geração de código.

## Decisões de projeto

| # | Decisão | Onde está no código | Teste |
|-|-|-|-|
| 1 | int → double só em operação mista | `TypeRules.aritmetica`, `relacional`, `igualdade`; `mesmoTipo` em declaração, atribuição e return | `dec01_int_double.txt` |
| 2 | char com int proibido; char só `==`/`!=` com char | `TypeRules.numerico` não inclui char; `TypeRules.igualdade` | `dec02_char_int.txt` |
| 3 | bool fora da aritmética; condição tem que ser bool | `TypeRules.numerico`; `TypeChecker.condition` | `dec03_bool_condicao.txt` |
| 4 | string só `==` e `!=` | `TypeRules.igualdade` | `dec04_string.txt` |
| 5 | `==` aceita tipos iguais e int/double | `TypeRules.igualdade` | `dec05_igualdade_mista.txt` |
| 6 | Função como comando: valor descartado | `TypeChecker.call(n, true)` | `dec06_funcao_como_comando.txt` |
| 7 | Recursão mútua por duas passadas | passada prévia `IdentificationVisitor.RoutineHeaders` (CP4) preenche a assinatura | `dec07_recursao_mutua.txt` |
| 8 | Argumento aceita int → double | `TypeRules.argumentoAceito` | `dec08_argumento_int_double.txt` |
| 9 | Basta algum return com valor | `TypeChecker.visitFuncDecl` e `sawReturnWithValue` | `dec09_return_algum.txt` |
| 10 | Mesmo nome com assinaturas diferentes é proibido | redeclaração no CP4 (`IdentificationVisitor.declare`) | `dec10_mesmo_nome.txt` |
| 11 | Nome de função sem parênteses é erro | `TypeChecker.visitId` | `dec11_funcao_sem_parenteses.txt` |

Itens escolhidos no papel além das 11 decisões: `%` só para int (`TypeRules.aritmetica`); `-` unário para int e double (`TypeRules.neg`); `return 1;` em função double é erro (`TypeRules.mesmoTipo`); a condição do `for` tem que ser bool (`TypeChecker.visitFor`).

**Fora da linguagem:** a linguagem não tem `print`, `read`, `break` nem rotinas aninhadas. O teste de "função usando variável do nível acima" usa variáveis globais (`func_nivel_acima.txt`).

**Registro para o gerador (não implementado):** double em 1 palavra da TAM, em ponto fixo, com perda de precisão. string em 1 palavra com o endereço de um vetor de chars no heap. A conversão int → double (decisões 1, 5 e 8) acontece no operador misto ou na passagem do argumento.

## Formato das mensagens

Uma mensagem por linha, ordenadas por linha e, na mesma linha, por coluna. As mensagens do CP4 entram na mesma lista.

```
linha L: operador 'op' não se aplica a T1 e T2
linha L: operador 'op' não se aplica a T
linha L: condição de 'if' deve ser bool, mas é T          ('while' e 'for' do mesmo jeito)
linha L: inicialização de 'x': esperado T, veio U
linha L: atribuição a 'x': esperado T, veio U
linha L: atribuição a 'f', que não é variável
linha L: return em 'f': esperado T, veio U
linha L: return sem valor em 'f', que devolve T
linha L: return com valor no procedimento 'p'
linha L: função 'f' não tem return com valor
linha L: 'f' espera N argumento(s), recebeu M
linha L: argumento I de 'f': esperado T, veio U
linha L: 'p' é procedimento e não pode ser usado em expressão
linha L: 'x' não é função nem procedimento
linha L: 'f' é função; use f(...) para chamá-la
linha L (aviso): 'x' pode ser usado sem valor
linha L: 'x' não declarado                                (CP4)
linha L: 'x' já declarado neste escopo (linha M)          (CP4)
```

Um programa sem nenhuma mensagem produz `nenhum erro`. Um programa com erro léxico ou sintático lista esses erros e termina com `verificação de tipos não executada: não há AST válida`.

Com qualquer erro (avisos não contam), `TypeReport.canGenerateCode()` é falso e `TypeReport.getTree()` devolve `null`: o programa não segue para a geração de código.

## Testes

Cada programa `test/programas/<nome>.txt` tem a saída esperada em `test/esperado/<nome>.txt`, escrita pelo grupo a partir das regras do papel. `TypeTest` lista os programas sem arquivo esperado e os que divergem.

| Grupo | Programas |
|-|-|
| Válido com todos os tipos e comandos | `valido_todos.txt` |
| Erro de tipo em expressão | `erro_expressao.txt` |
| Condição não booleana | `cond_int.txt` |
| return incompatível | `return_incompativel.txt` |
| Chamada errada (aridade e tipo) | `chamada_errada.txt` |
| Procedimento como expressão | `proc_em_expressao.txt` |
| Vários erros numa rodada, em ordem, sem cascata | `varios_erros.txt`, `nao_declarado_sem_cascata.txt` |
| Funções: recursiva, mutuamente recursiva, variável do nível acima | `func_recursiva.txt`, `func_mutua.txt`, `func_nivel_acima.txt` |
| Uma por decisão | `dec01_…` a `dec11_…` |
| Aviso de variável sem valor | `aviso_sem_valor.txt` |

`TypeTest` também testa as regras isoladas, sem passar pelo parser: todas as 325 combinações de operador binário com dois tipos básicos e as 10 de operador unário, comparadas com a tabela do papel. Testa ainda a propagação de `ERRO` e as relações `≼` e tipo igual. Depois confere que toda expressão de programa aceito tem o tipo gravado no nó. Por fim, confere que nenhuma entrada lança exceção (todos os prefixos dos programas de teste e 4000 mutações deles) e roda a regressão do CP3 e do CP4.
