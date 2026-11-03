package explosionparalela.shared;

/**
 * Monitor para pausar/reanudar/detener TODOS los hilos del juego a la vez.
 * Cada hilo duerme con dormir(ms), que respeta la pausa.
 */
public class ControlPausa {
    private boolean pausado = false;
    private boolean detenido = false;

    public synchronized void pausar() { pausado = true; }

    public synchronized void reanudar() {
        pausado = false;
        notifyAll();
    }

    public synchronized void detener() {
        detenido = true;
        pausado = false;
        notifyAll();
    }

    public synchronized boolean isPausado() { return pausado; }
    public synchronized boolean isDetenido() { return detenido; }

    /** Bloquea mientras esté en pausa. Devuelve false si el juego fue detenido. */
    public synchronized boolean esperarSiPausado() throws InterruptedException {
        while (pausado && !detenido) {
            wait();
        }
        return !detenido;
    }

    /** Duerme "ms" milisegundos sin avanzar mientras el juego esté en pausa. */
    public boolean dormir(long ms) {
        try {
            long restante = ms;
            while (restante > 0) {
                long paso = Math.min(50, restante);
                Thread.sleep(paso);
                if (!esperarSiPausado()) return false;
                restante -= paso;
            }
            return !isDetenido();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
