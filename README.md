# TP2 de Sistemas Operacionais (UFAM, 2026-2)

Equipe: Alexandre Pereira, Danilo Magalhaes, Lucas Cristiano.

- **Q1**: produto escalar sequencial x paralelo (C++ / pthread)
- **Q2**: multiplicacao de matrizes sequencial x paralela (C++ / pthread)
- **Q3**: JohnMarcos Bet, roleta online com threads x sem threads (Java / Swing)

## Compilar

```sh
make            # tudo (precisa g++, make e um JDK 21+ pra Q3)
make q1 q2      # so os C++
```

## Benchmark em outra maquina (Q1 e Q2)

```sh
make run        # so precisa de g++ e make
```

Gera `resultados/q1_<maquina>-<cpus>cpus.csv` e `resultados/q2_...`. Feche os outros programas antes
de rodar (leva alguns minutos) e mande os dois CSV pro grupo. Com todos os CSV em `resultados/`:

```sh
make graficos   # precisa python3 + matplotlib; gera os PNG e as tabelas de speedup (qN_speedup.md)
```

Rodar um so:

```sh
bin/q1Sequencial 1000000          # seq,tamanho,1,tempoMs,resultado
bin/q1Paralelo 1000000 8          # par,tamanho,threads,tempoMs,resultado
bin/q2Sequencial 1024
bin/q2Paralelo 1024 12
```

## Q3: JohnMarcos Bet

Cassino online com lobby e três jogos: roleta, crash (aviãozinho) e corrida de cavalos. Os bots são
professores do IComp. No lobby se escolhe o modo **com threads** ou **sem threads** e depois o jogo. As duas
versões têm interface gráfica; a sem threads faz tudo na thread da interface, então a janela trava
enquanto o programa trabalha. O placar do lobby compara o tempo dos dois modos (Ts / Tp).

```sh
java -cp bin/q3 Cassino         # lobby
```
