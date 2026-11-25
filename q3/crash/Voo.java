import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// estado do aviao, compartilhado entre a torre (que faz ele subir), os bots (que esperam o
// multiplicador passar do alvo) e a interface (o SACAR do humano). A cada quadro a torre avisa
// todo mundo com signalAll e cada bot acordado confere o proprio alvo
public class Voo {

    enum Fase { APOSTAS, VOANDO, CAIU }

    record Saida(double multiplicador, long tempoDeVooMs) {
    }

    static final double CRESCIMENTO_POR_MS = Math.log(2) / 5000;
    static final double MULTIPLICADOR_MAXIMO = 10;

    private final ReentrantLock trava = new ReentrantLock();
    private final Condition mudou = trava.newCondition();
    private final Map<Jogador, Integer> apostas = new LinkedHashMap<>();
    private final Map<Jogador, Saida> saidas = new LinkedHashMap<>();
    private final Random sorteio = new Random();
    private Fase fase = Fase.CAIU;
    private int rodada = 0;
    private long decolagem;
    private long tempoDeVooMs;
    private double pontoDeQueda = 1;
    private double multiplicador = 1;

    static double multiplicadorEm(long tempoDeVooMs) {
        return Math.exp(CRESCIMENTO_POR_MS * tempoDeVooMs);
    }

    // a chance de passar de m e 0,99/m, como nos jogos de verdade (1% cai na decolagem)
    private double sortearQueda() {
        return Math.min(MULTIPLICADOR_MAXIMO, Math.max(1.0, 0.99 / (1 - sorteio.nextDouble())));
    }

    void abrirApostas() {
        trava.lock();
        try {
            rodada++;
            fase = Fase.APOSTAS;
            apostas.clear();
            saidas.clear();
            multiplicador = 1;
            tempoDeVooMs = 0;
            pontoDeQueda = sortearQueda();
            mudou.signalAll();
        } finally {
            trava.unlock();
        }
    }

    boolean apostar(Jogador jogador, int valor) {
        trava.lock();
        try {
            if (fase != Fase.APOSTAS || apostas.containsKey(jogador) || !jogador.debitar(valor))
                return false;
            apostas.put(jogador, valor);
            return true;
        } finally {
            trava.unlock();
        }
    }

    void decolar() {
        trava.lock();
        try {
            fase = Fase.VOANDO;
            decolagem = System.currentTimeMillis();
            mudou.signalAll();
        } finally {
            trava.unlock();
        }
    }

    // um quadro do voo; devolve false quando o aviao cai
    boolean subir() {
        trava.lock();
        try {
            tempoDeVooMs = System.currentTimeMillis() - decolagem;
            multiplicador = multiplicadorEm(tempoDeVooMs);
            if (multiplicador >= pontoDeQueda) {
                multiplicador = pontoDeQueda;
                fase = Fase.CAIU;
            }
            mudou.signalAll();
            return fase == Fase.VOANDO;
        } finally {
            trava.unlock();
        }
    }

    // devolve o multiplicador do saque, ou 0 se nao deu (nao apostou, ja saiu ou o aviao caiu)
    double sacar(Jogador jogador) {
        trava.lock();
        try {
            Integer valor = apostas.get(jogador);
            if (fase != Fase.VOANDO || valor == null || saidas.containsKey(jogador))
                return 0;
            saidas.put(jogador, new Saida(multiplicador, tempoDeVooMs));
            jogador.creditar((int) Math.round(valor * multiplicador));
            return multiplicador;
        } finally {
            trava.unlock();
        }
    }

    int esperarApostas(int rodadaAnterior) throws InterruptedException {
        trava.lock();
        try {
            while (fase != Fase.APOSTAS || rodada == rodadaAnterior)
                mudou.await();
            return rodada;
        } finally {
            trava.unlock();
        }
    }

    // o bot dorme aqui; devolve true se o alvo chegou com o aviao ainda no ar
    boolean esperarPassar(double alvo) throws InterruptedException {
        trava.lock();
        try {
            while (fase != Fase.CAIU && multiplicador < alvo)
                mudou.await();
            return fase == Fase.VOANDO;
        } finally {
            trava.unlock();
        }
    }

    Fase fase() {
        trava.lock();
        try {
            return fase;
        } finally {
            trava.unlock();
        }
    }

    double multiplicador() {
        trava.lock();
        try {
            return multiplicador;
        } finally {
            trava.unlock();
        }
    }

    long tempoDeVooMs() {
        trava.lock();
        try {
            return tempoDeVooMs;
        } finally {
            trava.unlock();
        }
    }

    long decolagem() {
        trava.lock();
        try {
            return decolagem;
        } finally {
            trava.unlock();
        }
    }

    int rodada() {
        trava.lock();
        try {
            return rodada;
        } finally {
            trava.unlock();
        }
    }

    int apostaDe(Jogador jogador) {
        trava.lock();
        try {
            return apostas.getOrDefault(jogador, 0);
        } finally {
            trava.unlock();
        }
    }

    Map<Jogador, Integer> apostas() {
        trava.lock();
        try {
            return new LinkedHashMap<>(apostas);
        } finally {
            trava.unlock();
        }
    }

    Map<Jogador, Saida> saidas() {
        trava.lock();
        try {
            return new LinkedHashMap<>(saidas);
        } finally {
            trava.unlock();
        }
    }
}
