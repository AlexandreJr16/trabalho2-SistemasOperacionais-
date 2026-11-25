import java.util.Random;
import java.util.Set;

// regras da roleta europeia (0 a 36), usadas pelas duas versoes
// alvo e "vermelho", "preto", "par", "impar", "duzia1", "duzia2", "duzia3" ou um numero ("17")
public record Aposta(String alvo, int valor) {

    static final Set<Integer> VERMELHOS = Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    static final String[] ALVOS_FIXOS = {"vermelho", "preto", "par", "impar", "duzia1", "duzia2", "duzia3"};

    // o zero so paga quem apostou nele, nao conta como par nem como cor
    boolean ganhou(int numero) {
        if (numero == 0)
            return alvo.equals("0");
        return switch (alvo) {
            case "vermelho" -> VERMELHOS.contains(numero);
            case "preto" -> !VERMELHOS.contains(numero);
            case "par" -> numero % 2 == 0;
            case "impar" -> numero % 2 == 1;
            case "duzia1" -> numero <= 12;
            case "duzia2" -> numero > 12 && numero <= 24;
            case "duzia3" -> numero > 24;
            default -> Integer.parseInt(alvo) == numero;
        };
    }

    // ja inclui a aposta de volta: cor e par/impar pagam 1 pra 1, duzia 2 pra 1, numero 35 pra 1
    int premio() {
        if (alvo.startsWith("duzia"))
            return valor * 3;
        if (Character.isDigit(alvo.charAt(0)))
            return valor * 36;
        return valor * 2;
    }

    static boolean alvoValido(String alvo) {
        for (String fixo : ALVOS_FIXOS)
            if (fixo.equals(alvo))
                return true;
        return alvo.matches("\\d{1,2}") && Integer.parseInt(alvo) <= 36;
    }

    // um em cada quatro bots vai num numero seco, o resto em aposta de fora
    static Aposta aleatoria(Random sorteio, int valor) {
        if (sorteio.nextInt(4) == 0)
            return new Aposta(String.valueOf(sorteio.nextInt(37)), valor);
        return new Aposta(ALVOS_FIXOS[sorteio.nextInt(ALVOS_FIXOS.length)], valor);
    }

    static String corDe(int numero) {
        if (numero == 0)
            return "verde";
        return VERMELHOS.contains(numero) ? "vermelho" : "preto";
    }

    @Override
    public String toString() {
        return valor + " no " + alvo;
    }
}
