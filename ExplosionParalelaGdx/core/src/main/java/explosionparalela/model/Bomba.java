package explosionparalela.model;

import java.util.Collections;
import java.util.List;

/** Jarrito de barro que explota a los 4 segundos (lo controla threads.HiloBomba). */
public class Bomba {
    public static final int TIEMPO_MS = 4000;

    private final int fila;
    private final int columna;
    private volatile boolean enLlamas = false;
    private volatile List<int[]> fuego = Collections.emptyList();

    public Bomba(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public int getFila() { return fila; }
    public int getColumna() { return columna; }
    public boolean estaEnLlamas() { return enLlamas; }
    public List<int[]> getFuego() { return fuego; }

    public void explotar(List<int[]> celdasAfectadas) {
        this.fuego = celdasAfectadas;
        this.enLlamas = true;
    }
}
