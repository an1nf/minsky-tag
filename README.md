# minsky-tag

Traducciones de máquinas de Turing a máquinas de Minsky de 3 y 2 contadores y a sistemas 2-tag, con verificación paso a paso contra un simulador de referencia.

```text
MT -> 3 contadores (L, R, T) -> 2 contadores (Z = 2^L 3^R 5^T, Y)
MT -> 2-tag -> sistema de etiquetas cíclico
```

Fundamentos, demostraciones y referencias: [FUNDAMENTOS.md](FUNDAMENTOS.md).

## Estructura

```text
src/      código fuente (paquete minsky)
test/     tests JUnit 4
output/   programas compilados, producciones, traza y tabla de resultados
```

## Uso

```bash
mvn test
mvn package
java -jar target/minsky-tag-1.0.jar output --trace
```

`output/` contiene, para cada MT de ejemplo:

* `<mt>_3contadores.txt`, `<mt>_2contadores.txt`: programas INC/DEC/HALT;
* `<mt>_2tag.txt`: producciones del sistema 2-tag;
* `castor-2_traza_2tag.txt`: evolución de la palabra del 2-tag;
* `resultados.md`: tabla de resultados.
