import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.Map;
import javax.swing.JComponent;

// a curva do multiplicador com o aviao na ponta e um ponto pra cada jogador que saiu
public class GraficoCrash extends JComponent {

    static final int MARGEM = 60;

    private final Voo voo;
    private volatile String chamada = "";
    private volatile String aviso = "";

    GraficoCrash(Voo voo) {
        this.voo = voo;
        setPreferredSize(new Dimension(900, 420));
    }

    void chamar(String texto) {
        chamada = texto;
        repaint();
    }

    void avisar(String texto) {
        aviso = texto;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        Graphics2D g = Estilo.suave(grafico);
        g.setPaint(new GradientPaint(0, 0, new Color(16, 20, 40), 0, getHeight(), new Color(40, 16, 30)));
        g.fillRect(0, 0, getWidth(), getHeight());
        Voo.Fase fase = voo.fase();
        long tempo = voo.tempoDeVooMs();
        double multiplicador = voo.multiplicador();
        double escalaX = Math.max(10000, tempo * 1.15);
        double escalaY = Math.max(2, multiplicador * 1.2);

        desenharGrade(g, escalaY);
        if (fase != Voo.Fase.APOSTAS)
            desenharCurva(g, fase, tempo, escalaX, escalaY);
        desenharSaidas(g, escalaX, escalaY);
        desenharTextoGrande(g, fase, multiplicador);
        desenharAviso(g);
    }

    private int x(double tempoMs, double escalaX) {
        return MARGEM + (int) (tempoMs / escalaX * (getWidth() - 2 * MARGEM));
    }

    private int y(double multiplicador, double escalaY) {
        return getHeight() - MARGEM - (int) ((multiplicador - 1) / (escalaY - 1) * (getHeight() - 2 * MARGEM));
    }

    private void desenharGrade(Graphics2D g, double escalaY) {
        g.setFont(Estilo.fonte(12));
        double passo = escalaY > 6 ? 1 : escalaY > 3 ? 0.5 : 0.25;
        for (double m = 1; m <= escalaY; m += passo) {
            int linha = y(m, escalaY);
            g.setColor(new Color(255, 255, 255, 30));
            g.drawLine(MARGEM, linha, getWidth() - MARGEM, linha);
            g.setColor(Estilo.TEXTO_FRACO);
            g.drawString(Estilo.vezes(m), 8, linha + 4);
        }
    }

    private void desenharCurva(Graphics2D g, Voo.Fase fase, long tempo, double escalaX, double escalaY) {
        Path2D curva = new Path2D.Double();
        curva.moveTo(x(0, escalaX), y(1, escalaY));
        for (long t = 0; t <= tempo; t += Math.max(20, tempo / 200))
            curva.lineTo(x(t, escalaX), y(Voo.multiplicadorEm(t), escalaY));
        int pontaX = x(tempo, escalaX);
        int pontaY = y(Math.min(Voo.multiplicadorEm(tempo), voo.multiplicador()), escalaY);
        curva.lineTo(pontaX, pontaY);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(fase == Voo.Fase.CAIU ? Estilo.VERMELHO : new Color(255, 150, 40));
        g.draw(curva);
        g.setStroke(new BasicStroke(1));
        double inclinacao = Math.atan2(pontaY - y(Voo.multiplicadorEm(Math.max(0, tempo - 300)), escalaY), pontaX - x(Math.max(0, tempo - 300), escalaX));
        if (fase == Voo.Fase.CAIU)
            desenharExplosao(g, pontaX, pontaY);
        else
            desenharAviao(g, pontaX, pontaY, inclinacao);
    }

    private void desenharAviao(Graphics2D g, int x, int y, double inclinacao) {
        AffineTransform antes = g.getTransform();
        g.translate(x, y);
        g.rotate(inclinacao);
        g.setColor(Color.WHITE);
        g.fillRoundRect(-26, -6, 52, 12, 10, 10);
        g.setColor(Estilo.VERMELHO);
        g.fillPolygon(new int[] {-6, 8, -14}, new int[] {0, 0, -22}, 3);
        g.fillPolygon(new int[] {-6, 8, -14}, new int[] {0, 0, 22}, 3);
        g.fillPolygon(new int[] {-26, -18, -30}, new int[] {0, 0, -12}, 3);
        g.setColor(Estilo.AZUL);
        g.fillOval(12, -4, 8, 8);
        g.setTransform(antes);
    }

    private void desenharExplosao(Graphics2D g, int x, int y) {
        g.setColor(new Color(255, 200, 40, 200));
        g.fillOval(x - 34, y - 34, 68, 68);
        g.setColor(new Color(240, 60, 30, 220));
        g.fillOval(x - 20, y - 20, 40, 40);
    }

    private void desenharSaidas(Graphics2D g, double escalaX, double escalaY) {
        g.setFont(Estilo.fonte(12));
        for (Map.Entry<Jogador, Voo.Saida> saida : voo.saidas().entrySet()) {
            int px = x(saida.getValue().tempoDeVooMs(), escalaX);
            int py = y(saida.getValue().multiplicador(), escalaY);
            g.setColor(saida.getKey().cor);
            g.fillOval(px - 7, py - 7, 14, 14);
            g.drawString(saida.getKey().nome + " " + Estilo.vezes(saida.getValue().multiplicador()), px + 10, py - 8);
        }
    }

    private void desenharTextoGrande(Graphics2D g, Voo.Fase fase, double multiplicador) {
        g.setFont(Estilo.fonte(64));
        if (fase == Voo.Fase.APOSTAS) {
            g.setColor(Estilo.VERDE);
            g.setFont(Estilo.fonte(40));
            Estilo.textoCentrado(g, chamada, getWidth() / 2, getHeight() / 2);
            return;
        }
        g.setColor(fase == Voo.Fase.CAIU ? Estilo.VERMELHO : Color.WHITE);
        String texto = fase == Voo.Fase.CAIU ? "CAIU em " + Estilo.vezes(multiplicador) : Estilo.vezes(multiplicador);
        Estilo.textoCentrado(g, texto, getWidth() / 2, 110);
    }

    // texto longo explicando o que aconteceu; quebra em linhas pelo \n
    private void desenharAviso(Graphics2D g) {
        String texto = aviso;
        if (texto.isEmpty())
            return;
        String[] linhas = texto.split("\n");
        int altura = 28 * linhas.length + 16;
        int topo = getHeight() - altura - 10;
        g.setColor(new Color(60, 50, 10, 235));
        g.fillRoundRect(20, topo, getWidth() - 40, altura, 16, 16);
        g.setColor(Estilo.AMARELO);
        g.drawRoundRect(20, topo, getWidth() - 40, altura, 16, 16);
        g.setFont(Estilo.fonte(18));
        for (int i = 0; i < linhas.length; i++)
            g.drawString(linhas[i], 36, topo + 32 + 28 * i);
    }
}
