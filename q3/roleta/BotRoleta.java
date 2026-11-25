import java.util.List;
import java.util.Random;

// cada bot e uma thread: dorme na Condition da mesa ate abrir a rodada e, enquanto as apostas
// estao abertas, pensa um tempo aleatorio (sleep) e aposta, troca ou tira a ficha, junto com
// todo mundo. Quando acorda depois do "nao vai mais", a mesa recusa e ele chega tarde
public class BotRoleta implements Runnable {

    static final int MAXIMO_APOSTAS = 4;

    private final Jogador jogador;
    private final Mesa mesa;
    private final Feed feed;
    private final Cassino.Parametros parametros;
    private final Random sorteio = new Random();

    BotRoleta(Jogador jogador, Mesa mesa, Feed feed, Cassino.Parametros parametros) {
        this.jogador = jogador;
        this.mesa = mesa;
        this.feed = feed;
        this.parametros = parametros;
    }

    @Override
    public void run() {
        feed.log("thread criada, saldo " + jogador.saldo());
        int rodada = 0;
        try {
            while (true) {
                jogador.mudarStatus("dormindo até abrir a mesa");
                rodada = mesa.esperarProximaRodada(rodada);
                jogarRodada(rodada);
            }
        } catch (InterruptedException e) {
            feed.log("thread finalizada");
        }
    }

    private void jogarRodada(int rodada) throws InterruptedException {
        feed.log("acordou pra rodada " + rodada);
        jogador.mudarStatus("pensando...");
        while (mesa.aberta(rodada)) {
            Thread.sleep(parametros.atrasoDoBot(sorteio));
            agir();
        }
    }

    private void agir() {
        List<Aposta> minhas = mesa.apostasDe(jogador);
        int escolha = sorteio.nextInt(100);
        if (!minhas.isEmpty() && escolha < 20) {
            Aposta tirada = mesa.cancelarUltima(jogador);
            contar(tirada != null, "tirou " + tirada);
            return;
        }
        if (!minhas.isEmpty() && escolha < 45) {
            String novoAlvo = Aposta.aleatoria(sorteio).alvo();
            Aposta antiga = mesa.trocar(jogador, novoAlvo);
            contar(antiga != null, antiga == null ? "" : "mudou " + antiga + " pro " + Aposta.nomeDoAlvo(novoAlvo));
            return;
        }
        if (minhas.size() >= MAXIMO_APOSTAS)
            return;
        Aposta nova = Aposta.aleatoria(sorteio);
        contar(mesa.apostar(jogador, nova), "apostou " + nova);
    }

    private void contar(boolean conseguiu, String oQueFez) {
        if (conseguiu) {
            jogador.mudarStatus(oQueFez);
            feed.log(oQueFez + " (saldo " + jogador.saldo() + ")");
            return;
        }
        if (mesa.apostasAbertas())
            return;
        jogador.mudarStatus("chegou tarde: mesa fechada");
        feed.evento(jogador.nome + " tentou mexer depois do NÃO VAI MAIS e foi recusado", jogador.cor);
    }
}
