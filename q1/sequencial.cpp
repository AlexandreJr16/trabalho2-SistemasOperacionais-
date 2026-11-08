#include <cstdio>
#include <cstdlib>

#include "comum.h"

double produtoEscalar(const std::vector<double>& a, const std::vector<double>& b) {
    double soma = 0;
    for (size_t i = 0; i < a.size(); i++)
        soma += a[i] * b[i];
    return soma;
}

int main(int argc, char** argv) {
    if (argc < 2) {
        fprintf(stderr, "uso: %s <tamanho>\n", argv[0]);
        return 1;
    }
    long tamanho = atol(argv[1]);
    std::vector<double> a = gerarVetor(tamanho, SEMENTE_A);
    std::vector<double> b = gerarVetor(tamanho, SEMENTE_B);

    double inicio = agoraMs();
    double resultado = produtoEscalar(a, b);
    double tempoMs = agoraMs() - inicio;

    printf("seq,%ld,1,%.4f,%.10f\n", tamanho, tempoMs, resultado);
    return 0;
}
