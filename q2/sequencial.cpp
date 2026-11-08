#include <cstdio>
#include <cstdlib>

#include "comum.h"

// ordem i-k-j: o laco de dentro anda em linha em B e C, que e bem melhor pro cache que o i-j-k
void multiplicar(const std::vector<double>& a, const std::vector<double>& b, std::vector<double>& c, int n) {
    for (int i = 0; i < n; i++)
        for (int k = 0; k < n; k++) {
            double valorA = a[(size_t)i * n + k];
            for (int j = 0; j < n; j++)
                c[(size_t)i * n + j] += valorA * b[(size_t)k * n + j];
        }
}

int main(int argc, char** argv) {
    if (argc < 2) {
        fprintf(stderr, "uso: %s <n>\n", argv[0]);
        return 1;
    }
    int n = atoi(argv[1]);
    std::vector<double> a = gerarMatriz(n, SEMENTE_A);
    std::vector<double> b = gerarMatriz(n, SEMENTE_B);
    std::vector<double> c((size_t)n * n, 0.0);

    double inicio = agoraMs();
    multiplicar(a, b, c, n);
    double tempoMs = agoraMs() - inicio;

    printf("seq,%d,1,%.4f,%.6f\n", n, tempoMs, somaElementos(c));
    return 0;
}
