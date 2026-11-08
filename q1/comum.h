#pragma once

#include <chrono>
#include <random>
#include <vector>

/* - Funcoes:
 *     gerarVetor: vetor de doubles aleatorios entre -1 e 1, sempre igual pra mesma semente
 *     agoraMs: instante atual em milissegundos, pra medir so a conta
 */

const unsigned SEMENTE_A = 42;
const unsigned SEMENTE_B = 7;

inline std::vector<double> gerarVetor(long tamanho, unsigned semente) {
    std::mt19937_64 gerador(semente);
    std::uniform_real_distribution<double> distribuicao(-1.0, 1.0);
    std::vector<double> vetor(tamanho);
    for (long i = 0; i < tamanho; i++)
        vetor[i] = distribuicao(gerador);
    return vetor;
}

inline double agoraMs() {
    std::chrono::duration<double, std::milli> desdeInicio = std::chrono::steady_clock::now().time_since_epoch();
    return desdeInicio.count();
}
