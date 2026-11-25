import java.util.EnumMap;
import java.util.Map;

// ultima medida de cada jogo nos dois modos, pra o lobby mostrar Ts, Tp e Ts/Tp lado a lado
public class Placar {

    private static final Map<Jogo, double[]> medidas = new EnumMap<>(Jogo.class);

    static synchronized void registrar(Jogo jogo, boolean comThreads, double ms) {
        medidas.computeIfAbsent(jogo, chave -> new double[2])[comThreads ? 1 : 0] = Math.max(ms, 1);
    }

    static synchronized double medida(Jogo jogo, boolean comThreads) {
        return medidas.getOrDefault(jogo, new double[2])[comThreads ? 1 : 0];
    }

    // {jogo, medida, sem threads, com threads, Ts/Tp}; "-" enquanto falta jogar algum modo
    static synchronized String[] linha(Jogo jogo) {
        double sem = medida(jogo, false);
        double com = medida(jogo, true);
        String razao = sem > 0 && com > 0 ? String.format(Estilo.BRASIL, "%.2f", sem / com) : "-";
        return new String[] {jogo.titulo, jogo.medida, sem > 0 ? Estilo.tempo(sem) : "-", com > 0 ? Estilo.tempo(com) : "-", razao};
    }
}
