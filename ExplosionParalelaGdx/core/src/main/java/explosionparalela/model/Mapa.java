package explosionparalela.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Mapa del nivel. Es un recurso COMPARTIDO entre hilos (bombas, enemigos,
 * eventos, interfaz), por eso todos sus métodos son synchronized.
 */
public class Mapa {
    private final int filas;
    private final int columnas;
    private final TipoCelda[][] celdas;
    private final Map<Integer, TipoObjeto> ocultos = new HashMap<>();  // dentro de muros
    private final Map<Integer, TipoObjeto> suelo = new HashMap<>();    // ya visibles
    private final Set<Integer> bombasOcultas = new HashSet<>();        // dentro de muros
    private final Random azar = new Random();

    public Mapa(int filas, int columnas) {
        this.filas = filas;
        this.columnas = columnas;
        this.celdas = new TipoCelda[filas][columnas];
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                celdas[f][c] = TipoCelda.VACIO;
            }
        }
    }

    private int clave(int f, int c) { return f * columnas + c; }

    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }

    public synchronized boolean dentro(int f, int c) {
        return f >= 0 && f < filas && c >= 0 && c < columnas;
    }

    public synchronized TipoCelda getTipo(int f, int c) {
        return dentro(f, c) ? celdas[f][c] : TipoCelda.INDESTRUCTIBLE;
    }

    public synchronized void setTipo(int f, int c, TipoCelda tipo) {
        if (dentro(f, c)) celdas[f][c] = tipo;
    }

    /** Casillas por las que puede caminar el jugador. */
    public synchronized boolean esTransitable(int f, int c) {
        TipoCelda t = getTipo(f, c);
        return t == TipoCelda.VACIO || t == TipoCelda.PUERTA || t == TipoCelda.TRAMPA;
    }

    /** Casillas por las que caminan los enemigos (evitan las trampas). */
    public synchronized boolean esCaminoEnemigo(int f, int c) {
        TipoCelda t = getTipo(f, c);
        return t == TipoCelda.VACIO || t == TipoCelda.PUERTA;
    }

    // ---- contenido escondido dentro de los muros destructibles

    public synchronized void ocultarObjeto(int f, int c, TipoObjeto tipo) {
        ocultos.put(clave(f, c), tipo);
    }

    public synchronized void ocultarBomba(int f, int c) {
        bombasOcultas.add(clave(f, c));
    }

    /** Devuelve true (y la quita) si ese muro escondía una bomba. */
    public synchronized boolean quitarBombaOculta(int f, int c) {
        return bombasOcultas.remove(clave(f, c));
    }

    /** Destruye un muro destructible. Si escondía un objeto, éste queda en el suelo. */
    public synchronized boolean destruirMuro(int f, int c) {
        if (getTipo(f, c) != TipoCelda.DESTRUCTIBLE) return false;
        celdas[f][c] = TipoCelda.VACIO;
        TipoObjeto objeto = ocultos.remove(clave(f, c));
        if (objeto != null) suelo.put(clave(f, c), objeto);
        return true;
    }

    // ---- objetos en el suelo

    public synchronized TipoObjeto getObjetoSuelo(int f, int c) {
        return suelo.get(clave(f, c));
    }

    /** Recoge (y quita) el objeto de esa celda; null si no hay nada. */
    public synchronized TipoObjeto recogerObjeto(int f, int c) {
        return suelo.remove(clave(f, c));
    }

    public synchronized void colocarObjetoSuelo(int f, int c, TipoObjeto tipo) {
        if (getTipo(f, c) == TipoCelda.VACIO && !suelo.containsKey(clave(f, c))) {
            suelo.put(clave(f, c), tipo);
        }
    }

    /** Busca una celda de piso libre a distancia (Manhattan) mínima de una referencia. */
    public synchronized int[] celdaLibreAleatoria(int distMin, int fRef, int cRef) {
        for (int i = 0; i < 1000; i++) {
            int f = azar.nextInt(filas);
            int c = azar.nextInt(columnas);
            if (getTipo(f, c) == TipoCelda.VACIO && !suelo.containsKey(clave(f, c))
                    && Math.abs(f - fRef) + Math.abs(c - cRef) >= distMin) {
                return new int[]{f, c};
            }
        }
        return null;
    }
}
