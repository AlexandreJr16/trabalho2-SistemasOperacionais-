import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

// cada cavalo e uma thread: espera a largada no CountDownLatch e depois da passos, cada passo
// demorando o ritmo dele (sleep). A ordem de chegada sai de um contador atomico, entao dois
// cavalos chegando juntos nunca pegam a mesma colocacao
public class Cavalo implements Runnable {

    static final String[] NOMES = {"Relâmpago", "Pé de Pano", "Trovão", "Ventania", "Pangaré", "Foguete"};
    static final double PASSO_MEDIO = 0.015;
    static final double IMPULSO_DO_CHICOTE = 0.02;

    final int numero;
    final String nome;
    final Color cor;
    private final int ritmoMs;
    private final Random sorteio = new Random();
    private final AtomicInteger chicotadas = new AtomicInteger();
    private volatile double posicao = 0;
    private volatile int colocacao = 0;
    private volatile long chegouEm;
    private CountDownLatch largada;
    private AtomicInteger ordemDeChegada;

    Cavalo(int numero, int ritmoMs) {
        this.numero = numero;
        this.nome = NOMES[numero - 1];
        this.cor = Estilo.cor(numero - 1);
        this.ritmoMs = ritmoMs;
    }

    // cada pareo sorteia um ritmo novo pra cada cavalo, entre 35 e 95 ms por passo
    static List<Cavalo> sortearPareo(Random sorteio) {
        List<Cavalo> cavalos = new ArrayList<>();
        for (int numero = 1; numero <= NOMES.length; numero++)
            cavalos.add(new Cavalo(numero, 35 + sorteio.nextInt(61)));
        return cavalos;
    }

    void prepararLargada(CountDownLatch largada, AtomicInteger ordemDeChegada) {
        this.largada = largada;
        this.ordemDeChegada = ordemDeChegada;
    }

    @Override
    public void run() {
        try {
            largada.await();
            while (!chegou())
                darPasso();
            cruzarAChegada(ordemDeChegada);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void darPasso() throws InterruptedException {
        Thread.sleep(ritmoMs / 2 + sorteio.nextInt(ritmoMs));
        double impulso = chicotadas.getAndSet(0) > 0 ? IMPULSO_DO_CHICOTE : 0;
        posicao = Math.min(1, posicao + PASSO_MEDIO * (0.5 + sorteio.nextDouble()) + impulso);
    }

    void cruzarAChegada(AtomicInteger ordem) {
        chegouEm = System.currentTimeMillis();
        colocacao = ordem.incrementAndGet();
    }

    boolean chegou() {
        return posicao >= 1;
    }

    void chicotear() {
        chicotadas.incrementAndGet();
    }

    double posicao() {
        return posicao;
    }

    int colocacao() {
        return colocacao;
    }

    long chegouEm() {
        return chegouEm;
    }
}
