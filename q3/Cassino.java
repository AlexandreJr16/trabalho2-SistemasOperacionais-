import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

// lobby do JohnMarcos Bet: parametros, o modo (com ou sem threads), os jogos e o placar que compara
// os dois modos. A carteira do humano e uma so pra todos os jogos
// uso: java -cp bin/q3 Cassino
public class Cassino {

    static final int SALDO_INICIAL = 1000;

    record Parametros(int bots, int segundosDeAposta, int atrasoMinimoMs, int atrasoMaximoMs) {

        int atrasoDoBot(Random sorteio) {
            return atrasoMinimoMs + sorteio.nextInt(Math.max(1, atrasoMaximoMs - atrasoMinimoMs + 1));
        }
    }

    private final JFrame janela = new JFrame("JohnMarcos Bet");
    private final Jogador humano = new Jogador("Você", Estilo.COR_HUMANO, new Carteira(SALDO_INICIAL));
    private final JSpinner bots = new JSpinner(new SpinnerNumberModel(6, 1, Jogador.MAXIMO_DE_BOTS, 1));
    private final JSpinner segundosDeAposta = new JSpinner(new SpinnerNumberModel(15, 5, 60, 1));
    private final JSpinner atrasoMinimo = new JSpinner(new SpinnerNumberModel(1500, 0, 10000, 100));
    private final JSpinner atrasoMaximo = new JSpinner(new SpinnerNumberModel(4000, 0, 10000, 100));
    private final JButton comThreads = botaoDeModo("COM THREADS", true);
    private final JButton semThreads = botaoDeModo("SEM THREADS", false);
    private boolean modoComThreads = true;
    private final JLabel saldo = Estilo.rotulo("", 22);
    private final List<JButton> cartoes = new ArrayList<>();
    private final List<JLabel> celulasDoPlacar = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Cassino::new);
    }

    Cassino() {
        JPanel conteudo = new JPanel(new BorderLayout(12, 12));
        conteudo.setBackground(Estilo.FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        conteudo.add(cabecalho(), BorderLayout.NORTH);
        conteudo.add(jogos(), BorderLayout.CENTER);
        conteudo.add(parametros(), BorderLayout.WEST);
        conteudo.add(placar(), BorderLayout.SOUTH);
        janela.setContentPane(conteudo);
        janela.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        janela.setSize(1250, 800);
        janela.setLocationRelativeTo(null);
        atualizar();
        janela.setVisible(true);
        new Timer(500, evento -> atualizar()).start();
    }

    private JPanel cabecalho() {
        JLabel titulo = Estilo.rotulo("JohnMarcos Bet", 40);
        titulo.setForeground(Estilo.DOURADO);
        JLabel subtitulo = Estilo.rotulo("Escolha o modo e depois o jogo. Com threads dá pra abrir vários jogos ao mesmo tempo; "
                + "sem threads o jogo trava o lobby até fechar.", 14);
        subtitulo.setForeground(Estilo.TEXTO_FRACO);
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);
        cabecalho.add(textos, BorderLayout.CENTER);
        cabecalho.add(saldo, BorderLayout.EAST);
        return cabecalho;
    }

    private JPanel parametros() {
        JPanel painel = new JPanel(new GridLayout(0, 1, 4, 4));
        painel.setBackground(Estilo.PAINEL);
        painel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        painel.setPreferredSize(new Dimension(250, 400));
        painel.add(Estilo.rotulo("Parâmetros", 18));
        adicionarCampo(painel, "Bots (uma thread cada):", bots);
        adicionarCampo(painel, "Janela de apostas da roleta (s):", segundosDeAposta);
        adicionarCampo(painel, "Bot pensa no mínimo (ms):", atrasoMinimo);
        adicionarCampo(painel, "Bot pensa no máximo (ms):", atrasoMaximo);
        return painel;
    }

    private static void adicionarCampo(JPanel painel, String texto, JSpinner campo) {
        painel.add(Estilo.rotulo(texto, 13));
        campo.setFont(Estilo.fonte(14));
        painel.add(campo);
    }

    private JPanel jogos() {
        JPanel modos = new JPanel(new GridLayout(1, 2, 12, 0));
        modos.setOpaque(false);
        modos.add(comThreads);
        modos.add(semThreads);

        JPanel grade = new JPanel(new GridLayout(1, Jogo.values().length, 12, 12));
        grade.setOpaque(false);
        for (Jogo jogo : Jogo.values()) {
            JButton cartao = new JButton();
            cartao.setBackground(Estilo.FELTRO);
            cartao.setForeground(Estilo.TEXTO);
            cartao.setFocusPainted(false);
            cartao.addActionListener(evento -> abrir(jogo));
            cartoes.add(cartao);
            grade.add(cartao);
        }
        JPanel jogos = new JPanel(new BorderLayout(12, 12));
        jogos.setOpaque(false);
        jogos.add(modos, BorderLayout.NORTH);
        jogos.add(grade, BorderLayout.CENTER);
        return jogos;
    }

    private JButton botaoDeModo(String texto, boolean ehComThreads) {
        JButton botao = new JButton(texto);
        botao.setFont(Estilo.fonte(22));
        botao.setFocusPainted(false);
        botao.setPreferredSize(new Dimension(200, 60));
        botao.addActionListener(evento -> {
            modoComThreads = ehComThreads;
            atualizar();
        });
        return botao;
    }

    // o modo escolhido fica colorido e o outro apagado
    private void pintarModos() {
        comThreads.setBackground(modoComThreads ? Estilo.VERDE : Estilo.PAINEL);
        comThreads.setForeground(modoComThreads ? Color.BLACK : Estilo.TEXTO_FRACO);
        semThreads.setBackground(modoComThreads ? Estilo.PAINEL : Estilo.VERMELHO);
        semThreads.setForeground(modoComThreads ? Estilo.TEXTO_FRACO : Color.BLACK);
        for (JButton cartao : cartoes)
            cartao.setBackground(modoComThreads ? Estilo.FELTRO : new Color(90, 30, 30));
    }

    private JPanel placar() {
        String[] cabecalho = {"Jogo", "O que foi medido", "Sem threads (Ts)", "Com threads (Tp)", "Ts / Tp"};
        JPanel tabela = new JPanel(new GridLayout(Jogo.values().length + 1, cabecalho.length, 8, 4));
        tabela.setBackground(Estilo.PAINEL);
        tabela.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Estilo.DOURADO), "Placar de desempenho (última medida de cada modo)",
                0, 0, Estilo.fonte(14), Estilo.DOURADO));
        for (String titulo : cabecalho) {
            JLabel celula = Estilo.rotulo(titulo, 14);
            celula.setForeground(Estilo.DOURADO);
            tabela.add(celula);
        }
        for (int i = 0; i < Jogo.values().length * cabecalho.length; i++) {
            JLabel celula = Estilo.rotulo("-", 14);
            celulasDoPlacar.add(celula);
            tabela.add(celula);
        }
        return tabela;
    }

    private void atualizar() {
        saldo.setText("Sua carteira: " + Estilo.dinheiro(humano.saldo()));
        pintarModos();
        for (Jogo jogo : Jogo.values()) {
            String descricao = modoComThreads ? jogo.comThreads : jogo.semThreads;
            cartoes.get(jogo.ordinal()).setText("<html><center><span style='font-size:22px'>" + jogo.titulo + "</span><br><br>"
                    + "<span style='font-size:13px'>" + descricao + "</span></center></html>");
            String[] linha = Placar.linha(jogo);
            for (int coluna = 0; coluna < linha.length; coluna++)
                celulasDoPlacar.get(jogo.ordinal() * linha.length + coluna).setText(linha[coluna]);
        }
    }

    private Parametros lerParametros() {
        int minimo = (Integer) atrasoMinimo.getValue();
        int maximo = Math.max(minimo, (Integer) atrasoMaximo.getValue());
        return new Parametros((Integer) bots.getValue(), (Integer) segundosDeAposta.getValue(), minimo, maximo);
    }

    // sem threads a janela do jogo e modal: este metodo so volta quando ela fecha
    private void abrir(Jogo jogo) {
        Parametros parametros = lerParametros();
        boolean com = modoComThreads;
        switch (jogo) {
            case ROLETA -> {
                if (com)
                    new RoletaComThreads(janela, humano, parametros);
                else
                    new RoletaSemThreads(janela, humano, parametros);
            }
            case CRASH -> {
                if (com)
                    new CrashComThreads(janela, humano, parametros);
                else
                    new CrashSemThreads(janela, humano, parametros);
            }
            case CORRIDA -> {
                if (com)
                    new CorridaComThreads(janela, humano, parametros);
                else
                    new CorridaSemThreads(janela, humano, parametros);
            }
        }
        atualizar();
    }
}
