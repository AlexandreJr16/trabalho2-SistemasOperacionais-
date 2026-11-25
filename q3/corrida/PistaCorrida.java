import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

// a pista com uma raia por cavalo; clicar numa raia escolhe o seu cavalo
public class PistaCorrida extends JComponent {

    static final int LARGURA_DOS_NOMES = 150;

    private volatile List<Cavalo> cavalos = List.of();
    private volatile int escolhido = -1;
    private volatile long inicio;
    private IntConsumer aoEscolher = indice -> {
    };

    PistaCorrida() {
        setPreferredSize(new Dimension(900, 430));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evento) {
                int raia = evento.getY() / alturaDaRaia();
                if (raia < cavalos.size())
                    aoEscolher.accept(raia);
            }
        });
    }

    void aoEscolher(IntConsumer ouvinte) {
        aoEscolher = ouvinte;
    }

    void mostrar(List<Cavalo> novosCavalos) {
        cavalos = novosCavalos;
        repaint();
    }

    void escolher(int indice) {
        escolhido = indice;
        repaint();
    }

    void marcarLargada(long instante) {
        inicio = instante;
    }

    private int alturaDaRaia() {
        return getHeight() / Math.max(1, cavalos.size());
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        Graphics2D g = Estilo.suave(grafico);
        List<Cavalo> agora = cavalos;
        int altura = alturaDaRaia();
        for (int i = 0; i < agora.size(); i++)
            desenharRaia(g, agora.get(i), i, altura);
        desenharChegada(g);
    }

    private void desenharRaia(Graphics2D g, Cavalo cavalo, int indice, int altura) {
        int topo = indice * altura;
        g.setColor(indice % 2 == 0 ? new Color(70, 120, 50) : new Color(80, 135, 58));
        g.fillRect(0, topo, getWidth(), altura);
        if (indice == escolhido) {
            g.setColor(new Color(0, 220, 255, 60));
            g.fillRect(0, topo, getWidth(), altura);
            g.setColor(Estilo.COR_HUMANO);
            g.setStroke(new BasicStroke(3));
            g.drawRect(1, topo + 1, getWidth() - 3, altura - 3);
            g.setStroke(new BasicStroke(1));
        }
        g.setColor(Color.WHITE);
        g.setFont(Estilo.fonte(15));
        g.drawString(cavalo.numero + " " + cavalo.nome + (indice == escolhido ? " (seu)" : ""), 8, topo + altura / 2 + 5);

        int percurso = getWidth() - LARGURA_DOS_NOMES - 120;
        int x = LARGURA_DOS_NOMES + (int) (cavalo.posicao() * percurso);
        desenharCavalo(g, cavalo, x, topo + altura / 2);
        if (cavalo.colocacao() > 0) {
            g.setColor(cavalo.colocacao() == 1 ? Estilo.DOURADO : Color.WHITE);
            g.setFont(Estilo.fonte(16));
            g.drawString(cavalo.colocacao() + "º  " + Estilo.tempo(cavalo.chegouEm() - inicio), getWidth() - 140, topo + 20);
        }
    }

    // corpo, cabeca e pernas que balancam conforme a posicao, pra parecer galope
    private void desenharCavalo(Graphics2D g, Cavalo cavalo, int x, int centroY) {
        double galope = Math.sin(cavalo.posicao() * 120);
        g.setColor(new Color(60, 35, 20));
        g.setStroke(new BasicStroke(3));
        g.drawLine(x + 8, centroY + 6, x + 8 + (int) (galope * 6), centroY + 20);
        g.drawLine(x + 30, centroY + 6, x + 30 - (int) (galope * 6), centroY + 20);
        g.setStroke(new BasicStroke(1));
        g.setColor(cavalo.cor);
        g.fillOval(x, centroY - 10, 42, 20);
        g.fillOval(x + 36, centroY - 18, 18, 14);
        g.setColor(Color.BLACK);
        g.setFont(Estilo.fonte(12));
        Estilo.textoCentrado(g, String.valueOf(cavalo.numero), x + 21, centroY + 5);
    }

    private void desenharChegada(Graphics2D g) {
        int x = getWidth() - 60;
        int quadrado = 10;
        for (int y = 0; y < getHeight(); y += quadrado)
            for (int coluna = 0; coluna < 2; coluna++) {
                g.setColor((y / quadrado + coluna) % 2 == 0 ? Color.WHITE : Color.BLACK);
                g.fillRect(x + coluna * quadrado, y, quadrado, quadrado);
            }
    }
}
