import java.util.List;
import javax.swing.JFrame;

// roleta com threads: uma thread por bot, uma do crupie, uma de animacao e uma por pagamento.
// O humano aposta pelo pano (thread da interface) ao mesmo tempo que todo mundo
public class RoletaComThreads {

    private final Mesa mesa = new Mesa();
    private final Jogador humano;
    private final TelaRoleta tela;
    private final JanelaJogo janela;

    RoletaComThreads(JFrame dono, Jogador humano, Cassino.Parametros parametros) {
        this.humano = humano;
        List<Jogador> jogadores = Jogador.mesaCom(humano, parametros.bots());
        tela = new TelaRoleta(jogadores, Estilo.botao("Tirar todas as minhas", Estilo.AMARELO, this::tirarTodas));
        janela = new JanelaJogo(dono, Jogo.ROLETA, true, tela.centro, tela.painelJogadores, tela.controles, this::atualizar);
        tela.tabuleiro.aoClicar((alvo, tirar, quando) -> tela.jogadaDoHumano(mesa, humano, janela.feed, alvo, tirar));

        for (Jogador bot : Jogador.soOsBots(jogadores))
            janela.monitor.iniciar(bot.nome, new BotRoleta(bot, mesa, janela.feed, parametros));
        janela.monitor.iniciar("Crupiê", new Crupie(mesa, tela, janela, parametros.segundosDeAposta()));
        janela.mostrar();
    }

    private void tirarTodas() {
        int tiradas = mesa.cancelarTodas(humano);
        janela.feed.evento(tiradas > 0 ? "Você tirou " + tiradas + " fichas" : "Nada pra tirar (ou a mesa já fechou)", humano.cor);
    }

    private void atualizar() {
        tela.tabuleiro.mostrar(mesa.retrato());
        janela.mudarInformacao("Rodada " + mesa.rodada() + "   ·   ações nesta janela de apostas: " + mesa.acoesNaRodada()
                + "   ·   threads vivas no programa: " + Thread.activeCount());
    }
}
