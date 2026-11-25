# CP5 - Verificador de Tipos

Grupo: João Vitor Feijó Asevedo e Ester Camilly Simplício de Freitas

Nessa etapa o compilador calcula o tipo de cada expressão e recusa os programas que quebram as regras de tipo. As regras estão no `tipos-a-mao.pdf` e o código usa os mesmos nomes de regra que estão lá.

## Como rodar

Dentro da pasta `cp5-JoaoVitorFeijo-EsterFreitas`:

```
javac -encoding UTF-8 -d out src/*.java src/ast/*.java src/symbols/*.java src/visitor/*.java src/types/*.java test/*.java
java -cp out src.TypeMain test/programas/valido_todos.txt
java -cp out test.TypeTest
```

O `TypeMain` mostra os erros e no final diz se o programa pode ir para a geração de código. O `TypeTest` roda os testes do CP5 e depois os do CP3 e do CP4 (regressão).

## Como os tipos estão representados

- `src/symbols/Type.java`: um enum com `INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING`, `VOID` (retorno de procedimento) e `ERRO`. O `ERRO` é o tipo que a gente usa para não dar erro em cascata.
- `src/ast/Expr.java`: ganhou o campo `type`. Foi a única mudança na AST. Depois da verificação toda expressão fica com o tipo gravado, e é esse campo que o gerador vai ler.
- `src/symbols/Symbol.java`: o campo `type` que tinha ficado reservado no CP4 agora guarda o tipo da variável/parâmetro ou o tipo de retorno da rotina (procedimento = `VOID`). Também tem `paramTypes`, com os tipos dos parâmetros. Quem preenche isso é a passada de identificação do CP4, na hora do `declare`. Como a passada prévia já declara todas as rotinas antes dos corpos, a assinatura já existe quando o verificador roda.

O verificador não mexe na tabela de símbolos (não faz `declare`, `openScope` nem `closeScope`), ele só lê o `getDecl()` dos nós que o CP4 deixou decorados.

## Onde ficam as regras

São dois arquivos em `src/types/`:

- `TypeRules.java`: a tabela de operadores. Cada método é uma regra (ou um grupo de regras parecidas).
- `TypeChecker.java`: o Visitor que percorre a AST e aplica as regras. Não usa `instanceof` nem `switch` no tipo do nó.

Regra do papel -> onde está:

- `num(T)` -> `TypeRules.numerico`
- `U ≼ T` (argumentos) -> `TypeRules.argumentoAceito`
- tipo igual (declaração, atribuição, return) -> `TypeRules.mesmoTipo`
- [LIT-INT], [LIT-DOUBLE], [LIT-BOOL], [LIT-CHAR], [LIT-STRING] -> `TypeChecker.visitIntLit` ... `visitStringLit`
- [ID] -> `TypeChecker.visitId`
- [ARIT-INT] e [ARIT-MISTO] -> `TypeRules.aritmetica`
- [REL] -> `TypeRules.relacional`
- [IGUAL] e [IGUAL-MISTO] -> `TypeRules.igualdade`
- [LOG] -> `TypeRules.logico`
- [NAO] e [NEG] -> `TypeRules.nao` e `TypeRules.neg`
- [CHAMADA] -> `TypeChecker.visitCall` (que chama `call(n, false)`)
- [CHAMADA-CMD] -> `TypeChecker.visitCallStmt` (que chama `call(n, true)`)
- [DECL] e [DECL-SEM-VALOR] -> `TypeChecker.visitVarDecl`
- [ATRIB] -> `TypeChecker.visitAssign`
- [IF], [IF-ELSE], [WHILE], [FOR] -> `visitIf`, `visitWhile`, `visitFor` (a condição é conferida em `condition`)
- [BLOCO] -> `TypeChecker.visitBlock`
- [RETURN] e [RETURN-VAZIO] -> `TypeChecker.visitReturn`
- [FUNC] e [PROC] -> `visitFuncDecl` e `visitProcDecl` (as duas usam `routine`)
- [ERRO] -> todo método do `TypeRules` começa devolvendo `ERRO` se algum operando for `ERRO`

A função corrente fica guardada no `TypeChecker` (`routineName` e `routineReturn`) para conferir o `return`. Não tem pilha de laços porque a linguagem não tem `break`.

