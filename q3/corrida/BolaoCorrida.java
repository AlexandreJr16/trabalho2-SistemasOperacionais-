import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

// apostas de um pareo: cada jogador escolhe um cavalo; quem acertar o primeiro recebe 5 vezes
public class BolaoCorrida {

    static final int PREMIO = 5;

    record Palpite(Cavalo cavalo, int valor) {
    }

    private final Map<Jogador, Palpite> palpites = new LinkedHashMap<>();

    boolean apostar(Jogador jogador, Cavalo cavalo, int valor) {
        if (!jogador.debitar(valor))
            return false;
        palpites.put(jogador, new Palpite(cavalo, valor));
        jogador.mudarStatus("apostou " + valor + " no " + cavalo.nome);
        return true;
    }

    void botsApostam(List<Jogador> bots, List<Cavalo> cavalos, Random sorteio) {
        for (Jogador bot : bots)
            apostar(bot, cavalos.get(sorteio.nextInt(cavalos.size())), BotCrash.sortearFicha(sorteio));
    }

    void pagar(Cavalo vencedor, Feed feed) {
        for (Map.Entry<Jogador, Palpite> entrada : palpites.entrySet()) {
            Jogador jogador = entrada.getKey();
            Palpite palpite = entrada.getValue();
            boolean acertou = palpite.cavalo() == vencedor;
            if (acertou)
                jogador.creditar(palpite.valor() * PREMIO);
            jogador.mudarStatus(acertou ? "acertou! +" + palpite.valor() * PREMIO : "perdeu " + palpite.valor() + " no " + palpite.cavalo().nome);
            if (jogador.ehVoce())
                feed.evento(acertou ? "Seu cavalo ganhou! +" + Estilo.dinheiro(palpite.valor() * PREMIO) : "Seu cavalo não ganhou", acertou ? Estilo.VERDE : Estilo.VERMELHO);
        }
        palpites.clear();
    }
}
