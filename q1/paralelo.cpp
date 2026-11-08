#include <cstdio>
#include <cstdlib>
#include <pthread.h>

#include "comum.h"

struct Tarefa {
    const double* a;
    const double* b;
    long inicio;
    long fim;
    double somaParcial;
};

// cada thread soma so o pedaco dela e guarda na propria tarefa, entao ninguem escreve no
// mesmo lugar e nao precisa de lock; a unica sincronizacao e o join no final
void* somarPedaco(void* argumento) {
    Tarefa* tarefa = (Tarefa*)argumento;
    double soma = 0;
    for (long i = tarefa->inicio; i < tarefa->fim; i++)
        soma += tarefa->a[i] * tarefa->b[i];
    tarefa->somaParcial = soma;
    return nullptr;
}

// divide em blocos contiguos; as primeiras threads ficam com um elemento a mais quando nao divide certo
double produtoEscalar(const std::vector<double>& a, const std::vector<double>& b, int quantidadeThreads) {
    long tamanho = a.size();
    long base = tamanho / quantidadeThreads;
    long sobra = tamanho % quantidadeThreads;
    std::vector<Tarefa> tarefas(quantidadeThreads);
    std::vector<pthread_t> threads(quantidadeThreads);

    long inicio = 0;
    for (int t = 0; t < quantidadeThreads; t++) {
        long fim = inicio + base + (t < sobra ? 1 : 0);
        tarefas[t] = {a.data(), b.data(), inicio, fim, 0};
        pthread_create(&threads[t], nullptr, somarPedaco, &tarefas[t]);
        inicio = fim;
    }

    double soma = 0;
    for (int t = 0; t < quantidadeThreads; t++) {
        pthread_join(threads[t], nullptr);
        soma += tarefas[t].somaParcial;
    }
    return soma;
}

int main(int argc, char** argv) {
    if (argc < 3) {
        fprintf(stderr, "uso: %s <tamanho> <threads>\n", argv[0]);
        return 1;
    }
    long tamanho = atol(argv[1]);
    int quantidadeThreads = atoi(argv[2]);
    std::vector<double> a = gerarVetor(tamanho, SEMENTE_A);
    std::vector<double> b = gerarVetor(tamanho, SEMENTE_B);

    double inicio = agoraMs();
    double resultado = produtoEscalar(a, b, quantidadeThreads);
    double tempoMs = agoraMs() - inicio;

    printf("par,%ld,%d,%.4f,%.10f\n", tamanho, quantidadeThreads, tempoMs, resultado);
    return 0;
}
