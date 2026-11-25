import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JComponent;

// uma caixinha por thread do jogo com o estado que a JVM informa (Thread.getState), pra ver na
// hora quem esta rodando, quem esta dormindo no sleep e quem esta parado esperando trava ou sinal
public class MonitorThreads extends JComponent {

    static final int LARGURA_CAIXA = 150;
    static final int ALTURA_CAIXA = 36;
    static final int ESPACO = 6;
    static final int TOPO = 46;

    private final List<Thread> threads = new CopyOnWriteArrayList<>();
    private volatile String legenda = "cada caixinha é uma thread deste jogo; a cor é o estado dela agora";

    MonitorThreads() {
        setPreferredSize(new Dimension(800, 210));
    }

    // cria sem iniciar; as que ja terminaram saem da tela quando chegam as novas
    Thread criar(Runnable tarefa, String nome) {
        threads.removeIf(thread -> thread.getState() == Thread.State.TERMINATED);
        Thread thread = new Thread(tarefa, nome);
        thread.setDaemon(true);
        threads.add(thread);
        return thread;
    }

    Thread iniciar(String nome, Runnable tarefa) {
        Thread thread = criar(tarefa, nome);
        thread.start();
        return thread;
    }

    void acompanharSozinha(Thread unica) {
        threads.add(unica);
        legenda = "sem threads: uma thread só (a da interface) faz tudo, e enquanto ela trabalha a janela congela";
    }

    // quem chama e a thread da interface fechando a janela; ela mesma nao pode ser interrompida
    void interromperTodas() {
        for (Thread thread : threads)
            if (thread != Thread.currentThread())
                thread.interrupt();
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        Graphics2D g = Estilo.suave(grafico);
        g.setColor(Estilo.PAINEL);
        g.fillRect(0, 0, getWidth(), getHeight());
        long vivas = threads.stream().filter(Thread::isAlive).count();
        g.setColor(Estilo.TEXTO);
        g.setFont(Estilo.fonte(15));
        g.drawString("Threads do jogo: " + vivas + " vivas", 10, 20);
        g.setFont(Estilo.fonte(11));
        g.setColor(Estilo.TEXTO_FRACO);
        g.drawString(legenda, 10, 37);
        desenharLegendaDeCores(g);

        int x = 10;
        int y = TOPO;
        for (Thread thread : threads) {
            if (x + LARGURA_CAIXA > getWidth()) {
                x = 10;
                y += ALTURA_CAIXA + ESPACO;
            }
            desenharCaixa(g, thread, x, y);
            x += LARGURA_CAIXA + ESPACO;
        }
    }

    private void desenharLegendaDeCores(Graphics2D g) {
        Thread.State[] estados = {Thread.State.RUNNABLE, Thread.State.TIMED_WAITING, Thread.State.WAITING, Thread.State.BLOCKED};
        int x = getWidth() - 10;
        for (Thread.State estado : estados)
            x -= g.getFontMetrics().stringWidth(nomeDoEstado(estado)) + 30;
        for (Thread.State estado : estados) {
            g.setColor(corDoEstado(estado));
            g.fillOval(x, 10, 10, 10);
            g.drawString(nomeDoEstado(estado), x + 14, 20);
            x += g.getFontMetrics().stringWidth(nomeDoEstado(estado)) + 30;
        }
    }

    private void desenharCaixa(Graphics2D g, Thread thread, int x, int y) {
        Thread.State estado = thread.getState();
        Color cor = corDoEstado(estado);
        g.setColor(cor.darker().darker());
        g.fillRoundRect(x, y, LARGURA_CAIXA, ALTURA_CAIXA, 10, 10);
        g.setColor(cor);
        g.drawRoundRect(x, y, LARGURA_CAIXA, ALTURA_CAIXA, 10, 10);
        g.setFont(Estilo.fonte(12));
        g.setColor(Color.WHITE);
        g.drawString(thread.getName(), x + 6, y + 15);
        g.setFont(Estilo.fonte(11));
        g.setColor(cor);
        g.drawString(nomeDoEstado(estado), x + 6, y + 30);
    }

    static Color corDoEstado(Thread.State estado) {
        return switch (estado) {
            case RUNNABLE -> Estilo.VERDE;
            case TIMED_WAITING -> Estilo.AMARELO;
            case WAITING -> Estilo.AZUL;
            case BLOCKED -> Estilo.VERMELHO;
            case NEW, TERMINATED -> Estilo.TEXTO_FRACO;
        };
    }

    static String nomeDoEstado(Thread.State estado) {
        return switch (estado) {
            case RUNNABLE -> "executando";
            case TIMED_WAITING -> "dormindo (sleep)";
            case WAITING -> "esperando sinal";
            case BLOCKED -> "bloqueada na trava";
            case NEW -> "criada";
            case TERMINATED -> "terminou";
        };
    }
}
