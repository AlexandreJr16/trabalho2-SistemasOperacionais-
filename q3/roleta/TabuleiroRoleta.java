import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

// o pano de apostas com as fichas de cada jogador na cor dele. A ficha nova pisca, entao da pra
// ver o bot apostar, pular de casa ou tirar a ficha enquanto acontece
public class TabuleiroRoleta extends JComponent {

    static final int PISCADA_MS = 1500;
    static final int ALTURA_DO_NOME = 24;
    static final String[] ALVOS_DE_BAIXO = {"par", "vermelho", "preto", "impar"};

    interface Clique {
        void clicou(String alvo, boolean tirar, long quando);
    }

    private final List<Jogador> jogadores;
    private volatile Map<Jogador, List<Aposta>> fichas = Map.of();
    private volatile int sorteado = -1;
    private volatile String casaSobOMouse;
    private Clique aoClicar = (alvo, tirar, quando) -> {
    };

    TabuleiroRoleta(List<Jogador> jogadores) {
        this.jogadores = jogadores;
        setPreferredSize(new Dimension(760, 330));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent evento) {
                String alvo = alvoEm(evento.getPoint());
                if (alvo != null)
                    aoClicar.clicou(alvo, SwingUtilities.isRightMouseButton(evento), evento.getWhen());
            }

            @Override
            public void mouseMoved(MouseEvent evento) {
                casaSobOMouse = alvoEm(evento.getPoint());
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    void aoClicar(Clique ouvinte) {
        aoClicar = ouvinte;
    }

    void mostrar(Map<Jogador, List<Aposta>> novasFichas) {
        fichas = novasFichas;
        repaint();
    }

    void destacar(int numero) {
        sorteado = numero;
        repaint();
    }

    private String alvoEm(Point ponto) {
        for (Map.Entry<String, Rectangle> casa : casas().entrySet())
            if (casa.getValue().contains(ponto))
                return casa.getKey();
        return null;
    }

    // zero na coluna da esquerda, 3 linhas x 12 colunas de numeros, depois duzias e apostas de fora
    private Map<String, Rectangle> casas() {
        int largura = (getWidth() - 20) / 13;
        int altura = (getHeight() - 20) / 5;
        Map<String, Rectangle> casas = new LinkedHashMap<>();
        casas.put("0", new Rectangle(10, 10, largura, altura * 3));
        for (int coluna = 0; coluna < 12; coluna++)
            for (int linha = 0; linha < 3; linha++) {
                int numero = coluna * 3 + 3 - linha;
                casas.put(String.valueOf(numero), new Rectangle(10 + largura * (coluna + 1), 10 + altura * linha, largura, altura));
            }
        for (int duzia = 0; duzia < 3; duzia++)
            casas.put("duzia" + (duzia + 1), new Rectangle(10 + largura * (1 + duzia * 4), 10 + altura * 3, largura * 4, altura));
        for (int i = 0; i < ALVOS_DE_BAIXO.length; i++)
            casas.put(ALVOS_DE_BAIXO[i], new Rectangle(10 + largura + i * largura * 3, 10 + altura * 4, largura * 3, altura));
        return casas;
    }

    @Override
    protected void paintComponent(Graphics grafico) {
        Graphics2D g = Estilo.suave(grafico);
        g.setColor(Estilo.FELTRO);
        g.fillRect(0, 0, getWidth(), getHeight());
        Map<String, Rectangle> casas = casas();
        for (Map.Entry<String, Rectangle> casa : casas.entrySet())
            desenharCasa(g, casa.getKey(), casa.getValue());
        Map<Jogador, List<Aposta>> agora = fichas;
        for (Map.Entry<Jogador, List<Aposta>> entrada : agora.entrySet())
            desenharFichas(g, entrada.getKey(), entrada.getValue(), casas);
    }

    private void desenharCasa(Graphics2D g, String alvo, Rectangle area) {
        g.setColor(corDoFundo(alvo));
        g.fillRect(area.x, area.y, area.width, area.height);
        boolean ganhadora = sorteado >= 0 && new Aposta(alvo, 0).ganhou(sorteado);
        g.setStroke(new BasicStroke(ganhadora ? 4 : 1));
        g.setColor(ganhadora ? Estilo.DOURADO : alvo.equals(casaSobOMouse) ? Color.WHITE : new Color(230, 230, 230, 120));
        g.drawRect(area.x, area.y, area.width, area.height);
        g.setStroke(new BasicStroke(1));
        g.setColor(Color.WHITE);
        g.setFont(Estilo.fonte(Character.isDigit(alvo.charAt(0)) ? 16 : 14));
        Estilo.textoCentrado(g, Aposta.nomeDoAlvo(alvo).toUpperCase(), area.x + area.width / 2, area.y + 19);
    }

    private static Color corDoFundo(String alvo) {
        if (alvo.equals("0"))
            return new Color(20, 140, 60);
        if (alvo.equals("vermelho"))
            return new Color(170, 25, 30);
        if (alvo.equals("preto"))
            return new Color(25, 25, 25);
        if (!Character.isDigit(alvo.charAt(0)))
            return Estilo.FELTRO.darker();
        return Aposta.VERMELHOS.contains(Integer.parseInt(alvo)) ? new Color(170, 25, 30) : new Color(25, 25, 25);
    }

    // cada jogador tem um lugar fixo dentro da casa (abaixo do nome dela), entao fichas de gente
    // diferente nao se cobrem; mais de uma ficha do mesmo jogador na casa fica empilhada
    private void desenharFichas(Graphics2D g, Jogador jogador, List<Aposta> apostas, Map<String, Rectangle> casas) {
        int indice = jogadores.indexOf(jogador);
        int diametro = Math.max(16, (getWidth() - 20) / 13 / 2 - 2);
        long agora = System.currentTimeMillis();
        int empilhadas = 0;
        for (Aposta aposta : apostas) {
            Rectangle area = casas.get(aposta.alvo());
            int colunas = Math.max(1, (area.width - 4) / diametro);
            int linhas = Math.max(1, (area.height - ALTURA_DO_NOME) / diametro);
            int lugar = indice % (colunas * linhas);
            int x = area.x + 2 + lugar % colunas * diametro;
            int y = area.y + ALTURA_DO_NOME + lugar / colunas * diametro - empilhadas % 3 * 3;
            desenharFicha(g, jogador.cor, aposta.valor(), x, y, diametro, agora - aposta.criadaEm());
            empilhadas++;
        }
    }

    private void desenharFicha(Graphics2D g, Color cor, int valor, int x, int y, int diametro, long idade) {
        if (idade < PISCADA_MS) {
            int anel = (int) (diametro * (0.3 + idade / (double) PISCADA_MS));
            g.setColor(new Color(255, 255, 255, (int) (220 * (1 - idade / (double) PISCADA_MS))));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x - anel / 2, y - anel / 2, diametro + anel, diametro + anel);
            g.setStroke(new BasicStroke(1));
        }
        g.setColor(cor);
        g.fillOval(x, y, diametro, diametro);
        g.setColor(Color.WHITE);
        g.drawOval(x, y, diametro, diametro);
        g.setColor(Color.BLACK);
        g.setFont(Estilo.fonte(Math.max(9, diametro / 2 - 1)));
        Estilo.textoCentrado(g, String.valueOf(valor), x + diametro / 2, y + diametro / 2 + diametro / 6);
    }
}
