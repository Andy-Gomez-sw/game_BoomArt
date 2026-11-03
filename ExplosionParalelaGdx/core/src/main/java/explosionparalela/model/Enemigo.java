package explosionparalela.model;

/** Enemigo del juego. Su comportamiento vive en threads.HiloEnemigo. */
public class Enemigo {
    public enum Tipo {
        VIGILANTE(450),  // patrulla en línea recta y embiste si te ve en su fila/columna
        RAPIDO(250);     // más veloz: vaga por el mapa y te persigue si estás cerca

        private final int pausaMs;
        Tipo(int pausaMs) { this.pausaMs = pausaMs; }
        public int getPausaMs() { return pausaMs; }
    }

    private final Tipo tipo;
    private volatile int fila;
    private volatile int columna;
    private volatile boolean vivo = true;
    private volatile boolean persiguiendo = false;   // para dibujar el aviso "!"

    public Enemigo(Tipo tipo, int fila, int columna) {
        this.tipo = tipo;
        this.fila = fila;
        this.columna = columna;
    }

    public Tipo getTipo() { return tipo; }
    public int getFila() { return fila; }
    public int getColumna() { return columna; }
    public boolean estaVivo() { return vivo; }
    public void matar() { vivo = false; }

    public boolean estaPersiguiendo() { return persiguiendo; }
    public void setPersiguiendo(boolean persiguiendo) { this.persiguiendo = persiguiendo; }

    public void setPosicion(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }
}
