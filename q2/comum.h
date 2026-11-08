#pragma once

#include <chrono>
#include <random>
#include <vector>

/* - Funcoes:
 *     gerarMatriz: matriz n x n guardada em linha num vetor so, sempre igual pra mesma semente
 *     somaElementos: checksum pra conferir que sequencial e paralelo deram a mesma coisa
 *     agoraMs: instante atual em milissegundos, pra medir so a conta
 */

const unsigned SEMENTE_A = 42;
const unsigned SEMENTE_B = 7;

inline std::vector<double> gerarMatriz(int n, unsigned semente) {
    std::mt19937_64 gerador(semente);
    std::uniform_real_distribution<double> distribuicao(-1.0, 1.0);
    std::vector<double> matriz((size_t)n * n);
    for (double& elemento : matriz)
        elemento = distribuicao(gerador);
    return matriz;
}

inline double somaElementos(const std::vector<double>& matriz) {
    double soma = 0;
    for (double elemento : matriz)
        soma += elemento;
    return soma;
}

inline double agoraMs() {
    std::chrono::duration<double, std::milli> desdeInicio = std::chrono::steady_clock::now().time_since_epoch();
    return desdeInicio.count();
}
