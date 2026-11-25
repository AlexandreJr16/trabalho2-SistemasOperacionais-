import java.util.Random;

// cada bot e uma thread: dorme ate abrirem as apostas, pensa (sleep), aposta com um alvo e depois
// dorme na Condition do voo ate o multiplicador chegar no alvo. Se o aviao cair antes, perdeu
public class BotCrash implements Runnable {

    private final Jogador jogador;
    private final Voo voo;
    private final Feed feed;
    private final Cassino.Parametros parametros;
    private final Random sorteio = new Random();

    BotCrash(Jogador jogador, Voo voo, Feed feed, Cassino.Parametros parametros) {
        this.jogador = jogador;
        this.voo = voo;
        this.feed = feed;
        this.parametros = parametros;
    }

    // a maioria sai cedo e alguns arriscam: alvo entre 1,2x e uns 6x
    static double sortearAlvo(Random sorteio) {
        return 1.2 + Math.pow(sorteio.nextDouble(), 2) * 4.8;
    }

    static int sortearFicha(Random sorteio) {
        return Aposta.FICHAS[sorteio.nextInt(Aposta.FICHAS.length)];
    }

    @Override
    public void run() {
        int rodada = 0;
        try {
            while (true) {
                jogador.mudarStatus("dormindo até abrirem as apostas");
                rodada = voo.esperarApostas(rodada);
                jogarVoo();
            }
        } catch (InterruptedException e) {
            feed.log("thread finalizada");
        }
    }

    private void jogarVoo() throws InterruptedException {
        jogador.mudarStatus("pensando...");
        Thread.sleep(parametros.atrasoDoBot(sorteio));
        double alvo = sortearAlvo(sorteio);
        int valor = sortearFicha(sorteio);
        if (!voo.apostar(jogador, valor)) {
            jogador.mudarStatus("chegou tarde: o avião já decolou");
            feed.log("tentou apostar mas o avião já tinha decolado");
            return;
        }
        jogador.mudarStatus("apostou " + valor + ", sai em " + Estilo.vezes(alvo));
        feed.log("apostou " + valor + " e vai esperar " + Estilo.vezes(alvo));

        if (!voo.esperarPassar(alvo)) {
            jogador.mudarStatus("caiu junto com o avião: -" + valor);
            return;
        }
        double multiplicador = voo.sacar(jogador);
        jogador.mudarStatus("sacou em " + Estilo.vezes(multiplicador) + ": +" + Math.round(valor * multiplicador));
        feed.log("sacou em " + Estilo.vezes(multiplicador));
    }
}