Sobre a cascata: quando nenhuma regra serve, sai uma mensagem só naquele nó e ele vira `ERRO`. Daí pra cima o `ERRO` é aceito em tudo sem mensagem nova. Nome não declarado (erro do CP4) também vira `ERRO`. Uma chamada com número errado de argumentos continua com o tipo de retorno da função, para não estragar a expressão em volta.

O aviso de variável sem valor fica no `TypeChecker` (campo `withoutValue`). É uma análise conservadora: só conta atribuição em sequência ou nos dois lados de um if/else; dentro de if sem else, while e for não conta.

## Onde ficam as decisões

| # | Decisão | Código | Teste |
|-|-|-|-|
| 1 | int -> double só em operação mista | `TypeRules.aritmetica/relacional/igualdade` e `mesmoTipo` | `dec01_int_double.txt` |
| 2 | char com int não pode | `TypeRules.numerico` (char não é numérico) | `dec02_char_int.txt` |
| 3 | bool fora da aritmética, condição tem que ser bool | `TypeRules.numerico` e `TypeChecker.condition` | `dec03_bool_condicao.txt` |
| 4 | string só `==` e `!=` | `TypeRules.igualdade` | `dec04_string.txt` |
| 5 | `==` com tipos iguais ou int/double | `TypeRules.igualdade` | `dec05_igualdade_mista.txt` |
| 6 | função como comando descarta o valor | `TypeChecker.call(n, true)` | `dec06_funcao_como_comando.txt` |
| 7 | recursão mútua com duas passadas | `IdentificationVisitor.RoutineHeaders` (CP4) | `dec07_recursao_mutua.txt` |
| 8 | argumento aceita int -> double | `TypeRules.argumentoAceito` | `dec08_argumento_int_double.txt` |
| 9 | basta um return com valor | `TypeChecker.visitFuncDecl` | `dec09_return_algum.txt` |
| 10 | mesmo nome é redeclaração | `IdentificationVisitor.declare` (CP4) | `dec10_mesmo_nome.txt` |
| 11 | função sem parênteses é erro | `TypeChecker.visitId` | `dec11_funcao_sem_parenteses.txt` |

Obs.: a linguagem não tem `print`, `read`, `break` nem função dentro de função. Por isso o teste de "variável do nível acima" (`func_nivel_acima.txt`) usa variável global.

Para o gerador (ainda não implementado): double vai ocupar 1 palavra da TAM em ponto fixo, e string vai ser 1 palavra com o endereço de um vetor de chars no heap.

## Mensagens

Uma por linha, ordenadas por linha (e coluna). Os erros do CP4 aparecem junto. Exemplos:

```
linha 3: condição de 'if' deve ser bool, mas é int
linha 4: operador '+' não se aplica a int e bool
linha 7: atribuição a 'x': esperado int, veio double
linha 8: return em 'f': esperado int, veio bool
linha 11: 'fat' espera 1 argumento(s), recebeu 2
linha 12: argumento 1 de 'mostra': esperado string, veio int
linha 13: 'p' é procedimento e não pode ser usado em expressão
linha 14: 'f' é função; use f(...) para chamá-la
linha 15 (aviso): 'x' pode ser usado sem valor
```

Também tem: `inicialização de 'x': esperado T, veio U`, `atribuição a 'f', que não é variável`, `return sem valor em 'f', que devolve T`, `return com valor no procedimento 'p'`, `função 'f' não tem return com valor`, `'x' não é função nem procedimento` e `operador '!' não se aplica a T`.

Se não tiver nenhuma mensagem, a saída é `nenhum erro`. Se o programa tiver erro léxico ou sintático, não tem AST e a verificação nem roda. Com qualquer erro (aviso não conta) o programa não vai para a geração de código.

## Testes

Cada `test/programas/<nome>.txt` tem a saída esperada em `test/esperado/<nome>.txt`, que a gente escreveu a partir das regras do papel. Tem os programas que o enunciado pede (válido com tudo, erro em expressão, condição int, return errado, chamada errada, procedimento em expressão, vários erros), os de função (recursiva, mútua e usando global) e um por decisão.

O `TypeTest` também testa a tabela de operadores sem passar pelo parser (todas as combinações de operador com dois tipos), confere que toda expressão ficou com tipo e roda vários programas cortados e alterados para ver se nada lança exceção.
