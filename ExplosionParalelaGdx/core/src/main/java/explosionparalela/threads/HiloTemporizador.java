package explosionparalela.threads;

import explosionparalela.core.Juego;
import explosionparalela.shared.ControlPausa;

import java.util.concurrent.atomic.AtomicInteger;

/** HILO 1: contrarreloj del juego (máximo 4 minutos). */
public class HiloTemporizador extends Thread {
    private final Juego juego;
    private final ControlPausa control;
    private final AtomicInteger restantes;

    public HiloTemporizador(Juego juego, ControlPausa control, int segundosTotales) {
        super("Hilo-Temporizador");
        setDaemon(true);
        this.juego = juego;
        this.control = control;
        this.restantes = new AtomicInteger(segundosTotales);
    }

    public int getRestantes() { return restantes.get(); }

    /** Agrega segundos sin pasar de "tope" (el máximo de la partida). Devuelve los segundos realmente sumados. */
    public int sumar(int segundos, int tope) {
        int antes = restantes.getAndUpdate(v -> Math.min(tope, v + segundos));
        return Math.min(tope, antes + segundos) - antes;
    }

    @Override
    public void run() {
        while (juego.estaActivo()) {
            if (!control.dormir(1000)) return;
            if (restantes.decrementAndGet() <= 0) {
                juego.terminar(false, "Se acabó el tiempo");
                return;
            }
        }
    }
}
