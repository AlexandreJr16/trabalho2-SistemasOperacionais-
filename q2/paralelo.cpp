#include <cstdio>
#include <cstdlib>
#include <pthread.h>

#include "comum.h"

struct Tarefa {
    const double* a;
    const double* b;
    double* c;
    int n;
    int linhaInicio;
    int linhaFim;
};

// cada thread calcula um bloco de linhas de C; como cada uma so escreve nas linhas dela,
// nao tem disputa e nao precisa de lock
void* multiplicarLinhas(void* argumento) {
    Tarefa* tarefa = (Tarefa*)argumento;
    int n = tarefa->n;
    for (int i = tarefa->linhaInicio; i < tarefa->linhaFim; i++)
        for (int k = 0; k < n; k++) {
            double valorA = tarefa->a[(size_t)i * n + k];
            for (int j = 0; j < n; j++)
                tarefa->c[(size_t)i * n + j] += valorA * tarefa->b[(size_t)k * n + j];
        }
    return nullptr;
}

// as primeiras threads levam uma linha a mais quando n nao divide certo
void multiplicar(const std::vector<double>& a, const std::vector<double>& b, std::vector<double>& c, int n, int quantidadeThreads) {
    int base = n / quantidadeThreads;
    int sobra = n % quantidadeThreads;
    std::vector<Tarefa> tarefas(quantidadeThreads);
    std::vector<pthread_t> threads(quantidadeThreads);

    int linhaInicio = 0;
    for (int t = 0; t < quantidadeThreads; t++) {
        int linhaFim = linhaInicio + base + (t < sobra ? 1 : 0);
        tarefas[t] = {a.data(), b.data(), c.data(), n, linhaInicio, linhaFim};
        pthread_create(&threads[t], nullptr, multiplicarLinhas, &tarefas[t]);
        linhaInicio = linhaFim;
    }

    for (int t = 0; t < quantidadeThreads; t++)
        pthread_join(threads[t], nullptr);
}

int main(int argc, char** argv) {
    if (argc < 3) {
        fprintf(stderr, "uso: %s <n> <threads>\n", argv[0]);
        return 1;
    }
    int n = atoi(argv[1]);
    int quantidadeThreads = atoi(argv[2]);
    std::vector<double> a = gerarMatriz(n, SEMENTE_A);
    std::vector<double> b = gerarMatriz(n, SEMENTE_B);
    std::vector<double> c((size_t)n * n, 0.0);

    double inicio = agoraMs();
    multiplicar(a, b, c, n, quantidadeThreads);
    double tempoMs = agoraMs() - inicio;

    printf("par,%d,%d,%.4f,%.6f\n", n, quantidadeThreads, tempoMs, somaElementos(c));
    return 0;
}
