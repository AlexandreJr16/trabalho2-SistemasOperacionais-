import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

// JohnMarcos Bet sem threads: mesmas regras, mas um programa sequencial so. Cada jogador
// aposta na sua vez e os outros ficam esperando; depois que passa a vez nao da pra mudar nada
// uso: java -cp bin/q3 RoletaSequencial [jogadores saldoInicial atrasoMinimoMs atrasoMaximoMs]
public class RoletaSequencial {

    static final long CAIXA_INICIAL = 100000;
    static final int MAXIMO_APOSTAS = 4;
    static final int[] FICHAS = {10, 25, 50, 100};
    static final int DURACAO_GIRO_MS = 4500;

    static final Scanner teclado = new Scanner(System.in);
    static final Random sorteio = new Random();
    static long caixa = CAIXA_INICIAL;
    static int atrasoMinimoMs;
    static int atrasoMaximoMs;

    public static void main(String[] args) throws InterruptedException {
        int quantidadeBots = args.length == 4 ? Integer.parseInt(args[0]) : lerInteiro("Bots", 6);
        int saldoInicial = args.length == 4 ? Integer.parseInt(args[1]) : lerInteiro("Saldo inicial", 1000);
        atrasoMinimoMs = args.length == 4 ? Integer.parseInt(args[2]) : lerInteiro("Atraso minimo dos bots (ms)", 500);
        atrasoMaximoMs = Math.max(atrasoMinimoMs, args.length == 4 ? Integer.parseInt(args[3]) : lerInteiro("Atraso maximo dos bots (ms)", 3000));

        List<String> nomes = new ArrayList<>(List.of("Voce"));
        for (int i = 1; i <= quantidadeBots; i++)
            nomes.add("Jogador " + i);
        int[] saldos = new int[nomes.size()];
        Arrays.fill(saldos, saldoInicial);

        int rodada = 1;
        do {
            jogarRodada(rodada++, nomes, saldos);
        } while (!lerLinha("Nova rodada? [S/n] ").equalsIgnoreCase("n"));
    }

    static void jogarRodada(int rodada, List<String> nomes, int[] saldos) throws InterruptedException {
        long inicio = System.currentTimeMillis();
        System.out.println("\n=== rodada " + rodada + ": facam suas apostas, um de cada vez ===");
        List<List<Aposta>> apostas = new ArrayList<>();
        int acoes = 0;
        for (int i = 0; i < nomes.size(); i++) {
            System.out.println("--- vez de " + nomes.get(i) + " (saldo " + saldos[i] + "), o resto espera ---");
            List<Aposta> daVez = i == 0 ? vezDoHumano(saldos, i) : vezDoBot(nomes.get(i), saldos, i);
            acoes += daVez.size();
            apostas.add(daVez);
        }
        System.out.println("nao vai mais!");

        int numero = girar();
        apurar(nomes, saldos, apostas, numero);
        double segundos = (System.currentTimeMillis() - inicio) / 1000.0;
        System.out.printf("rodada %d levou %.1f s para %d jogadores (%d acoes), caixa da casa %d%n", rodada, segundos, nomes.size(), acoes, caixa);
    }

    // o atraso e o tempo que o bot fica pensando; aqui ele soma no tempo da rodada inteira
    static List<Aposta> vezDoBot(String nome, int[] saldos, int indice) throws InterruptedException {
        List<Aposta> daVez = new ArrayList<>();
        int quantas = 1 + sorteio.nextInt(MAXIMO_APOSTAS - 1);
        for (int k = 0; k < quantas; k++) {
            System.out.println(nome + " pensando...");
            Thread.sleep(atrasoMinimoMs + sorteio.nextInt(atrasoMaximoMs - atrasoMinimoMs + 1));
            int ficha = FICHAS[sorteio.nextInt(FICHAS.length)];
            if (ficha > saldos[indice])
                break;
            Aposta aposta = Aposta.aleatoria(sorteio, ficha);
            saldos[indice] -= ficha;
            daVez.add(aposta);
            System.out.println(nome + " apostou " + aposta + " (saldo " + saldos[indice] + ")");
        }
        return daVez;
    }

    static List<Aposta> vezDoHumano(int[] saldos, int indice) {
        List<Aposta> daVez = new ArrayList<>();
        System.out.println("alvos: vermelho preto par impar duzia1 duzia2 duzia3 ou um numero de 0 a 36");
        while (daVez.size() < MAXIMO_APOSTAS) {
            String[] partes = lerLinha("sua aposta (ex: vermelho 50, 17 20) ou enter pra passar a vez: ").split("\\s+");
            if (partes[0].isEmpty())
                break;
            if (partes.length != 2 || !Aposta.alvoValido(partes[0]) || !partes[1].matches("\\d+")) {
                System.out.println("nao entendi, tenta de novo");
                continue;
            }
            int valor = Integer.parseInt(partes[1]);
            if (valor <= 0 || valor > saldos[indice]) {
                System.out.println("saldo insuficiente (" + saldos[indice] + ")");
                continue;
            }
            saldos[indice] -= valor;
            daVez.add(new Aposta(partes[0], valor));
            System.out.println("voce apostou " + daVez.get(daVez.size() - 1) + " (saldo " + saldos[indice] + ")");
        }
        return daVez;
    }

    // o programa inteiro fica parado mostrando a roleta, nao tem mais nada acontecendo
    static int girar() throws InterruptedException {
        int numero = sorteio.nextInt(37);
        long fim = System.currentTimeMillis() + DURACAO_GIRO_MS;
        while (System.currentTimeMillis() < fim) {
            System.out.print("\rgirando... " + sorteio.nextInt(37) + "   ");
            Thread.sleep(80);
        }
        System.out.println("\rdeu " + numero + " " + Aposta.corDe(numero) + "!        ");
        return numero;
    }

    static void apurar(List<String> nomes, int[] saldos, List<List<Aposta>> apostas, int numero) {
        for (int i = 0; i < nomes.size(); i++) {
            int ganho = 0;
            int perda = 0;
            for (Aposta aposta : apostas.get(i)) {
                if (aposta.ganhou(numero)) {
                    caixa -= aposta.premio() - aposta.valor();
                    saldos[i] += aposta.premio();
                    ganho += aposta.premio();
                } else {
                    caixa += aposta.valor();
                    perda += aposta.valor();
                }
            }
            System.out.println(nomes.get(i) + ": recebeu " + ganho + ", perdeu " + perda + ", saldo agora " + saldos[i]);
        }
    }

    static int lerInteiro(String pergunta, int padrao) {
        String resposta = lerLinha(pergunta + " [" + padrao + "]: ");
        return resposta.isEmpty() ? padrao : Integer.parseInt(resposta);
    }

    // acabou a entrada (ctrl+d ou arquivo redirecionado) = fecha o cassino
    static String lerLinha(String pergunta) {
        System.out.print(pergunta);
        if (!teclado.hasNextLine())
            System.exit(0);
        return teclado.nextLine().trim();
    }
}
