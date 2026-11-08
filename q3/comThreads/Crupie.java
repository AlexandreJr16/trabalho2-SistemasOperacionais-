import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// thread que conduz as rodadas: abre, conta o tempo, fecha, manda girar e apura.
// Os jogadores agem por conta propria enquanto isso, ninguem espera a vez de ninguem
public class Crupie implements Runnable {

    static final int INTERVALO_ENTRE_RODADAS_MS = 4000;

    private final Mesa mesa;
    private final Caixa caixa;
    private final List<Jogador> jogadores;
    private final PainelRoleta roleta;
    private final int segundosDeAposta;
    private final long dinheiroTotal;
    private final Random sorteio = new Random();
    private volatile String fase = "preparando a mesa";
    private volatile int segundosRestantes = 0;
    private boolean pausado = false;

    Crupie(Mesa mesa, Caixa caixa, List<Jogador> jogadores, PainelRoleta roleta, int segundosDeAposta) {
        this.mesa = mesa;
        this.caixa = caixa;
        this.jogadores = jogadores;
        this.roleta = roleta;
        this.segundosDeAposta = segundosDeAposta;
        this.dinheiroTotal = contarDinheiro();
    }

    @Override
    public void run() {
        try {
            while (true) {
                esperarSeEstiverPausado();
                conduzirRodada();
                Thread.sleep(INTERVALO_ENTRE_RODADAS_MS);
            }
        } catch (InterruptedException e) {
            Janela.log("crupie foi embora");
        }
    }

    private void conduzirRodada() throws InterruptedException {
        long inicio = System.currentTimeMillis();
        mesa.abrirRodada();
        fase = "APOSTAS ABERTAS";
        Janela.log("=== rodada " + mesa.rodada() + ": facam suas apostas! ===");
        contarTempo();

        Map<Jogador, List<Aposta>> apostas = mesa.fecharApostas();
        fase = "NAO VAI MAIS";
        Janela.log("nao vai mais! " + mesa.acoesNaRodada() + " acoes de " + apostas.size() + " jogadores nessa janela");

        int numero = sorteio.nextInt(37);
        fase = "GIRANDO";
        Thread animacao = roleta.girar(numero);
        animacao.join();
        Janela.log("deu " + numero + " " + Aposta.corDe(numero) + "!");

        fase = "PAGANDO";
        apurar(apostas, numero);
        double segundos = (System.currentTimeMillis() - inicio) / 1000.0;
        Janela.log(String.format("rodada %d levou %.1f s, caixa da casa %d, dinheiro total %d (tem que ser %d)",
                mesa.rodada(), segundos, caixa.saldo(), contarDinheiro(), dinheiroTotal));
        fase = "proxima rodada ja ja";
    }

    private void contarTempo() throws InterruptedException {
        for (segundosRestantes = segundosDeAposta; segundosRestantes > 0; segundosRestantes--)
            Thread.sleep(1000);
    }

    // uma thread de pagamento por jogador, todas mexendo no caixa ao mesmo tempo
    private void apurar(Map<Jogador, List<Aposta>> apostas, int numero) throws InterruptedException {
        List<Thread> pagamentos = new ArrayList<>();
        for (Map.Entry<Jogador, List<Aposta>> entrada : apostas.entrySet()) {
            Thread pagamento = new Thread(() -> acertarConta(entrada.getKey(), entrada.getValue(), numero), "Pagamento " + entrada.getKey().nome);
            pagamentos.add(pagamento);
            pagamento.start();
        }
        for (Thread pagamento : pagamentos)
            pagamento.join();
    }

    private void acertarConta(Jogador jogador, List<Aposta> apostas, int numero) {
        int ganho = 0;
        int perda = 0;
        for (Aposta aposta : apostas) {
            if (aposta.ganhou(numero)) {
                caixa.pagar(aposta.premio() - aposta.valor());
                jogador.creditar(aposta.premio());
                ganho += aposta.premio();
            } else {
                caixa.receber(aposta.valor());
                perda += aposta.valor();
            }
        }
        jogador.mudarStatus(ganho > 0 ? "ganhou " + ganho : "perdeu " + perda);
        Janela.log(jogador.nome + ": recebeu " + ganho + ", perdeu " + perda + ", saldo agora " + jogador.saldo());
    }

    private long contarDinheiro() {
        long total = caixa.saldo();
        for (Jogador jogador : jogadores)
            total += jogador.saldo();
        return total;
    }

    private synchronized void esperarSeEstiverPausado() throws InterruptedException {
        if (pausado)
            fase = "PAUSADO";
        while (pausado)
            wait();
    }

    synchronized boolean alternarPausa() {
        pausado = !pausado;
        notifyAll();
        return pausado;
    }

    String fase() {
        return fase;
    }

    int segundosRestantes() {
        return segundosRestantes;
    }
}
