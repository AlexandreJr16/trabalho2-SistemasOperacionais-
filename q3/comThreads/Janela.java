import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// interface. Roda na thread de eventos do Swing (EDT): os cliques do humano viram apostas
// na mesma Mesa que os bots usam, e um timer redesenha o estado de todo mundo
public class Janela {

    static final Color FELTRO = new Color(10, 80, 45);
    static final Color DOURADO = new Color(212, 175, 55);
    static final int ATUALIZACAO_MS = 150;
    static final int TAMANHO_MAXIMO_LOG = 200000;
    static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static final JTextPane areaLog = new JTextPane();

    private final Mesa mesa;
    private final Caixa caixa;
    private final Crupie crupie;
    private final Jogador humano;
    private final List<Jogador> jogadores;
    private final List<JLabel> cartoes = new ArrayList<>();
    private final JLabel rotuloFase = rotulo(22);
    private final JLabel rotuloInfo = rotulo(14);
    private final JSpinner valorFicha = new JSpinner(new SpinnerNumberModel(50, 10, 1000, 10));
    private final JSpinner numeroEscolhido = new JSpinner(new SpinnerNumberModel(17, 0, 36, 1));

    Janela(Mesa mesa, Caixa caixa, Crupie crupie, Jogador humano, List<Jogador> jogadores, PainelRoleta roleta) {
        this.mesa = mesa;
        this.caixa = caixa;
        this.crupie = crupie;
        this.humano = humano;
        this.jogadores = jogadores;

        JFrame quadro = new JFrame("JohnMarcos Bet - roleta com threads");
        quadro.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        quadro.setLayout(new BorderLayout());
        quadro.add(montarCabecalho(), BorderLayout.NORTH);
        quadro.add(montarMesa(roleta), BorderLayout.CENTER);
        quadro.add(montarRodape(), BorderLayout.SOUTH);
        quadro.setSize(1250, 900);
        quadro.setLocationRelativeTo(null);
        quadro.setVisible(true);

        new Timer(ATUALIZACAO_MS, evento -> atualizar()).start();
    }

    // cada thread escreve com o nome dela e uma cor propria, pra dar pra ver quem fez o que
    static void log(String texto) {
        String nomeThread = Thread.currentThread().getName();
        String linha = LocalTime.now().format(HORA) + "  [" + nomeThread + "] " + texto + "\n";
        System.out.print(linha);
        SimpleAttributeSet estilo = new SimpleAttributeSet();
        StyleConstants.setForeground(estilo, corDaThread(nomeThread));
        SwingUtilities.invokeLater(() -> escreverNoLog(linha, estilo));
    }

    private static void escreverNoLog(String linha, SimpleAttributeSet estilo) {
        StyledDocument documento = areaLog.getStyledDocument();
        try {
            documento.insertString(documento.getLength(), linha, estilo);
            if (documento.getLength() > TAMANHO_MAXIMO_LOG)
                documento.remove(0, documento.getLength() - TAMANHO_MAXIMO_LOG / 2);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        areaLog.setCaretPosition(documento.getLength());
    }

    private static Color corDaThread(String nomeThread) {
        if (nomeThread.startsWith("Crupie"))
            return DOURADO;
        if (nomeThread.startsWith("AWT"))
            return new Color(120, 200, 255);
        float matiz = (nomeThread.hashCode() & 0xff) / 255f;
        return Color.getHSBColor(matiz, 0.55f, 1f);
    }

    private JPanel montarCabecalho() {
        JPanel cabecalho = new JPanel(new GridLayout(2, 1));
        cabecalho.setBackground(FELTRO.darker());
        cabecalho.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        cabecalho.add(rotuloFase);
        cabecalho.add(rotuloInfo);
        return cabecalho;
    }

    private JPanel montarMesa(PainelRoleta roleta) {
        JPanel grade = new JPanel(new GridLayout(0, 2, 8, 8));
        grade.setBackground(FELTRO);
        for (Jogador jogador : jogadores) {
            JLabel cartao = rotulo(13);
            cartao.setVerticalAlignment(JLabel.TOP);
            cartao.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(jogador == humano ? Color.CYAN : DOURADO, 2),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));
            cartoes.add(cartao);
            grade.add(cartao);
        }
        JScrollPane rolagem = new JScrollPane(grade);
        rolagem.getViewport().setBackground(FELTRO);

