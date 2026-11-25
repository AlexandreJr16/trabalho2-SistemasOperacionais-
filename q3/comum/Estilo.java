import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JLabel;

// cores, fontes e componentes basicos, pra todas as janelas terem a mesma cara
public class Estilo {

    static final Color FUNDO = new Color(20, 22, 28);
    static final Color PAINEL = new Color(32, 35, 44);
    static final Color FELTRO = new Color(12, 84, 48);
    static final Color DOURADO = new Color(222, 180, 60);
    static final Color TEXTO = new Color(236, 236, 236);
    static final Color TEXTO_FRACO = new Color(160, 165, 175);
    static final Color VERDE = new Color(70, 200, 100);
    static final Color VERMELHO = new Color(225, 65, 65);
    static final Color AMARELO = new Color(240, 200, 70);
    static final Color AZUL = new Color(90, 155, 240);
    static final Color COR_HUMANO = new Color(0, 220, 255);
    static final Color[] CORES = {new Color(255, 140, 0), new Color(186, 104, 255), new Color(255, 90, 160),
            new Color(150, 220, 70), new Color(255, 225, 90), new Color(110, 180, 255), new Color(255, 120, 110),
            new Color(80, 230, 190), new Color(230, 160, 255), new Color(200, 200, 200)};
    static final Locale BRASIL = Locale.of("pt", "BR");

    static Font fonte(int tamanho) {
        return new Font(Font.SANS_SERIF, Font.BOLD, tamanho);
    }

    static Color cor(int indice) {
        return CORES[indice % CORES.length];
    }

    static JLabel rotulo(String texto, int tamanho) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setForeground(TEXTO);
        rotulo.setFont(fonte(tamanho));
        return rotulo;
    }

    static JButton botao(String texto, Color cor, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.setFont(fonte(14));
        botao.setBackground(cor);
        botao.setForeground(Color.BLACK);
        botao.setFocusPainted(false);
        botao.addActionListener(evento -> acao.run());
        return botao;
    }

    static Graphics2D suave(Graphics grafico) {
        Graphics2D g = (Graphics2D) grafico;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static void textoCentrado(Graphics2D g, String texto, int centroX, int baseY) {
        g.drawString(texto, centroX - g.getFontMetrics().stringWidth(texto) / 2, baseY);
    }

    static String dinheiro(long valor) {
        return String.format(BRASIL, "R$ %,d", valor);
    }

    static String tempo(double ms) {
        if (ms < 1000)
            return String.format(BRASIL, "%.0f ms", ms);
        return String.format(BRASIL, "%.1f s", ms / 1000);
    }

    static String vezes(double multiplicador) {
        return String.format(BRASIL, "%.2fx", multiplicador);
    }
}
