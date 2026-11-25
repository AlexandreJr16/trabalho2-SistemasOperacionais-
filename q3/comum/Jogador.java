import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// quem esta sentado na mesa: o humano ou um bot. Os bots de cada jogo sao Runnables a parte
// (BotRoleta, BotCrash) que usam esta classe so pra saldo, cor e o status que aparece na tela
public class Jogador {

    static final int SALDO_DO_BOT = 1000;
    static final int MAXIMO_DE_BOTS = 10;
    static final String DONO_DO_CASSINO = "João Marcos";
    static final List<String> PROFESSORES = List.of("Alberto Castro", "Alexandre Passito", "Altigran Silva",
            "Ana Carolina Oran", "André Carvalho", "Aristóbulo Angelim", "Bruno Gadelha", "César Melo", "David Fernandes",
            "Edjair Mota", "Edjard Mota", "Eulanda Santos", "Edleno Moura", "Edson Silva-Jr", "Eduardo Nakamura",
            "Eduardo Souto", "Eduardo Feitosa", "Elaine Harada", "Fabíola Nakamura", "Horácio Fernandes",
            "José Francisco", "José Pio", "José Reginaldo", "Juan Colonna", "Ketlen Lucena", "Leandro Galvão",
            "Marco Cristo", "Mário Salvatierra", "Moisés Carvalho", "Rafael Giusti", "Raimundo Barreto",
            "Rosiane Freitas", "Ruiter Caldas", "Tanara Lauschner", "Tayana Conte", "Thais Castro");

    final String nome;
    final Color cor;
    private final Carteira carteira;
    private volatile String status = "chegando na mesa";

    Jogador(String nome, Color cor, Carteira carteira) {
        this.nome = nome;
        this.cor = cor;
        this.carteira = carteira;
    }

    // o humano primeiro e depois os bots, que sao professores do IComp sorteados a cada mesa;
    // o dono do cassino (o professor da disciplina) sempre senta
    static List<Jogador> mesaCom(Jogador humano, int quantidadeDeBots) {
        List<String> sorteados = new ArrayList<>(PROFESSORES);
        Collections.shuffle(sorteados);
        sorteados.add(0, DONO_DO_CASSINO);
        List<Jogador> jogadores = new ArrayList<>(List.of(humano));
        for (int i = 0; i < quantidadeDeBots; i++)
            jogadores.add(new Jogador(sorteados.get(i), Estilo.cor(i), new Carteira(SALDO_DO_BOT)));
        return jogadores;
    }

    static List<Jogador> soOsBots(List<Jogador> jogadores) {
        return jogadores.subList(1, jogadores.size());
    }

    boolean ehVoce() {
        return cor == Estilo.COR_HUMANO;
    }

    boolean debitar(int valor) {
        return carteira.debitar(valor);
    }

    void creditar(int valor) {
        carteira.creditar(valor);
    }

    int saldo() {
        return carteira.saldo();
    }

    String status() {
        return status;
    }

    void mudarStatus(String novoStatus) {
        status = novoStatus;
    }
}
