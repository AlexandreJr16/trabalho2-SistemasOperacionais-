import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

// corrida com threads: um juiz e uma thread por cavalo. Todos ficam parados no CountDownLatch ate
// a largada e depois correm ao mesmo tempo, cada um no seu ritmo; o juiz espera todos com join
public class CorridaComThreads {

    static final int CONTAGEM_DA_LARGADA = 3;

    private final Jogador humano;
    private final List<Jogador> jogadores;
    private final PistaCorrida pista = new PistaCorrida();
    private final BolaoCorrida bolao = new BolaoCorrida();
    private final JSpinner valor = new JSpinner(new SpinnerNumberModel(50, 10, 1000, 10));
    private final Random sorteio = new Random();
    private final JanelaJogo janela;
    private volatile List<Cavalo> cavalos;
    private volatile int escolhido = 0;
    private volatile boolean correndo = false;

    CorridaComThreads(JFrame dono, Jogador humano, Cassino.Parametros parametros) {
        this.humano = humano;
        jogadores = Jogador.mesaCom(humano, parametros.bots());
        janela = new JanelaJogo(dono, Jogo.CORRIDA, true, pista, new PainelJogadores(jogadores),
                JanelaJogo.barraDeControles(Estilo.rotulo("Clique numa raia pra escolher o cavalo.  Valor:", 14), valor,
                        Estilo.botao("APOSTAR E LARGAR", Estilo.AMARELO, this::apostarELargar),
                        Estilo.botao("CHICOTADA!", Estilo.VERMELHO, this::chicotear)), this::atualizar);
        pista.aoEscolher(this::escolher);
        novoPareo();
        janela.mudarFase("Escolha um cavalo e clique em APOSTAR E LARGAR", Estilo.DOURADO);
        janela.mostrar();
    }

    private void novoPareo() {
        cavalos = Cavalo.sortearPareo(sorteio);
        pista.mostrar(cavalos);
        pista.escolher(escolhido);
    }

    private void escolher(int indice) {
        if (correndo)
            return;
        escolhido = indice;
        pista.escolher(indice);
    }

    private void apostarELargar() {
        if (correndo)
            return;
        correndo = true;
        novoPareo();
        if (!bolao.apostar(humano, cavalos.get(escolhido), (Integer) valor.getValue()))
            janela.feed.evento("Sem saldo: você só vai assistir", Estilo.VERMELHO);
        bolao.botsApostam(Jogador.soOsBots(jogadores), cavalos, sorteio);
        janela.monitor.iniciar("Juiz da corrida", this::apitar);
    }

    private void apitar() {
        try {
            correr();
        } catch (InterruptedException e) {
            janela.feed.log("juiz foi embora");
        }
        correndo = false;
    }

    private void correr() throws InterruptedException {
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger ordemDeChegada = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (Cavalo cavalo : cavalos) {
            cavalo.prepararLargada(largada, ordemDeChegada);
            threads.add(janela.monitor.iniciar("Cavalo " + cavalo.numero, cavalo));
        }
        for (int restantes = CONTAGEM_DA_LARGADA; restantes > 0; restantes--) {
            janela.mudarFase("Largada em " + restantes + "... (os cavalos esperam no latch)", Estilo.AMARELO);
            Thread.sleep(1000);
        }
        long inicio = System.currentTimeMillis();
        pista.marcarLargada(inicio);
        largada.countDown();
        janela.mudarFase("LARGARAM! Todos correndo ao mesmo tempo", Estilo.VERDE);
        for (Thread thread : threads)
            thread.join();

        double duracao = System.currentTimeMillis() - inicio;
        Placar.registrar(Jogo.CORRIDA, true, duracao);
        Cavalo vencedor = cavalos.stream().filter(cavalo -> cavalo.colocacao() == 1).findFirst().orElseThrow();
        bolao.pagar(vencedor, janela.feed);
        janela.mudarFase("Venceu " + vencedor.nome + "! Corrida levou " + Estilo.tempo(duracao), Estilo.DOURADO);
        janela.feed.evento("Venceu o " + vencedor.numero + " " + vencedor.nome + " em " + Estilo.tempo(duracao), vencedor.cor);
    }

    // a interface esta livre durante a corrida, entao a chicotada chega no cavalo na hora
    private void chicotear() {
        if (!correndo) {
            janela.feed.evento("A corrida ainda não começou", Estilo.TEXTO_FRACO);
            return;
        }
        cavalos.get(escolhido).chicotear();
        humano.mudarStatus("chicotada no " + cavalos.get(escolhido).nome + "!");
    }

    private void atualizar() {
        janela.mudarInformacao("Seu cavalo: " + cavalos.get(escolhido).nome + "   ·   prêmio: " + BolaoCorrida.PREMIO
                + "x pra quem acertar o primeiro   ·   threads vivas no programa: " + Thread.activeCount());
    }
}
