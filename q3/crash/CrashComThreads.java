import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

// crash com threads: a torre de controle e uma thread que faz o aviao subir, cada bot e uma thread
// esperando o proprio alvo, e a interface fica livre pra atender o SACAR do humano na hora
public class CrashComThreads {

    static final int SEGUNDOS_DE_APOSTA = 7;
    static final int QUADRO_MS = 40;
    static final int PAUSA_ENTRE_VOOS_MS = 5000;

    private final Voo voo = new Voo();
    private final Jogador humano;
    private final List<Jogador> jogadores;
    private final GraficoCrash grafico = new GraficoCrash(voo);
    private final JSpinner valor = new JSpinner(new SpinnerNumberModel(50, 10, 1000, 10));
    private final JButton apostar = Estilo.botao("APOSTAR", Estilo.AMARELO, this::apostar);
    private final JButton sacar = new JButton("SACAR");
    private final JanelaJogo janela;

    CrashComThreads(JFrame dono, Jogador humano, Cassino.Parametros parametros) {
        this.humano = humano;
        jogadores = Jogador.mesaCom(humano, parametros.bots());
        sacar.setFont(Estilo.fonte(26));
        sacar.setBackground(Estilo.VERDE);
        sacar.addActionListener(this::sacar);
        janela = new JanelaJogo(dono, Jogo.CRASH, true, grafico, new PainelJogadores(jogadores),
                JanelaJogo.barraDeControles(Estilo.rotulo("Valor:", 14), valor,
                        apostar, sacar), this::atualizar);

        for (Jogador bot : Jogador.soOsBots(jogadores))
            janela.monitor.iniciar(bot.nome, new BotCrash(bot, voo, janela.feed, parametros));
        janela.monitor.iniciar("Torre de controle", this::controlarVoos);
        janela.mostrar();
    }

    private void controlarVoos() {
        try {
            while (true) {
                umVoo();
                Thread.sleep(PAUSA_ENTRE_VOOS_MS);
            }
        } catch (InterruptedException e) {
            janela.feed.log("torre fechada");
        }
    }

    private void umVoo() throws InterruptedException {
        voo.abrirApostas();
        grafico.avisar("");
        janela.feed.evento("Voo " + voo.rodada() + ": apostas abertas", Estilo.DOURADO);
        for (int restantes = SEGUNDOS_DE_APOSTA; restantes > 0; restantes--) {
            grafico.chamar("Aposte agora! Decola em " + restantes + " s");
            janela.mudarFase("APOSTAS ABERTAS: decola em " + restantes + " s", Estilo.VERDE);
            Thread.sleep(1000);
        }
        voo.decolar();
        janela.mudarFase("VOANDO! Clique em SACAR antes de cair", Estilo.AMARELO);
        while (voo.subir())
            Thread.sleep(QUADRO_MS);
        janela.mudarFase("CAIU em " + Estilo.vezes(voo.multiplicador()), Estilo.VERMELHO);
        contarResultado();
    }

    private void contarResultado() {
        Map<Jogador, Integer> apostas = voo.apostas();
        Map<Jogador, Voo.Saida> saidas = voo.saidas();
        janela.feed.evento("Caiu em " + Estilo.vezes(voo.multiplicador()) + ": " + saidas.size() + " de " + apostas.size() + " saíram a tempo", Estilo.VERMELHO);
        if (apostas.containsKey(humano) && !saidas.containsKey(humano))
            janela.feed.evento("Você não sacou: perdeu " + Estilo.dinheiro(apostas.get(humano)), Estilo.VERMELHO);
    }

    private void apostar() {
        int quanto = (Integer) valor.getValue();
        if (!voo.apostar(humano, quanto)) {
            janela.feed.evento("Só dá pra apostar uma vez, antes da decolagem e com saldo", Estilo.VERMELHO);
            return;
        }
        humano.mudarStatus("apostou " + quanto);
        janela.feed.evento("Você apostou " + Estilo.dinheiro(quanto) + ". Agora é com você: SACAR!", humano.cor);
    }

    // getWhen e a hora do clique; a diferenca pra agora e quanto o clique esperou pra ser lido
    private void sacar(ActionEvent evento) {
        double multiplicador = voo.sacar(humano);
        long atraso = System.currentTimeMillis() - evento.getWhen();
        if (multiplicador == 0) {
            janela.feed.evento("Nada pra sacar agora", Estilo.TEXTO_FRACO);
            return;
        }
        Placar.registrar(Jogo.CRASH, true, atraso);
        long recebido = Math.round(voo.apostaDe(humano) * multiplicador);
        humano.mudarStatus("sacou em " + Estilo.vezes(multiplicador) + ": +" + recebido);
        janela.feed.evento("Você sacou em " + Estilo.vezes(multiplicador) + " e recebeu " + Estilo.dinheiro(recebido)
                + " (clique lido em " + atraso + " ms)", Estilo.VERDE);
    }

    // cada botao so acende quando da pra usar: APOSTAR antes da decolagem, SACAR durante o voo
    private void atualizar() {
        Voo.Fase fase = voo.fase();
        boolean apostou = voo.apostaDe(humano) > 0;
        apostar.setEnabled(fase == Voo.Fase.APOSTAS && !apostou);
        sacar.setEnabled(fase == Voo.Fase.VOANDO && apostou && !voo.saidas().containsKey(humano));
        janela.mudarInformacao("Voo " + voo.rodada() + "   ·   apostaram " + voo.apostas().size() + "   ·   já saíram "
                + voo.saidas().size() + "   ·   threads vivas no programa: " + Thread.activeCount());
    }
}
