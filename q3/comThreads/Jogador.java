import java.util.List;
import java.util.Random;

// cada bot e uma thread: dorme ate abrir a rodada e, enquanto as apostas estao abertas, fica
// pensando um tempo aleatorio e apostando, trocando ou cancelando, junto com todo mundo.
// O humano usa a mesma classe mas sem thread propria (quem chama e a interface)
public class Jogador implements Runnable {

    static final int MAXIMO_APOSTAS = 4;
    static final int[] FICHAS = {10, 25, 50, 100};

    final String nome;
    private final Mesa mesa;
    private final int atrasoMinimoMs;
    private final int atrasoMaximoMs;
    private final Random sorteio = new Random();
    private int saldo;
    private volatile String status = "esperando a rodada";

    Jogador(String nome, int saldoInicial, Mesa mesa, int atrasoMinimoMs, int atrasoMaximoMs) {
        this.nome = nome;
        this.saldo = saldoInicial;
        this.mesa = mesa;
        this.atrasoMinimoMs = atrasoMinimoMs;
        this.atrasoMaximoMs = atrasoMaximoMs;
    }

    @Override
    public void run() {
        Janela.log("thread criada, saldo " + saldo());
        int rodada = 0;
        try {
            while (true) {
                status = "dormindo, esperando a rodada";
                rodada = mesa.esperarProximaRodada(rodada);
                jogarRodada(rodada);
            }
        } catch (InterruptedException e) {
            Janela.log("thread finalizada");
        }
    }

    private void jogarRodada(int rodada) throws InterruptedException {
        Janela.log("acordou pra rodada " + rodada);
        while (mesa.aberta(rodada)) {
            status = "pensando...";
            Thread.sleep(atrasoMinimoMs + sorteio.nextInt(atrasoMaximoMs - atrasoMinimoMs + 1));
            agir();
        }
        status = "esperando o giro";
    }

    // depois do sleep a mesa pode ter fechado; ai a Mesa recusa e o bot fica sabendo que chegou tarde
    private void agir() {
        List<Aposta> minhas = mesa.apostasDe(this);
        int escolha = sorteio.nextInt(100);
        if (!minhas.isEmpty() && escolha < 20) {
            Aposta cancelada = mesa.cancelarUltima(this);
            avisar(cancelada != null, "cancelou " + cancelada);
            return;
        }
        if (!minhas.isEmpty() && escolha < 45) {
            String novoAlvo = Aposta.aleatoria(sorteio, 0).alvo();
            Aposta antiga = mesa.trocar(this, novoAlvo);
            avisar(antiga != null, antiga == null ? "" : "trocou " + antiga + " pro " + novoAlvo);
            return;
        }
        int ficha = FICHAS[sorteio.nextInt(FICHAS.length)];
        if (minhas.size() >= MAXIMO_APOSTAS || ficha > saldo())
            return;
        Aposta nova = Aposta.aleatoria(sorteio, ficha);
        avisar(mesa.apostar(this, nova), "apostou " + nova);
    }

    private void avisar(boolean conseguiu, String oQueFez) {
        if (!conseguiu) {
            status = "chegou tarde, mesa fechada";
            Janela.log("tentou mexer na aposta mas o crupie ja fechou a mesa");
            return;
        }
        status = oQueFez;
        Janela.log(oQueFez + " (saldo " + saldo() + ")");
    }

    synchronized boolean debitar(int valor) {
        if (valor > saldo)
            return false;
        saldo -= valor;
        return true;
    }

    synchronized void creditar(int valor) {
        saldo += valor;
    }

    synchronized int saldo() {
        return saldo;
    }

    String status() {
        return status;
    }

    void mudarStatus(String novoStatus) {
        status = novoStatus;
    }
}
