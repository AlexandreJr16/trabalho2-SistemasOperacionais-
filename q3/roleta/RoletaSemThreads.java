import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JFrame;

// roleta sem threads: as mesmas regras, mas tudo roda na thread da interface. Cada jogador aposta
// na sua vez; enquanto um bot "pensa" (sleep) a janela inteira congela e os seus cliques ficam na
// fila. Quando a sua vez passa, nao da mais pra mudar nada
public class RoletaSemThreads {

    static final int CLIQUE_ATRASADO_MS = 300;

    private final Mesa mesa = new Mesa();
    private final Jogador humano;
    private final List<Jogador> jogadores;
    private final Cassino.Parametros parametros;
    private final TelaRoleta tela;
    private final JanelaJogo janela;
    private final Random sorteio = new Random();
    private boolean suaVez = false;
    private long inicioDaRodada;

    RoletaSemThreads(JFrame dono, Jogador humano, Cassino.Parametros parametros) {
        this.humano = humano;
        this.parametros = parametros;
        jogadores = Jogador.mesaCom(humano, parametros.bots());
        tela = new TelaRoleta(jogadores,
                Estilo.botao("ABRIR RODADA", Estilo.VERDE, this::abrirRodada),
                Estilo.botao("PASSAR A VEZ", Estilo.AMARELO, this::passarAVez));
        janela = new JanelaJogo(dono, Jogo.ROLETA, false, tela.centro, tela.painelJogadores, tela.controles, this::atualizar);
        tela.tabuleiro.aoClicar(this::clicou);
        janela.mudarFase("Clique em ABRIR RODADA", Estilo.DOURADO);
        janela.mostrar();
    }

    private void abrirRodada() {
        if (mesa.apostasAbertas())
            return;
        inicioDaRodada = System.currentTimeMillis();
        tela.limparResultado();
        mesa.abrirRodada();
        suaVez = true;
        tela.painelJogadores.destacar(humano);
        janela.mudarFase("SUA VEZ: aposte no pano e clique em PASSAR A VEZ", Estilo.VERDE);
        janela.feed.evento("Rodada " + mesa.rodada() + ": cada um aposta na sua vez, você primeiro", Estilo.DOURADO);
    }

    private void passarAVez() {
        if (!suaVez)
            return;
        suaVez = false;
        for (Jogador bot : Jogador.soOsBots(jogadores))
            vezDoBot(bot);
        fecharGirarPagar();
    }

    // o sleep aqui e na thread da interface: a tela so muda porque pintamos na marra
    private void vezDoBot(Jogador bot) {
        tela.painelJogadores.destacar(bot);
        janela.mudarFase("Vez de " + bot.nome + ": o programa está ocupado, a janela não responde", Estilo.AMARELO);
        bot.mudarStatus("pensando... (todo mundo esperando)");
        janela.atualizarAgora();
        int quantas = 1 + sorteio.nextInt(BotRoleta.MAXIMO_APOSTAS - 1);
        for (int i = 0; i < quantas; i++) {
            JanelaJogo.dormir(parametros.atrasoDoBot(sorteio));
            Aposta aposta = Aposta.aleatoria(sorteio);
            if (mesa.apostar(bot, aposta))
                bot.mudarStatus("apostou " + aposta);
            janela.feed.log(bot.nome + " apostou " + aposta);
            janela.atualizarAgora();
        }
        bot.mudarStatus(bot.status() + " e passou a vez");
    }

    private void fecharGirarPagar() {
        Map<Jogador, List<Aposta>> apostas = mesa.fecharApostas();
        tela.painelJogadores.destacar(null);
        janela.mudarFase("NÃO VAI MAIS! Girando...", Estilo.VERMELHO);
        janela.atualizarAgora();
        int numero = sorteio.nextInt(37);
        tela.roda.girar(numero);
        tela.mostrarResultado(numero);
        janela.mudarFase("Deu " + numero + " " + Aposta.corDe(numero) + "! Pagando um por um...", Estilo.DOURADO);
        for (Map.Entry<Jogador, List<Aposta>> entrada : apostas.entrySet()) {
            tela.painelJogadores.destacar(entrada.getKey());
            janela.atualizarAgora();
            JanelaJogo.dormir(Crupie.CONFIRMACAO_DO_PAGAMENTO_MS);
            Crupie.acertarConta(entrada.getKey(), entrada.getValue(), numero, janela.feed);
        }
        tela.painelJogadores.destacar(null);

        double duracao = System.currentTimeMillis() - inicioDaRodada;
        Placar.registrar(Jogo.ROLETA, false, duracao);
        janela.mudarFase("Rodada levou " + Estilo.tempo(duracao) + ". Clique em ABRIR RODADA", Estilo.DOURADO);
        janela.feed.evento("Rodada levou " + Estilo.tempo(duracao) + " com " + parametros.bots() + " bots, um depois do outro", Estilo.DOURADO);
    }

    // clique que chega com hora antiga ficou parado na fila enquanto o programa trabalhava
    private void clicou(String alvo, boolean tirar, long quando) {
        if (suaVez) {
            tela.jogadaDoHumano(mesa, humano, janela.feed, alvo, tirar);
            return;
        }
        boolean atrasado = System.currentTimeMillis() - quando > CLIQUE_ATRASADO_MS;
        janela.feed.evento(atrasado ? "Seu clique ficou na fila enquanto o programa trabalhava e só foi lido agora"
                : "Não é a sua vez: clique em ABRIR RODADA", Estilo.VERMELHO);
    }

    private void atualizar() {
        tela.tabuleiro.mostrar(mesa.retrato());
        janela.mudarInformacao("Rodada " + mesa.rodada() + "   ·   uma thread só faz tudo: a da interface ("
                + Thread.currentThread().getName() + ")");
    }
}
