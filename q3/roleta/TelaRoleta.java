import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

// pecas da roleta que as duas versoes usam: a roda, o pano com as fichas, a lista de jogadores,
// a escolha da ficha e o que acontece quando o humano clica no pano
public class TelaRoleta {

    final PainelRoleta roda = new PainelRoleta();
    final TabuleiroRoleta tabuleiro;
    final PainelJogadores painelJogadores;
    final JPanel centro = new JPanel(new BorderLayout(10, 0));
    final JPanel controles;
    private int fichaEscolhida = Aposta.FICHAS[1];

    TelaRoleta(List<Jogador> jogadores, JComponent... extras) {
        tabuleiro = new TabuleiroRoleta(jogadores);
        painelJogadores = new PainelJogadores(jogadores);
        centro.setBackground(Estilo.FELTRO);
        centro.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centro.add(roda, BorderLayout.WEST);
        centro.add(tabuleiro, BorderLayout.CENTER);
        controles = JanelaJogo.barraDeControles(seletorDeFicha(extras));
    }

    int ficha() {
        return fichaEscolhida;
    }

    void mostrarResultado(int numero) {
        tabuleiro.destacar(numero);
    }

    void limparResultado() {
        tabuleiro.destacar(-1);
        roda.limpar();
    }

    // botao esquerdo poe a ficha escolhida na casa, o direito tira a sua ficha de la
    void jogadaDoHumano(Mesa mesa, Jogador humano, Feed feed, String alvo, boolean tirar) {
        if (tirar) {
            Aposta tirada = mesa.tirar(humano, alvo);
            if (tirada != null)
                humano.mudarStatus("tirou " + tirada);
            feed.evento(tirada == null ? "Você não tem ficha aí (ou a mesa já fechou)" : "Você tirou " + tirada, humano.cor);
            return;
        }
        Aposta aposta = new Aposta(alvo, fichaEscolhida);
        if (mesa.apostar(humano, aposta)) {
            humano.mudarStatus("apostou " + aposta);
            feed.evento("Você apostou " + aposta, humano.cor);
            return;
        }
        feed.evento(mesa.apostasAbertas() ? "Saldo insuficiente pra essa ficha" : "Mesa fechada: espere a próxima rodada", Estilo.VERMELHO);
    }

    private JComponent seletorDeFicha(JComponent[] extras) {
        JPanel painel = new JPanel();
        painel.setOpaque(false);
        painel.add(Estilo.rotulo("Sua ficha:", 14));
        ButtonGroup grupo = new ButtonGroup();
        for (int valor : Aposta.FICHAS) {
            JToggleButton botao = new JToggleButton(String.valueOf(valor), valor == fichaEscolhida);
            botao.setFont(Estilo.fonte(14));
            botao.addActionListener(evento -> fichaEscolhida = valor);
            grupo.add(botao);
            painel.add(botao);
        }
        painel.add(Estilo.rotulo("   clique numa casa pra apostar, botão direito tira a ficha   ", 13));
        for (JComponent extra : extras)
            painel.add(extra);
        return painel;
    }
}
