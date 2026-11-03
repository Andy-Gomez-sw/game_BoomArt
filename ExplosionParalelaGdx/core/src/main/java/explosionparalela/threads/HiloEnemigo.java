package explosionparalela.threads;

import explosionparalela.core.Juego;
import explosionparalela.model.Enemigo;
import explosionparalela.model.Jugador;
import explosionparalela.shared.ControlPausa;

import java.util.Random;

/**
 * HILO 3: cada enemigo tiene su propio hilo y su propio comportamiento.
 *
 *  - VIGILANTE: patrulla en línea recta (vertical u horizontal). Si el jugador
 *    está en su misma fila o columna, a la vista y cerca, EMBISTE hacia él.
 *  - RAPIDO: vaga en varias direcciones. Si el jugador está a menos de
 *    RADIO_RAPIDO pasos de camino, lo PERSIGUE por el camino más corto.
 */
public class HiloEnemigo extends Thread {
    /** Casillas a las que el vigilante ve en línea recta. */
    public static final int VISION_VIGILANTE = 6;
    /** Pasos de camino a los que el rápido detecta al jugador. */
    public static final int RADIO_RAPIDO = 12;

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final Juego juego;
    private final ControlPausa control;
    private final Enemigo enemigo;
    private final Random azar = new Random();
    private int df;
    private int dc;

    public HiloEnemigo(Juego juego, ControlPausa control, Enemigo enemigo) {
        super("Hilo-Enemigo-" + enemigo.getTipo());
        setDaemon(true);
        this.juego = juego;
        this.control = control;
        this.enemigo = enemigo;
        if (enemigo.getTipo() == Enemigo.Tipo.VIGILANTE) {
            // eje fijo: vertical u horizontal
            if (azar.nextBoolean()) { df = 1; dc = 0; } else { df = 0; dc = 1; }
        } else {
            elegirDireccionLibre();
        }
    }

    @Override
    public void run() {
        while (enemigo.estaVivo() && juego.estaActivo()) {
            if (!control.dormir(enemigo.getTipo().getPausaMs())) return;
            if (!enemigo.estaVivo()) return;

            boolean persiguiendo;
            if (enemigo.getTipo() == Enemigo.Tipo.VIGILANTE) {
                persiguiendo = embestir();
                if (!persiguiendo) patrullar();
            } else {
                persiguiendo = perseguir();
                if (!persiguiendo) vagar();
            }
            enemigo.setPersiguiendo(persiguiendo);
            juego.verificarContacto(enemigo);
        }
    }

    // ------------------------------------------------------------ vigilante

    /** Si ve al jugador en su fila/columna, da un paso hacia él. */
    private boolean embestir() {
        Jugador j = juego.getJugador();
        int ef = enemigo.getFila();
        int ec = enemigo.getColumna();
        int sf = 0;
        int sc = 0;
        int distancia;
        if (j.getFila() == ef && j.getColumna() != ec) {
            sc = Integer.signum(j.getColumna() - ec);
            distancia = Math.abs(j.getColumna() - ec);
        } else if (j.getColumna() == ec && j.getFila() != ef) {
            sf = Integer.signum(j.getFila() - ef);
            distancia = Math.abs(j.getFila() - ef);
        } else {
            return false;
        }
        if (distancia > VISION_VIGILANTE) return false;
        for (int k = 1; k < distancia; k++) {          // ¿hay algo en medio?
            if (!juego.puedeMover(ef + sf * k, ec + sc * k)) return false;
        }
        df = sf;
        dc = sc;
        if (juego.puedeMover(ef + sf, ec + sc)) {
            enemigo.setPosicion(ef + sf, ec + sc);
        }
        return true;
    }

    /**
     * Avanza en línea recta y rebota al toparse con un muro.
     * Si queda encerrado en su eje, cambia de eje (vertical <-> horizontal).
     */
    private void patrullar() {
        int nf = enemigo.getFila() + df;
        int nc = enemigo.getColumna() + dc;
        if (juego.puedeMover(nf, nc)) {
            enemigo.setPosicion(nf, nc);
            return;
        }
        df = -df;
        dc = -dc;
        if (!juego.puedeMover(enemigo.getFila() + df, enemigo.getColumna() + dc)) {
            int tmp = df;   // los dos lados bloqueados: probar el otro eje
            df = Math.abs(dc);
            dc = Math.abs(tmp);
        }
    }

    // -------------------------------------------------------------- rápido

    /** Si el jugador está cerca (por camino), da un paso por la ruta más corta. */
    private boolean perseguir() {
        Jugador j = juego.getJugador();
        int[] paso = Rutas.primerPaso(juego, enemigo.getFila(), enemigo.getColumna(),
                j.getFila(), j.getColumna(), RADIO_RAPIDO);
        if (paso == null) return false;
        df = paso[0];
        dc = paso[1];
        int nf = enemigo.getFila() + df;
        int nc = enemigo.getColumna() + dc;
        if (juego.puedeMover(nf, nc)) {
            enemigo.setPosicion(nf, nc);
        }
        return true;
    }

    /** Cambia de dirección al azar o cuando se topa con un muro. */
    private void vagar() {
        int nf = enemigo.getFila() + df;
        int nc = enemigo.getColumna() + dc;
        if (azar.nextDouble() < 0.25 || !juego.puedeMover(nf, nc)) {
            elegirDireccionLibre();
            nf = enemigo.getFila() + df;
            nc = enemigo.getColumna() + dc;
        }
        if (juego.puedeMover(nf, nc)) {
            enemigo.setPosicion(nf, nc);
        }
    }

    private void elegirDireccionLibre() {
        int inicio = azar.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int[] d = DIRS[(inicio + i) % 4];
            if (juego.puedeMover(enemigo.getFila() + d[0], enemigo.getColumna() + d[1])) {
                df = d[0];
                dc = d[1];
                return;
            }
        }
        df = 0;
        dc = 0;
    }
}
