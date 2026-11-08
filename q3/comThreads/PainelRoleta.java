import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

// desenho da roleta. Quem gira e uma thread propria de animacao, entao a tela continua
// respondendo (e os jogadores continuam vivos) enquanto ela roda
public class PainelRoleta extends JComponent {

    static final int[] ORDEM_DA_RODA = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};
    static final double PASSO = 360.0 / ORDEM_DA_RODA.length;
    static final int DURACAO_GIRO_MS = 4500;
    static final int QUADRO_MS = 16;

    private volatile double angulo = 0;
    private volatile int numeroSorteado = -1;

    PainelRoleta() {
        setPreferredSize(new Dimension(360, 360));
    }

    Thread girar(int numero) {
        numeroSorteado = -1;
        Thread animacao = new Thread(() -> animar(numero), "Animacao roleta");
        animacao.start();
        return animacao;
    }

    // a casa do numero tem que parar com o meio dela embaixo do ponteiro (90 graus = topo),
    // depois de umas 5 voltas desacelerando
    private void animar(int numero) {
        double anguloInicial = angulo;
        double alvo = 90 - posicaoNaRoda(numero) * PASSO - PASSO / 2;
        double voltas = Math.ceil((anguloInicial + 1800 - alvo) / 360);
        double anguloFinal = alvo + 360 * voltas;
        long inicio = System.currentTimeMillis();
        double progresso = 0;
        while (progresso < 1) {
            progresso = Math.min(1, (System.currentTimeMillis() - inicio) / (double) DURACAO_GIRO_MS);
            double desacelerado = 1 - Math.pow(1 - progresso, 3);
            angulo = anguloInicial + (anguloFinal - anguloInicial) * desacelerado;
            repaint();
            dormir();
        }
        angulo = anguloFinal % 360;
        numeroSorteado = numero;
        repaint();
    }

    private void dormir() {
        try {
            Thread.sleep(QUADRO_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static int posicaoNaRoda(int numero) {
        for (int i = 0; i < ORDEM_DA_RODA.length; i++)
            if (ORDEM_DA_RODA[i] == numero)
                return i;
        return 0;
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        Graphics2D g = (Graphics2D) grafico;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int diametro = Math.min(getWidth(), getHeight()) - 30;
        int x = (getWidth() - diametro) / 2;
        int y = (getHeight() - diametro) / 2 + 10;
        double anguloAgora = angulo;

        desenharCasas(g, x, y, diametro, anguloAgora);
        desenharMiolo(g, x, y, diametro);
        desenharPonteiro(g, getWidth() / 2, y);
    }

    private void desenharCasas(Graphics2D g, int x, int y, int diametro, double anguloAgora) {
        double centroX = x + diametro / 2.0;
        double centroY = y + diametro / 2.0;
        double raioTexto = diametro / 2.0 * 0.86;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, diametro / 32)));
        for (int i = 0; i < ORDEM_DA_RODA.length; i++) {
            int numero = ORDEM_DA_RODA[i];
            double inicioCasa = anguloAgora + i * PASSO;
            g.setColor(corDaCasa(numero));
            g.fillArc(x, y, diametro, diametro, (int) Math.round(inicioCasa), (int) Math.ceil(PASSO));
            double meio = Math.toRadians(inicioCasa + PASSO / 2);
            String texto = String.valueOf(numero);
            int largura = g.getFontMetrics().stringWidth(texto);
            g.setColor(Color.WHITE);
            g.drawString(texto, (int) (centroX + raioTexto * Math.cos(meio)) - largura / 2, (int) (centroY - raioTexto * Math.sin(meio)) + 4);
        }
        g.setColor(new Color(212, 175, 55));
        g.setStroke(new BasicStroke(4));
        g.drawOval(x, y, diametro, diametro);
    }

    private void desenharMiolo(Graphics2D g, int x, int y, int diametro) {
        int diametroMiolo = (int) (diametro * 0.68);
        int xMiolo = x + (diametro - diametroMiolo) / 2;
        int yMiolo = y + (diametro - diametroMiolo) / 2;
        g.setColor(new Color(60, 35, 20));
        g.fillOval(xMiolo, yMiolo, diametroMiolo, diametroMiolo);
        g.setColor(new Color(212, 175, 55));
        g.drawOval(xMiolo, yMiolo, diametroMiolo, diametroMiolo);

        int sorteado = numeroSorteado;
        String texto = sorteado < 0 ? "JohnMarcos Bet" : sorteado + " " + Aposta.corDe(sorteado);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, sorteado < 0 ? diametro / 16 : diametro / 9));
        int largura = g.getFontMetrics().stringWidth(texto);
        g.setColor(sorteado < 0 ? new Color(212, 175, 55) : corDaCasa(sorteado).brighter());
        g.drawString(texto, x + (diametro - largura) / 2, y + diametro / 2 + g.getFont().getSize() / 3);
    }

    private void desenharPonteiro(Graphics2D g, int centroX, int topo) {
        int[] xs = {centroX - 10, centroX + 10, centroX};
        int[] ys = {topo - 14, topo - 14, topo + 8};
        g.setColor(Color.WHITE);
        g.fillPolygon(xs, ys, 3);
    }

    private static Color corDaCasa(int numero) {
        if (numero == 0)
            return new Color(20, 140, 60);
        return Aposta.VERMELHOS.contains(numero) ? new Color(190, 30, 35) : new Color(25, 25, 25);
    }
}
