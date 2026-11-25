import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;

// moldura comum dos jogos: modo e fase no topo, o jogo no meio, jogadores do lado, controles
// embaixo e no rodape o monitor de threads e o feed. Com threads a janela e independente (da
// pra abrir varios jogos); sem threads ela e modal e trava o lobby, como um programa sequencial
public class JanelaJogo {

    static final int ATUALIZACAO_MS = 100;

    final Feed feed = new Feed();
    final MonitorThreads monitor = new MonitorThreads();
    private final JDialog janela;
    private final JPanel conteudo = new JPanel(new BorderLayout(8, 8));
    private final JLabel fase = Estilo.rotulo(" ", 26);
    private final JLabel informacao = Estilo.rotulo(" ", 14);
    private final Runnable aoAtualizar;
    private final Timer atualizacao;

    JanelaJogo(JFrame dono, Jogo jogo, boolean comThreads, JComponent centro, JComponent lateral, JComponent controles, Runnable aoAtualizar) {
        this.aoAtualizar = aoAtualizar;
        janela = new JDialog(dono, "JohnMarcos Bet - " + jogo.titulo + (comThreads ? " (com threads)" : " (sem threads)"),
                comThreads ? Dialog.ModalityType.MODELESS : Dialog.ModalityType.APPLICATION_MODAL);
        conteudo.setBackground(Estilo.FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        conteudo.add(cabecalho(jogo, comThreads), BorderLayout.NORTH);
        conteudo.add(meio(centro, lateral, controles), BorderLayout.CENTER);
        conteudo.add(rodape(), BorderLayout.SOUTH);
        janela.setContentPane(conteudo);
        janela.setSize(1380, 940);
        janela.setLocationRelativeTo(dono);
        janela.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        atualizacao = new Timer(ATUALIZACAO_MS, evento -> atualizarTela());
        janela.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent evento) {
                atualizacao.stop();
                monitor.interromperTodas();
            }
        });
        if (!comThreads)
            monitor.acompanharSozinha(Thread.currentThread());
    }

    // com threads volta na hora; sem threads (modal) so volta quando a janela fecha
    void mostrar() {
        atualizacao.start();
        janela.setVisible(true);
    }

    void mudarFase(String texto, Color cor) {
        Feed.naInterface(() -> {
            fase.setText(texto);
            fase.setForeground(cor);
        });
    }

    void mudarInformacao(String texto) {
        Feed.naInterface(() -> informacao.setText(texto));
    }

    // a versao sem threads chama isto no meio dos lacos: e o unico jeito de a tela mudar
    // enquanto a thread da interface esta ocupada. Os cliques continuam na fila
    void atualizarAgora() {
        aoAtualizar.run();
        conteudo.validate();
        conteudo.paintImmediately(0, 0, conteudo.getWidth(), conteudo.getHeight());
    }

    private void atualizarTela() {
        aoAtualizar.run();
        conteudo.repaint();
    }

    private JPanel cabecalho(Jogo jogo, boolean comThreads) {
        JLabel modo = Estilo.rotulo(comThreads ? "  COM THREADS  " : "  SEM THREADS  ", 16);
        modo.setOpaque(true);
        modo.setBackground(comThreads ? Estilo.VERDE.darker() : Estilo.VERMELHO.darker());
        JPanel esquerda = new JPanel(new BorderLayout(10, 0));
        esquerda.setOpaque(false);
        esquerda.add(modo, BorderLayout.WEST);
        esquerda.add(Estilo.rotulo(jogo.titulo, 22), BorderLayout.CENTER);

        JPanel cabecalho = new JPanel(new BorderLayout(20, 4));
        cabecalho.setBackground(Estilo.PAINEL);
        cabecalho.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        cabecalho.add(esquerda, BorderLayout.WEST);
        cabecalho.add(fase, BorderLayout.CENTER);
        cabecalho.add(informacao, BorderLayout.SOUTH);
        return cabecalho;
    }

    private JPanel meio(JComponent centro, JComponent lateral, JComponent controles) {
        JPanel meio = new JPanel(new BorderLayout(8, 8));
        meio.setOpaque(false);
        meio.add(centro, BorderLayout.CENTER);
        meio.add(lateral, BorderLayout.EAST);
        meio.add(controles, BorderLayout.SOUTH);
        return meio;
    }

    private JPanel rodape() {
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        rodape.setOpaque(false);
        rodape.add(monitor, BorderLayout.CENTER);
        rodape.add(feed, BorderLayout.EAST);
        return rodape;
    }

    // a versao sem threads dorme na propria thread da interface, de proposito
    static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static JPanel barraDeControles(JComponent... componentes) {
        JPanel barra = new JPanel();
        barra.setBackground(Estilo.PAINEL);
        for (JComponent componente : componentes)
            barra.add(componente);
        return barra;
    }
}
