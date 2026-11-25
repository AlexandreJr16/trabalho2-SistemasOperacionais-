import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

// crash sem threads: o voo e um laco na thread da interface. Enquanto ele roda, o clique em SACAR
// fica parado na fila de eventos e so e lido quando o aviao ja caiu. Sem threads, o unico jeito de
// sair e decidir antes da decolagem (saque automatico)
public class CrashSemThreads {

    private final Voo voo = new Voo();
    private final Jogador humano;
    private final List<Jogador> jogadores;
    private final Cassino.Parametros parametros;
    private final GraficoCrash grafico = new GraficoCrash(voo);
    private final PainelJogadores painelJogadores;
    private final JSpinner valor = new JSpinner(new SpinnerNumberModel(50, 10, 1000, 10));
    private final JSpinner saqueAutomatico = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Voo.MULTIPLICADOR_MAXIMO, 0.1));
    private final Map<Jogador, Double> alvos = new LinkedHashMap<>();
    private final Random sorteio = new Random();
    private final JanelaJogo janela;
    private boolean jaLeuOSacar = false;
    private long fimDoVoo = Long.MAX_VALUE;

    CrashSemThreads(JFrame dono, Jogador humano, Cassino.Parametros parametros) {
        this.humano = humano;
        this.parametros = parametros;
        jogadores = Jogador.mesaCom(humano, parametros.bots());
        painelJogadores = new PainelJogadores(jogadores);
        JButton sacar = new JButton("SACAR");
        sacar.setFont(Estilo.fonte(26));
        sacar.setBackground(Estilo.VERDE);
        sacar.addActionListener(this::sacar);
        janela = new JanelaJogo(dono, Jogo.CRASH, false, grafico, painelJogadores,
                JanelaJogo.barraDeControles(Estilo.rotulo("Valor:", 14), valor,
                        Estilo.rotulo("  Saque automático, decidido antes de decolar (0 = desligado):", 14), saqueAutomatico,
                        Estilo.botao("APOSTAR E DECOLAR", Estilo.AMARELO, this::apostarEDecolar), sacar), this::atualizar);
        janela.mudarFase("Escolha o valor e clique em APOSTAR E DECOLAR", Estilo.DOURADO);
        janela.mostrar();
    }

    private void apostarEDecolar() {
        voo.abrirApostas();
        alvos.clear();
        jaLeuOSacar = false;
        fimDoVoo = Long.MAX_VALUE;
        grafico.avisar("");
        int quanto = (Integer) valor.getValue();
        double automatico = (Double) saqueAutomatico.getValue();
        if (voo.apostar(humano, quanto))
            humano.mudarStatus("apostou " + quanto + (automatico >= 1.01 ? ", automático em " + Estilo.vezes(automatico) : ", sem automático"));
        if (automatico >= 1.01)
            alvos.put(humano, automatico);
        for (Jogador bot : Jogador.soOsBots(jogadores))
            vezDoBot(bot);
        voar();
    }

    private void vezDoBot(Jogador bot) {
        painelJogadores.destacar(bot);
        grafico.chamar(bot.nome + " está apostando...");
        janela.mudarFase("Vez de " + bot.nome + " apostar: a janela não responde", Estilo.AMARELO);
        janela.atualizarAgora();
        JanelaJogo.dormir(parametros.atrasoDoBot(sorteio));
        double alvo = BotCrash.sortearAlvo(sorteio);
        int quanto = BotCrash.sortearFicha(sorteio);
        if (voo.apostar(bot, quanto)) {
            alvos.put(bot, alvo);
            bot.mudarStatus("apostou " + quanto + ", automático em " + Estilo.vezes(alvo));
        }
        janela.atualizarAgora();
    }

    // o laco do voo: a cada quadro confere os alvos de todo mundo e pinta a tela na marra
    private void voar() {
        painelJogadores.destacar(null);
        voo.decolar();
        janela.mudarFase("VOANDO! Tente clicar em SACAR...", Estilo.AMARELO);
        grafico.avisar("Sem threads, a única thread está presa neste laço do voo: o seu SACAR fica na fila\n"
                + "e só é lido quando o avião cair. Os bots saem porque marcaram o alvo ANTES de decolar\n"
                + "(saque automático, conferido dentro do laço). Você também pode: use o campo ao lado do valor.");
        while (voo.subir()) {
            sacarQuemChegouNoAlvo();
            janela.atualizarAgora();
            JanelaJogo.dormir(CrashComThreads.QUADRO_MS);
        }
        fimDoVoo = System.currentTimeMillis();
        for (Jogador jogador : voo.apostas().keySet())
            if (!voo.saidas().containsKey(jogador))
                jogador.mudarStatus("caiu junto com o avião");
        grafico.avisar("");
        janela.mudarFase("CAIU em " + Estilo.vezes(voo.multiplicador()) + ". Clique em APOSTAR E DECOLAR", Estilo.VERMELHO);
        janela.feed.evento("Caiu em " + Estilo.vezes(voo.multiplicador()) + ": " + voo.saidas().size() + " de " + voo.apostas().size() + " saíram a tempo", Estilo.VERMELHO);
        janela.atualizarAgora();
    }

    private void sacarQuemChegouNoAlvo() {
        for (Map.Entry<Jogador, Double> alvo : alvos.entrySet()) {
            if (voo.multiplicador() < alvo.getValue())
                continue;
            double multiplicador = voo.sacar(alvo.getKey());
            if (multiplicador > 0)
                alvo.getKey().mudarStatus("saiu no automático em " + Estilo.vezes(multiplicador));
        }
    }

    // so roda depois do laco do voo: a hora do clique (getWhen) mostra quando ele foi feito de verdade
    private void sacar(ActionEvent evento) {
        if (evento.getWhen() > fimDoVoo) {
            janela.feed.evento("Nada pra sacar: o avião já caiu", Estilo.TEXTO_FRACO);
            return;
        }
        if (jaLeuOSacar || voo.apostaDe(humano) == 0 || voo.saidas().containsKey(humano))
            return;
        jaLeuOSacar = true;
        long atraso = System.currentTimeMillis() - evento.getWhen();
        Placar.registrar(Jogo.CRASH, false, atraso);
        long clicouNoVoo = evento.getWhen() - voo.decolagem();
        String quando = clicouNoVoo < 0 ? "antes da decolagem" : "em " + Estilo.vezes(Voo.multiplicadorEm(clicouNoVoo));
        grafico.avisar("Você clicou SACAR " + quando + ", mas o programa estava preso no laço do voo.\n"
                + "O clique ficou " + Estilo.tempo(atraso) + " na fila e só foi lido agora, depois da queda em " + Estilo.vezes(voo.multiplicador()) + ".\n"
                + "Perdeu " + Estilo.dinheiro(voo.apostaDe(humano)) + ". Sem threads, só o saque automático salva.");
        janela.feed.evento("Seu SACAR esperou " + Estilo.tempo(atraso) + " na fila e chegou depois da queda", Estilo.VERMELHO);
    }

    private void atualizar() {
        janela.mudarInformacao("Voo " + voo.rodada() + "   ·   uma thread só faz tudo: a da interface ("
                + Thread.currentThread().getName() + ")");
    }
}
