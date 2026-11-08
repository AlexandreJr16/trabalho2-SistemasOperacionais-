import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// estado compartilhado da rodada. Checar se as apostas estao abertas e mexer nelas acontece
// sempre sob a mesma trava, entao nenhuma aposta entra depois do "nao vai mais"
public class Mesa {

    private final ReentrantLock trava = new ReentrantLock();
    private final Condition mudouRodada = trava.newCondition();
    private final Map<Jogador, List<Aposta>> apostas = new LinkedHashMap<>();
    private boolean apostasAbertas = false;
    private int rodada = 0;
    private int acoesNaRodada = 0;

    void abrirRodada() {
        trava.lock();
        try {
            rodada++;
            acoesNaRodada = 0;
            apostas.clear();
            apostasAbertas = true;
            mudouRodada.signalAll();
        } finally {
            trava.unlock();
        }
    }

    // devolve uma copia congelada das apostas, que e o que o crupie usa pra apurar
    Map<Jogador, List<Aposta>> fecharApostas() {
        trava.lock();
        try {
            apostasAbertas = false;
            Map<Jogador, List<Aposta>> congeladas = new LinkedHashMap<>();
            for (Map.Entry<Jogador, List<Aposta>> entrada : apostas.entrySet())
                congeladas.put(entrada.getKey(), new ArrayList<>(entrada.getValue()));
            return congeladas;
        } finally {
            trava.unlock();
        }
    }

    // o jogador dorme aqui (sem gastar CPU) ate o crupie abrir uma rodada nova
    int esperarProximaRodada(int rodadaAnterior) throws InterruptedException {
        trava.lock();
        try {
            while (!apostasAbertas || rodada == rodadaAnterior)
                mudouRodada.await();
            return rodada;
        } finally {
            trava.unlock();
        }
    }

    boolean aberta(int rodadaDoJogador) {
        trava.lock();
        try {
            return apostasAbertas && rodada == rodadaDoJogador;
        } finally {
            trava.unlock();
        }
    }

    boolean apostar(Jogador jogador, Aposta aposta) {
        trava.lock();
        try {
            if (!apostasAbertas || !jogador.debitar(aposta.valor()))
                return false;
            apostas.computeIfAbsent(jogador, chave -> new ArrayList<>()).add(aposta);
            acoesNaRodada++;
            return true;
        } finally {
            trava.unlock();
        }
    }

    // tira a ultima aposta do jogador e devolve o dinheiro; null se nao tinha ou ja fechou
    Aposta cancelarUltima(Jogador jogador) {
        trava.lock();
        try {
            List<Aposta> doJogador = apostas.get(jogador);
            if (!apostasAbertas || doJogador == null || doJogador.isEmpty())
                return null;
            Aposta removida = doJogador.remove(doJogador.size() - 1);
            jogador.creditar(removida.valor());
            acoesNaRodada++;
            return removida;
        } finally {
            trava.unlock();
        }
    }

    // a trava e reentrante, entao da pra chamar cancelar e apostar aqui dentro e a troca
    // inteira fica atomica: o crupie nao consegue fechar no meio dela
    Aposta trocar(Jogador jogador, String novoAlvo) {
        trava.lock();
        try {
            Aposta antiga = cancelarUltima(jogador);
            if (antiga == null)
                return null;
            apostar(jogador, new Aposta(novoAlvo, antiga.valor()));
            acoesNaRodada--;
            return antiga;
        } finally {
            trava.unlock();
        }
    }

    int cancelarTodas(Jogador jogador) {
        trava.lock();
        try {
            int canceladas = 0;
            while (cancelarUltima(jogador) != null)
                canceladas++;
            return canceladas;
        } finally {
            trava.unlock();
        }
    }

    List<Aposta> apostasDe(Jogador jogador) {
        trava.lock();
        try {
            return new ArrayList<>(apostas.getOrDefault(jogador, List.of()));
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

    int acoesNaRodada() {
        trava.lock();
        try {
            return acoesNaRodada;
        } finally {
            trava.unlock();
        }
    }

    boolean apostasAbertas() {
        trava.lock();
        try {
            return apostasAbertas;
        } finally {
            trava.unlock();
        }
    }
}
