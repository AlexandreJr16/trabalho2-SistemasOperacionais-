// dinheiro da casa: varias threads de pagamento mexem aqui ao mesmo tempo na apuracao,
// entao todo acesso passa pelo monitor do objeto (synchronized)
public class Caixa {

    private long saldo;

    Caixa(long saldoInicial) {
        saldo = saldoInicial;
    }

    synchronized void receber(int valor) {
        saldo += valor;
    }

    synchronized void pagar(int valor) {
        saldo -= valor;
    }

    synchronized long saldo() {
        return saldo;
    }
}
