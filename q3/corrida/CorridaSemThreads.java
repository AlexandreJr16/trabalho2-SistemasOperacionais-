import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

// corrida sem threads: um laco so da um passo de cada cavalo por vez, e cada passo espera o ritmo
// daquele cavalo. As esperas se somam, entao a corrida dura bem mais, o cavalo rapido fica preso
// esperando os lentos e a chicotada so e lida quando a corrida ja acabou
public class CorridaSemThreads {

    private final Jogador humano;
    private final List<Jogador> jogadores;
    private final PistaCorrida pista = new PistaCorrida();
    private final BolaoCorrida bolao = new BolaoCorrida();
    private final JSpinner valor = new JSpinner(new SpinnerNumberModel(50, 10, 1000, 10));
    private final Random sorteio = new Random();
    private final JanelaJogo janela;
    private List<Cavalo> cavalos;
    private int escolhido = 0;
    private long inicioDaCorrida = 0;
    private long fimDaCorrida = 0;
    private int chicotadasAtrasadas = 0;

    CorridaSemThreads(JFrame dono, Jogador humano, Cassino.Parametros parametros) {
        this.humano = humano;
        jogadores = Jogador.mesaCom(humano, parametros.bots());
        JButton chicote = new JButton("CHICOTADA!");
        chicote.setFont(Estilo.fonte(14));
        chicote.setBackground(Estilo.VERMELHO);
        chicote.addActionListener(this::chicotear);
        janela = new JanelaJogo(dono, Jogo.CORRIDA, false, pista, new PainelJogadores(jogadores),
                JanelaJogo.barraDeControles(Estilo.rotulo("Clique numa raia pra escolher o cavalo.  Valor:", 14), valor,
                        Estilo.botao("APOSTAR E LARGAR", Estilo.AMARELO, this::apostarELargar),
                        chicote), this::atualizar);
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
        escolhido = indice;
        pista.escolher(indice);
    }

    private void apostarELargar() {
        novoPareo();
        if (!bolao.apostar(humano, cavalos.get(escolhido), (Integer) valor.getValue()))
            janela.feed.evento("Sem saldo: você só vai assistir", Estilo.VERMELHO);
        bolao.botsApostam(Jogador.soOsBots(jogadores), cavalos, sorteio);
        chicotadasAtrasadas = 0;
        correr();
    }

    private void correr() {
        AtomicInteger ordemDeChegada = new AtomicInteger();
        inicioDaCorrida = System.currentTimeMillis();
        pista.marcarLargada(inicioDaCorrida);
        janela.mudarFase("Correndo, um cavalo de cada vez (a janela não responde)", Estilo.AMARELO);
        while (cavalos.stream().anyMatch(cavalo -> !cavalo.chegou()))
            for (Cavalo cavalo : cavalos)
                passoDe(cavalo, ordemDeChegada);
        fimDaCorrida = System.currentTimeMillis();

        double duracao = fimDaCorrida - inicioDaCorrida;
        Placar.registrar(Jogo.CORRIDA, false, duracao);
        Cavalo vencedor = cavalos.stream().filter(cavalo -> cavalo.colocacao() == 1).findFirst().orElseThrow();
        bolao.pagar(vencedor, janela.feed);
        janela.mudarFase("Venceu " + vencedor.nome + "! Corrida levou " + Estilo.tempo(duracao), Estilo.DOURADO);
        janela.feed.evento("Venceu o " + vencedor.numero + " " + vencedor.nome + " em " + Estilo.tempo(duracao) + ", um passo de cada vez", vencedor.cor);
    }

    private void passoDe(Cavalo cavalo, AtomicInteger ordemDeChegada) {
        if (cavalo.chegou())
            return;
        try {
            cavalo.darPasso();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (cavalo.chegou())
            cavalo.cruzarAChegada(ordemDeChegada);
        janela.atualizarAgora();
    }

    // so e chamado depois que o laco da corrida termina; pela hora do clique (getWhen) da pra
    // saber se ele foi feito durante a corrida e ficou esperando na fila
    private void chicotear(ActionEvent evento) {
        if (evento.getWhen() < inicioDaCorrida || evento.getWhen() > fimDaCorrida) {
            janela.feed.evento("Não tem corrida acontecendo agora", Estilo.TEXTO_FRACO);
            return;
        }
        chicotadasAtrasadas++;
        janela.feed.evento(chicotadasAtrasadas + "ª chicotada lida só agora, depois da chegada: não adiantou nada", Estilo.VERMELHO);
    }

    private void atualizar() {
        janela.mudarInformacao("Seu cavalo: " + cavalos.get(escolhido).nome + "   ·   uma thread só faz tudo: a da interface ("
                + Thread.currentThread().getName() + ")");
    }
}
