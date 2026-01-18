package sma;

public class Produto {

    private final String id;
    private final String nome;
    private final String tipo;
    private final double val;
    private int quant;

    public Produto(String id, String nome, String tipo, double val, int quant) {
        this.id = id;
        this.nome = nome;
        this.tipo = tipo;
        this.val = val;
        this.quant = quant;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return nome;
    }
    public double getPrice() {
        return val;
    }
    public boolean isAvailable() {
        return quant > 0;
    }

    public synchronized boolean purchase() {
        if (quant > 0) {
            quant--;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return nome + " (R$ " + val + ", qtd: " + quant + ")";
    }
}