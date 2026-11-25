// os jogos do lobby, com o que cada um mede no placar e o que cada versao mostra
enum Jogo {

    ROLETA("Roleta", "tempo da rodada",
            "Todo mundo aposta ao mesmo tempo e pode mudar a ficha até o crupiê fechar a mesa.",
            "Um jogador por vez. Na vez dos bots a janela trava e você não mexe em nada."),
    CRASH("Crash (aviãozinho)", "atraso pra ler o seu SACAR",
            "Aposte e clique em SACAR antes de o avião cair. O clique é atendido na hora.",
            "O laço do voo prende a única thread: seu SACAR só é lido depois da queda. Só o saque automático funciona."),
    CORRIDA("Corrida de cavalos", "tempo da corrida",
            "Cada cavalo é uma thread com o próprio ritmo. Dê chicotadas no seu durante a corrida.",
            "Um laço só move um cavalo de cada vez: a corrida demora a soma de todos.");

    final String titulo;
    final String medida;
    final String comThreads;
    final String semThreads;

    Jogo(String titulo, String medida, String comThreads, String semThreads) {
        this.titulo = titulo;
        this.medida = medida;
        this.comThreads = comThreads;
        this.semThreads = semThreads;
    }
}
