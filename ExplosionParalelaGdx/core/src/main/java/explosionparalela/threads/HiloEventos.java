package explosionparalela.threads;

import explosionparalela.core.Juego;
import explosionparalela.model.Jugador;
import explosionparalela.model.Mapa;
import explosionparalela.model.TipoObjeto;
import explosionparalela.shared.ControlPausa;

import java.util.Random;

/**
 * HILO 4: eventos aleatorios. Cada cierto tiempo hace aparecer algo en el mapa:
 * casi siempre una moneda, y a veces un ítem de ayuda (corazón, reloj, escudo o fuego extra).
 * Se pueden agregar más eventos (trampas nuevas, enemigos, etc.) en eventoAleatorio().
 */
public class HiloEventos extends Thread {
    private static final double PROBABILIDAD_AYUDA = 0.35;

    private final Juego juego;
    private final ControlPausa control;
    private final Random azar = new Random();

    public HiloEventos(Juego juego, ControlPausa control) {
        super("Hilo-Eventos");
        setDaemon(true);
        this.juego = juego;
        this.control = control;
    }

    @Override
    public void run() {
        while (juego.estaActivo()) {
            if (!control.dormir(8000 + azar.nextInt(7000))) return;
            eventoAleatorio();
        }
    }

    private void eventoAleatorio() {
        Mapa mapa = juego.getMapa();
        Jugador jugador = juego.getJugador();
        int[] celda = mapa.celdaLibreAleatoria(3, jugador.getFila(), jugador.getColumna());
        if (celda == null) return;
        TipoObjeto objeto = azar.nextDouble() < PROBABILIDAD_AYUDA ? ayudaAleatoria(jugador.getVidas()) : TipoObjeto.MONEDA;
        mapa.colocarObjetoSuelo(celda[0], celda[1], objeto);
    }

    /** Elige una ayuda; el corazón sale más cuando te quedan pocas vidas y no sale con la vida llena. */
    private TipoObjeto ayudaAleatoria(int vidas) {
        double corazon = vidas >= Jugador.VIDAS_INICIALES ? 0 : (vidas == 1 ? 0.45 : 0.30);
        double reloj = 0.30;
        double escudo = 0.18;
        double alcance = 0.14;
        double total = corazon + reloj + escudo + alcance;
        double tiro = azar.nextDouble() * total;
        if (tiro < corazon) return TipoObjeto.CORAZON;
        tiro -= corazon;
        if (tiro < reloj) return TipoObjeto.RELOJ;
        tiro -= reloj;
        if (tiro < escudo) return TipoObjeto.ESCUDO;
        return TipoObjeto.ALCANCE;
    }
}
