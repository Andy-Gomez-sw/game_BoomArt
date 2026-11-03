package explosionparalela.threads;

import explosionparalela.core.Juego;
import explosionparalela.model.Bomba;
import explosionparalela.shared.ControlPausa;

/** HILO 2: cada bomba (jarrito) controla su propio tiempo de explosión. */
public class HiloBomba extends Thread {
    private final Juego juego;
    private final ControlPausa control;
    private final Bomba bomba;

    public HiloBomba(Juego juego, ControlPausa control, Bomba bomba) {
        super("Hilo-Bomba-" + bomba.getFila() + "-" + bomba.getColumna());
        setDaemon(true);
        this.juego = juego;
        this.control = control;
        this.bomba = bomba;
    }

    @Override
    public void run() {
        if (!control.dormir(Bomba.TIEMPO_MS)) return;  // 4 segundos
        juego.explotarBomba(bomba);                    // explosión
        if (!control.dormir(400)) return;              // las llamas se ven un momento
        juego.retirarBomba(bomba);
    }
}