        JPanel mesaInteira = new JPanel(new BorderLayout(10, 0));
        mesaInteira.setBackground(FELTRO);
        mesaInteira.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mesaInteira.add(roleta, BorderLayout.WEST);
        mesaInteira.add(rolagem, BorderLayout.CENTER);
        return mesaInteira;
    }

    private JPanel montarRodape() {
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.setBackground(FELTRO.darker());
        controles.add(rotuloBranco("Sua ficha:"));
        controles.add(valorFicha);
        for (String alvo : Aposta.ALVOS_FIXOS)
            controles.add(botao(alvo, () -> apostarHumano(alvo)));
        controles.add(rotuloBranco("  Numero:"));
        controles.add(numeroEscolhido);
        controles.add(botao("apostar no numero", () -> apostarHumano(String.valueOf(numeroEscolhido.getValue()))));
        controles.add(botao("cancelar minhas apostas", this::cancelarHumano));
        controles.add(botao("pausar / continuar crupie", this::pausar));

        areaLog.setEditable(false);
        areaLog.setBackground(new Color(18, 18, 18));
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane rolagemLog = new JScrollPane(areaLog);
        rolagemLog.setPreferredSize(new Dimension(100, 260));

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(controles, BorderLayout.NORTH);
        rodape.add(rolagemLog, BorderLayout.CENTER);
        return rodape;
    }

    private void apostarHumano(String alvo) {
        Aposta aposta = new Aposta(alvo, (Integer) valorFicha.getValue());
        if (mesa.apostar(humano, aposta)) {
            humano.mudarStatus("apostou " + aposta);
            log("voce apostou " + aposta + " (saldo " + humano.saldo() + ")");
            return;
        }
        String motivo = mesa.apostasAbertas() ? "sem saldo pra isso" : "a mesa esta fechada";
        log("aposta recusada: " + motivo);
    }

    private void cancelarHumano() {
        int canceladas = mesa.cancelarTodas(humano);
        humano.mudarStatus(canceladas > 0 ? "cancelou tudo" : humano.status());
        log(canceladas > 0 ? "voce cancelou " + canceladas + " apostas" : "nada pra cancelar (ou a mesa ja fechou)");
    }

    private void pausar() {
        boolean pausado = crupie.alternarPausa();
        log(pausado ? "pediu pausa: o crupie para depois desta rodada" : "crupie liberado");
    }

    // so le o estado: quem muda as coisas sao as threads dos jogadores e do crupie
    private void atualizar() {
        String fase = crupie.fase();
        boolean aberta = fase.equals("APOSTAS ABERTAS");
        rotuloFase.setText(aberta ? fase + " - fecha em " + crupie.segundosRestantes() + " s" : fase);
        rotuloFase.setForeground(aberta ? Color.GREEN : fase.equals("NAO VAI MAIS") ? Color.RED : DOURADO);
        rotuloInfo.setText("Rodada " + mesa.rodada() + "   |   caixa da casa: " + caixa.saldo()
                + "   |   acoes nesta janela: " + mesa.acoesNaRodada() + "   |   threads vivas: " + Thread.activeCount());
        for (int i = 0; i < jogadores.size(); i++)
            cartoes.get(i).setText(textoDoCartao(jogadores.get(i)));
    }

    private String textoDoCartao(Jogador jogador) {
        List<Aposta> apostas = mesa.apostasDe(jogador);
        String listaApostas = apostas.isEmpty() ? "-" : apostas.toString().replaceAll("[\\[\\]]", "");
        return "<html><b>" + jogador.nome + "</b> &nbsp; saldo <b>" + jogador.saldo() + "</b><br>"
                + "<i>" + jogador.status() + "</i><br>apostas: " + listaApostas + "</html>";
    }

    private static JButton botao(String texto, Runnable acao) {
        JButton botao = new JButton(texto);
        botao.addActionListener(evento -> acao.run());
        return botao;
    }

    private static JLabel rotulo(int tamanhoFonte) {
        JLabel rotulo = new JLabel();
        rotulo.setForeground(Color.WHITE);
        rotulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, tamanhoFonte));
        return rotulo;
    }

    private static JLabel rotuloBranco(String texto) {
        JLabel rotulo = rotulo(13);
        rotulo.setText(texto);
        return rotulo;
    }
}
