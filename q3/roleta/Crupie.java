import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

// thread que conduz as rodadas: abre, conta o tempo, fecha, manda girar e paga.
// Os jogadores agem por conta propria enquanto isso, ninguem espera a vez de ninguem
public class Crupie implements Runnable {

    static final int INTERVALO_ENTRE_RODADAS_MS = 5000;
    static final int CONFIRMACAO_DO_PAGAMENTO_MS = 600;

    private final Mesa mesa;
    private final TelaRoleta tela;
    private final JanelaJogo janela;
    private final int segundosDeAposta;
    private final Random sorteio = new Random();

    Crupie(Mesa mesa, TelaRoleta tela, JanelaJogo janela, int segundosDeAposta) {
        this.mesa = mesa;
        this.tela = tela;
        this.janela = janela;
        this.segundosDeAposta = segundosDeAposta;
    }

    @Override
    public void run() {
        try {
            while (true) {
                conduzirRodada();
                Thread.sleep(INTERVALO_ENTRE_RODADAS_MS);
            }
        } catch (InterruptedException e) {
            janela.feed.log("crupiê foi embora");
        }
    }

    private void conduzirRodada() throws InterruptedException {
        long inicio = System.currentTimeMillis();
        tela.limparResultado();
        mesa.abrirRodada();
        janela.feed.evento("Rodada " + mesa.rodada() + ": façam suas apostas!", Estilo.DOURADO);
        contarTempo();

        Map<Jogador, List<Aposta>> apostas = mesa.fecharApostas();
        janela.mudarFase("NÃO VAI MAIS!", Estilo.VERMELHO);
        janela.feed.evento("Não vai mais! " + mesa.acoesNaRodada() + " ações de " + apostas.size() + " jogadores nessa janela", Estilo.VERMELHO);

        int numero = sorteio.nextInt(37);
        janela.mudarFase("Girando...", Estilo.DOURADO);
        janela.monitor.iniciar("Animação da roda", () -> tela.roda.girar(numero)).join();
        tela.mostrarResultado(numero);
        janela.mudarFase("Deu " + numero + " " + Aposta.corDe(numero) + "! Pagando todo mundo ao mesmo tempo...", Estilo.DOURADO);
        apurar(apostas, numero);

        double duracao = System.currentTimeMillis() - inicio;
        Placar.registrar(Jogo.ROLETA, true, duracao);
        janela.mudarFase("Rodada levou " + Estilo.tempo(duracao) + ". Próxima já já", Estilo.DOURADO);
    }

    private void contarTempo() throws InterruptedException {
        for (int restantes = segundosDeAposta; restantes > 0; restantes--) {
            janela.mudarFase("APOSTAS ABERTAS: fecha em " + restantes + " s", restantes > 3 ? Estilo.VERDE : Estilo.AMARELO);
            Thread.sleep(1000);
        }
    }

    // uma thread de pagamento por jogador; cada uma espera a "confirmacao" ao mesmo tempo
    private void apurar(Map<Jogador, List<Aposta>> apostas, int numero) throws InterruptedException {
        List<Thread> pagamentos = new ArrayList<>();
        for (Map.Entry<Jogador, List<Aposta>> entrada : apostas.entrySet())
            pagamentos.add(janela.monitor.iniciar("Pagamento " + entrada.getKey().nome, () -> pagar(entrada.getKey(), entrada.getValue(), numero)));
        for (Thread pagamento : pagamentos)
            pagamento.join();
    }

    private void pagar(Jogador jogador, List<Aposta> apostas, int numero) {
        try {
            Thread.sleep(CONFIRMACAO_DO_PAGAMENTO_MS);
            acertarConta(jogador, apostas, numero, janela.feed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // usado pelas duas versoes: devolve o premio pro jogador e conta no feed se foi o humano
    static void acertarConta(Jogador jogador, List<Aposta> apostas, int numero, Feed feed) {
        int premio = 0;
        int apostado = 0;
        for (Aposta aposta : apostas) {
            apostado += aposta.valor();
            if (aposta.ganhou(numero))
                premio += aposta.premio();
        }
        jogador.creditar(premio);
        jogador.mudarStatus(premio > 0 ? "ganhou " + Estilo.dinheiro(premio) : "perdeu " + Estilo.dinheiro(apostado));
        feed.log(jogador.nome + ": apostou " + apostado + ", recebeu " + premio + ", saldo " + jogador.saldo());
        if (jogador.ehVoce())
            feed.evento(premio > 0 ? "Você ganhou " + Estilo.dinheiro(premio) + "!" : "Você perdeu " + Estilo.dinheiro(apostado), premio > 0 ? Estilo.VERDE : Estilo.VERMELHO);
    }
}
