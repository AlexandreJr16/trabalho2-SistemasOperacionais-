"""Le os CSV de resultados/ (um por maquina) e gera os graficos e as tabelas de speedup.

Uso: python3 grafico.py q1   (ou q2)
"""
import csv
import glob
import statistics
import sys
from collections import defaultdict

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

NOME_TAMANHO = {"q1": "tamanho do vetor", "q2": "ordem da matriz (n)"}


# mediana das repeticoes, por (versao, tamanho, threads); a mediana ignora rodada que pegou o SO ocupado
def lerMaquina(caminho):
    tempos = defaultdict(list)
    with open(caminho) as arquivo:
        for linha in csv.DictReader(arquivo):
            chave = (linha["versao"], int(linha["tamanho"]), int(linha["threads"]))
            tempos[chave].append(float(linha["tempoMs"]))
            cabecalho = f'{linha["maquina"]} ({linha["cpu"]}, {linha["nucleos"]} CPUs)'
    medianas = {chave: statistics.median(valores) for chave, valores in tempos.items()}
    return cabecalho, medianas


def graficoTempo(questao, maquina, cabecalho, medianas):
    tamanhos = sorted({tamanho for (_, tamanho, _) in medianas})
    threadsUsadas = sorted({threads for (versao, _, threads) in medianas if versao == "par"})
    plt.figure(figsize=(8, 5))
    plt.plot(tamanhos, [medianas[("seq", t, 1)] for t in tamanhos], "k-o", linewidth=2.5, label="sequencial")
    for threads in threadsUsadas:
        plt.plot(tamanhos, [medianas[("par", t, threads)] for t in tamanhos], "-o", markersize=3, label=f"paralelo {threads} threads")
    plt.xscale("log")
    plt.yscale("log")
    plt.xlabel(NOME_TAMANHO[questao])
    plt.ylabel("tempo (ms)")
    plt.title(f"{questao.upper()}: tempo x tamanho\n{cabecalho}")
    plt.grid(True, which="both", alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    plt.savefig(f"resultados/{questao}_tempo_{maquina}.png", dpi=130)
    plt.close()


def tabelaSpeedup(cabecalho, medianas):
    tamanhos = sorted({tamanho for (_, tamanho, _) in medianas})
    threadsUsadas = sorted({threads for (versao, _, threads) in medianas if versao == "par"})
    linhas = [f"### {cabecalho}", "", "| tamanho | Ts (ms) | " + " | ".join(f"Sp {t}t" for t in threadsUsadas) + " |"]
    linhas.append("|" + "---|" * (len(threadsUsadas) + 2))
    for tamanho in tamanhos:
        tempoSequencial = medianas[("seq", tamanho, 1)]
        speedups = [tempoSequencial / medianas[("par", tamanho, t)] for t in threadsUsadas]
        linhas.append(f"| {tamanho} | {tempoSequencial:.3f} | " + " | ".join(f"{s:.2f}" for s in speedups) + " |")
    return "\n".join(linhas) + "\n"


# speedup x threads no maior tamanho, uma curva por maquina, pra comparar as CPUs
def graficoSpeedup(questao, maquinas):
    plt.figure(figsize=(8, 5))
    for cabecalho, medianas in maquinas:
        maiorTamanho = max(tamanho for (_, tamanho, _) in medianas)
        threadsUsadas = sorted({threads for (versao, _, threads) in medianas if versao == "par"})
        tempoSequencial = medianas[("seq", maiorTamanho, 1)]
        speedups = [tempoSequencial / medianas[("par", maiorTamanho, t)] for t in threadsUsadas]
        plt.plot(threadsUsadas, speedups, "-o", label=f"{cabecalho}, tamanho {maiorTamanho}")
    plt.axhline(1, color="gray", linestyle="--", linewidth=1)
    plt.xlabel("threads")
    plt.ylabel("speedup Sp = Ts / Tp")
    plt.title(f"{questao.upper()}: speedup no maior tamanho")
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=8)
    plt.tight_layout()
    plt.savefig(f"resultados/{questao}_speedup.png", dpi=130)
    plt.close()


def main():
    questao = sys.argv[1]
    maquinas = []
    tabelas = []
    for caminho in sorted(glob.glob(f"resultados/{questao}_*.csv")):
        maquina = caminho.split(f"{questao}_", 1)[1].removesuffix(".csv")
        cabecalho, medianas = lerMaquina(caminho)
        graficoTempo(questao, maquina, cabecalho, medianas)
        tabelas.append(tabelaSpeedup(cabecalho, medianas))
        maquinas.append((cabecalho, medianas))
    graficoSpeedup(questao, maquinas)

    with open(f"resultados/{questao}_speedup.md", "w") as arquivo:
        arquivo.write("\n".join(tabelas))
    print("\n".join(tabelas))
    print(f"graficos e tabela em resultados/{questao}_*")


main()
