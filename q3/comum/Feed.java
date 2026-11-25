import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// duas abas: "acontecendo agora" so com os poucos eventos que importam, em letra grande e
// sumindo devagar, e "log das threads" com tudo, cada linha com a hora e a thread que escreveu
public class Feed extends JTabbedPane {

    static final int EVENTOS_VISIVEIS = 6;
    static final int VIDA_DO_EVENTO_MS = 15000;
    static final int TAMANHO_MAXIMO_DO_LOG = 100000;
    static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    record Evento(String texto, Color cor, long instante) {
    }

    private final List<Evento> eventos = new ArrayList<>();
    private final JTextArea log = new JTextArea();
    private final JComponent quadro = new JComponent() {
        @Override
        protected void paintComponent(Graphics grafico) {
            desenharEventos(Estilo.suave(grafico), getWidth(), getHeight());
        }
    };

    Feed() {
        log.setEditable(false);
        log.setBackground(new Color(14, 14, 16));
        log.setForeground(new Color(190, 230, 190));
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        addTab("Acontecendo agora", quadro);
        addTab("Log das threads (tudo)", new JScrollPane(log));
        setPreferredSize(new Dimension(520, 210));
    }

    void evento(String texto, Color cor) {
        synchronized (eventos) {
            eventos.add(new Evento(texto, cor, System.currentTimeMillis()));
            if (eventos.size() > EVENTOS_VISIVEIS)
                eventos.remove(0);
        }
        log(texto);
    }

    // vai pro terminal tambem: o enunciado pede bastante print
    void log(String texto) {
        String linha = LocalTime.now().format(HORA) + "  [" + Thread.currentThread().getName() + "] " + texto;
        System.out.println(linha);
        naInterface(() -> escreverNoLog(linha));
    }

    // na versao sem threads quem chama ja e a thread da interface, entao escreve na hora
    static void naInterface(Runnable acao) {
        if (SwingUtilities.isEventDispatchThread())
            acao.run();
        else
            SwingUtilities.invokeLater(acao);
    }

    private void escreverNoLog(String linha) {
        log.append(linha + "\n");
        if (log.getDocument().getLength() > TAMANHO_MAXIMO_DO_LOG)
            log.replaceRange("", 0, TAMANHO_MAXIMO_DO_LOG / 2);
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void desenharEventos(Graphics2D g, int largura, int altura) {
        g.setColor(Estilo.PAINEL);
        g.fillRect(0, 0, largura, altura);
        List<Evento> copia;
        synchronized (eventos) {
            copia = new ArrayList<>(eventos);
        }
        long agora = System.currentTimeMillis();
        int y = 26;
        g.setFont(Estilo.fonte(15));
        for (int i = copia.size() - 1; i >= 0 && y < altura; i--) {
            Evento evento = copia.get(i);
            float transparencia = Math.max(0.25f, 1f - (agora - evento.instante()) / (float) VIDA_DO_EVENTO_MS);
            Color cor = evento.cor();
            g.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), (int) (255 * transparencia)));
            g.fillOval(10, y - 11, 10, 10);
            g.drawString(evento.texto(), 28, y);
            y += 28;
        }
    }
}
