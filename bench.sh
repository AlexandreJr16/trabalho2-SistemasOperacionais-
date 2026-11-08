#!/usr/bin/env bash
# roda sequencial e paralelo em varios tamanhos e quantidades de threads e grava um CSV por maquina
# uso (da raiz do projeto): ./bench.sh q1   ou   ./bench.sh q2   (ou so: make run)
set -e
cd "$(dirname "$0")"
QUESTAO=$1

if [ "$QUESTAO" = q1 ]; then
    TAMANHOS="1000 10000 100000 1000000 5000000 10000000 50000000 100000000"
else
    TAMANHOS="128 256 512 768 1024 1536 2048"
fi
REPETICOES=3

# linux tem nproc e /proc/cpuinfo; no mac cai pro sysctl
NUCLEOS=$(nproc 2>/dev/null || sysctl -n hw.ncpu)
CPU=$(grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2 || true)
[ -z "$CPU" ] && CPU=$(sysctl -n machdep.cpu.brand_string 2>/dev/null || echo desconhecida)
CPU=$(echo "$CPU" | sed 's/^ *//; s/,//g')
MAQUINA=$(hostname | cut -d. -f1)

# sempre testa tambem com o numero de CPUs da maquina e o dobro dele, que e onde costuma ficar o pico
THREADS=$(printf '%s\n' 1 2 4 6 8 12 16 "$NUCLEOS" $((NUCLEOS * 2)) | sort -nu | tr '\n' ' ')

# o nome leva a quantidade de CPUs pra duas maquinas com o mesmo hostname nao se sobrescreverem
SAIDA="resultados/${QUESTAO}_${MAQUINA}-${NUCLEOS}cpus.csv"
BINARIO_SEQ="bin/${QUESTAO}Sequencial"
BINARIO_PAR="bin/${QUESTAO}Paralelo"

echo "$QUESTAO em $MAQUINA ($CPU, $NUCLEOS CPUs), threads: $THREADS"
echo "maquina,cpu,nucleos,versao,tamanho,threads,tempoMs,resultado" > "$SAIDA"
for tamanho in $TAMANHOS; do
    for repeticao in $(seq $REPETICOES); do
        echo "$MAQUINA,$CPU,$NUCLEOS,$($BINARIO_SEQ "$tamanho")" >> "$SAIDA"
        for threads in $THREADS; do
            echo "$MAQUINA,$CPU,$NUCLEOS,$($BINARIO_PAR "$tamanho" "$threads")" >> "$SAIDA"
        done
    done
    echo "  tamanho $tamanho ok"
done
echo "gravado em $SAIDA"
