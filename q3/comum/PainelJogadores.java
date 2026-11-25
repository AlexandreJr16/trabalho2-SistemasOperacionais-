import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JComponent;

// lista lateral com saldo e status de cada jogador; na versao sem threads destaca de quem e a vez
public class PainelJogadores extends JComponent {

    static final int ALTURA_LINHA = 52;

    private final List<Jogador> jogadores;
    private volatile Jogador daVez;

    PainelJogadores(List<Jogador> jogadores) {
        this.jogadores = jogadores;
        setPreferredSize(new Dimension(300, ALTURA_LINHA * jogadores.size() + 34));
    }

    void destacar(Jogador jogador) {
        daVez = jogador;
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        Graphics2D g = Estilo.suave(grafico);
        g.setColor(Estilo.PAINEL);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Estilo.DOURADO);
        g.setFont(Estilo.fonte(15));
        g.drawString("Jogadores na mesa", 10, 22);
        int y = 34;
        for (Jogador jogador : jogadores) {
            desenharLinha(g, jogador, y);
            y += ALTURA_LINHA;
        }
    }

    private void desenharLinha(Graphics2D g, Jogador jogador, int y) {
        if (jogador == daVez) {
            g.setColor(new Color(90, 80, 20));
            g.fillRoundRect(4, y, getWidth() - 8, ALTURA_LINHA - 4, 10, 10);
        }
        g.setColor(jogador.cor);
        g.fillOval(10, y + 8, 16, 16);
        g.setFont(Estilo.fonte(14));
        g.drawString(jogador.nome, 34, y + 21);
        String saldo = Estilo.dinheiro(jogador.saldo());
        g.setColor(Estilo.TEXTO);
        g.drawString(saldo, getWidth() - 12 - g.getFontMetrics().stringWidth(saldo), y + 21);
        g.setFont(Estilo.fonte(12));
        g.setColor(Estilo.TEXTO_FRACO);
        g.drawString(jogador.status(), 34, y + 40);
    }
}
