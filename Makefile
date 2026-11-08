CXX = g++
CXXFLAGS = -std=c++17 -O2 -Wall -Wextra -pthread

all: q1 q2 q3

q1: bin/q1Sequencial bin/q1Paralelo

q2: bin/q2Sequencial bin/q2Paralelo

q3:
	mkdir -p bin/q3
	javac -Xlint:all,-serial -d bin/q3 q3/*.java q3/comThreads/*.java q3/semThreads/*.java

bin/q1Sequencial: q1/sequencial.cpp q1/comum.h
bin/q1Paralelo: q1/paralelo.cpp q1/comum.h
bin/q2Sequencial: q2/sequencial.cpp q2/comum.h
bin/q2Paralelo: q2/paralelo.cpp q2/comum.h

bin/q1Sequencial bin/q1Paralelo bin/q2Sequencial bin/q2Paralelo:
	mkdir -p bin
	$(CXX) $(CXXFLAGS) -o $@ $<

# roda os benchmarks da Q1 e Q2 nesta maquina e gera resultados/qN_<maquina>-<cpus>cpus.csv
run: q1 q2
	./bench.sh q1
	./bench.sh q2
	@echo "pronto: manda os CSV de resultados/ pro grupo"

graficos:
	python3 grafico.py q1
	python3 grafico.py q2

limpar:
	rm -rf bin

.PHONY: all q1 q2 q3 run graficos limpar
