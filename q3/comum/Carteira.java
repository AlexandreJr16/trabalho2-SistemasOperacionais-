// saldo de alguem. Bots, threads de pagamento e varios jogos abertos ao mesmo tempo mexem no
// mesmo saldo, entao todo acesso passa pelo monitor do objeto (synchronized)
public class Carteira {

    private int saldo;

    Carteira(int saldoInicial) {
        saldo = saldoInicial;
    }

    synchronized boolean debitar(int valor) {
        if (valor > saldo)
            return false;
        saldo -= valor;
        return true;
    }

    synchronized void creditar(int valor) {
        saldo += valor;
    }

    synchronized int saldo() {
        return saldo;
    }
}
