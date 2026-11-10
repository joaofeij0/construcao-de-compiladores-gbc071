# CP2: Parser recursivo descendente

Compilar e executar (Java 11+), a partir desta pasta:

```
javac -encoding UTF-8 src/\*.java test/\*.java
java -Dstdout.encoding=UTF-8 -cp . test.ParserTest          # suíte de testes do parser
java -Dstdout.encoding=UTF-8 -cp . test.ScannerTest         # testes do scanner (CP1, inalterados)
java -Dstdout.encoding=UTF-8 -cp . src.ParserMain \[arquivo] # scanner + parser em um arquivo (ou demo)

```

* `gramatica-first-follow.md`: gramática final, FIRST/FOLLOW e verificação LL(1).
* `src/`: scanner do CP1 (`Scanner`, `Token`, `TokenType`, `LexicalError`, `Main`) **sem modificação** + `Parser`, `SyntaxError`, `ParserMain`.
* `test/`: `ParserTest` (válidos, inválidos, múltiplos erros, robustez) e `ScannerTest` (CP1); `test-execution-log.txt` é a saída de `ParserTest`.

