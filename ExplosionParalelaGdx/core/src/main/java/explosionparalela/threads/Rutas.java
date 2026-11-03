package explosionparalela.threads;

import explosionparalela.core.Juego;
import explosionparalela.model.Mapa;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Búsqueda del camino más corto (BFS) para que los enemigos persigan al jugador. */
public final class Rutas {
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private Rutas() { }

    /**
     * Calcula el primer paso del camino más corto entre dos casillas, pasando solo
     * por casillas que los enemigos pueden pisar (la meta siempre cuenta, aunque
     * el jugador esté parado sobre una trampa).
     *
     * @param maxPasos longitud máxima del camino a considerar
     * @return {dFila, dColumna} del primer paso, o null si no hay camino dentro del alcance
     */
    public static int[] primerPaso(Juego juego, int fIni, int cIni, int fMeta, int cMeta, int maxPasos) {
        if (fIni == fMeta && cIni == cMeta) return null;
        Mapa mapa = juego.getMapa();
        int filas = mapa.getFilas();
        int columnas = mapa.getColumnas();

        int[][] dist = new int[filas][columnas];
        int[][] primero = new int[filas][columnas];   // dirección del primer paso para llegar ahí
        for (int[] fila : dist) Arrays.fill(fila, -1);

        ArrayDeque<int[]> cola = new ArrayDeque<>();
        dist[fIni][cIni] = 0;
        cola.add(new int[]{fIni, cIni});

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            int f = actual[0];
            int c = actual[1];
            if (dist[f][c] >= maxPasos) continue;

            for (int k = 0; k < DIRS.length; k++) {
                int nf = f + DIRS[k][0];
                int nc = c + DIRS[k][1];
                if (!mapa.dentro(nf, nc) || dist[nf][nc] != -1) continue;
                boolean esMeta = nf == fMeta && nc == cMeta;
                if (!esMeta && !juego.puedeMover(nf, nc)) continue;

                dist[nf][nc] = dist[f][c] + 1;
                primero[nf][nc] = (f == fIni && c == cIni) ? k : primero[f][c];
                if (esMeta) return DIRS[primero[nf][nc]];
                cola.add(new int[]{nf, nc});
            }
        }
        return null;
    }
}
