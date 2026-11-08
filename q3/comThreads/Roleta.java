import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

// JohnMarcos Bet com threads: um bot por thread, crupie, animacao e pagamentos em threads
// proprias, e o humano apostando pela interface ao mesmo tempo que todo mundo
// uso: java -cp bin/q3 Roleta [jogadores segundosDeAposta saldoInicial atrasoMinimoMs atrasoMaximoMs]
public class Roleta {

    static final long CAIXA_INICIAL = 100000;

    record Parametros(int jogadores, int segundosDeAposta, int saldoInicial, int atrasoMinimoMs, int atrasoMaximoMs) {
    }

    public static void main(String[] args) throws Exception {
        Parametros parametros = args.length == 5 ? lerArgumentos(args) : perguntarParametros();
        Mesa mesa = new Mesa();
        Caixa caixa = new Caixa(CAIXA_INICIAL);
        Jogador humano = new Jogador("Voce", parametros.saldoInicial(), mesa, 0, 0);
        List<Jogador> jogadores = new ArrayList<>(List.of(humano));
        for (int i = 1; i <= parametros.jogadores(); i++)
            jogadores.add(new Jogador("Jogador " + i, parametros.saldoInicial(), mesa, parametros.atrasoMinimoMs(), parametros.atrasoMaximoMs()));

        PainelRoleta roleta = new PainelRoleta();
        Crupie crupie = new Crupie(mesa, caixa, jogadores, roleta, parametros.segundosDeAposta());
        SwingUtilities.invokeAndWait(() -> new Janela(mesa, caixa, crupie, humano, jogadores, roleta));

        Janela.log("abrindo o cassino com " + parametros.jogadores() + " bots, janela de " + parametros.segundosDeAposta() + " s");
        for (Jogador jogador : jogadores.subList(1, jogadores.size()))
            new Thread(jogador, jogador.nome).start();
        new Thread(crupie, "Crupie").start();
    }

    private static Parametros lerArgumentos(String[] args) {
        return new Parametros(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                Integer.parseInt(args[3]), Integer.parseInt(args[4]));
    }

    // fechar o dialogo ou cancelar usa os valores padrao
    private static Parametros perguntarParametros() {
        JSpinner jogadores = new JSpinner(new SpinnerNumberModel(6, 1, 40, 1));
        JSpinner segundos = new JSpinner(new SpinnerNumberModel(15, 3, 120, 1));
        JSpinner saldo = new JSpinner(new SpinnerNumberModel(1000, 10, 100000, 100));
        JSpinner atrasoMinimo = new JSpinner(new SpinnerNumberModel(500, 0, 20000, 100));
        JSpinner atrasoMaximo = new JSpinner(new SpinnerNumberModel(3000, 0, 20000, 100));
        JPanel painel = new JPanel(new GridLayout(0, 2, 6, 6));
        painel.add(new JLabel("Bots (threads jogadoras):"));
        painel.add(jogadores);
        painel.add(new JLabel("Janela de apostas (s):"));
        painel.add(segundos);
        painel.add(new JLabel("Saldo inicial:"));
        painel.add(saldo);
        painel.add(new JLabel("Atraso minimo dos bots (ms):"));
        painel.add(atrasoMinimo);
        painel.add(new JLabel("Atraso maximo dos bots (ms):"));
        painel.add(atrasoMaximo);
        JOptionPane.showMessageDialog(null, painel, "JohnMarcos Bet - parametros", JOptionPane.PLAIN_MESSAGE);

        int minimo = (Integer) atrasoMinimo.getValue();
        int maximo = Math.max(minimo, (Integer) atrasoMaximo.getValue());
        return new Parametros((Integer) jogadores.getValue(), (Integer) segundos.getValue(), (Integer) saldo.getValue(), minimo, maximo);
    }
}
