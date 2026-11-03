package explosionparalela.model;

/** Avatar del jugador. */
public class Jugador {
    public static final int VIDAS_INICIALES = 3;
    private static final long INVULNERABLE_MS = 1500;

    private final String nombre;
    private final Avatar avatar;
    private volatile int fila;
    private volatile int columna;
    private int vidas = VIDAS_INICIALES;
    private volatile long invulnerableHasta = 0;
    private volatile long escudoHasta = 0;

    public Jugador(String nombre, Avatar avatar, int fila, int columna) {
        this.nombre = nombre;
        this.avatar = avatar;
        this.fila = fila;
        this.columna = columna;
    }

    public String getNombre() { return nombre; }
    public Avatar getAvatar() { return avatar; }
    public int getColor() { return avatar.getColor(); }
    public int getFila() { return fila; }
    public int getColumna() { return columna; }

    public void setPosicion(int fila, int columna) {
        this.fila = fila;
        this.columna = columna;
    }

    public synchronized int getVidas() { return vidas; }

    /** Quita una vida salvo que esté en periodo de invulnerabilidad o tenga el escudo puesto. */
    public synchronized boolean quitarVida() {
        long ahora = System.currentTimeMillis();
        if (vidas <= 0 || ahora < invulnerableHasta || ahora < escudoHasta) return false;
        vidas--;
        invulnerableHasta = ahora + INVULNERABLE_MS;
        return true;
    }

    public boolean estaInvulnerable() {
        return System.currentTimeMillis() < invulnerableHasta;
    }

    /** Recupera una vida (nunca más de VIDAS_INICIALES). Devuelve false si ya estaba llena. */
    public synchronized boolean curar() {
        if (vidas <= 0 || vidas >= VIDAS_INICIALES) return false;
        vidas++;
        return true;
    }

    /** Escudo: durante "ms" milisegundos nada te quita vidas. */
    public void darEscudo(long ms) {
        escudoHasta = Math.max(escudoHasta, System.currentTimeMillis()) + ms;
    }

    public boolean tieneEscudo() {
        return System.currentTimeMillis() < escudoHasta;
    }

    /** Milisegundos que le quedan al escudo (0 si no tiene). */
    public long escudoRestanteMs() {
        return Math.max(0L, escudoHasta - System.currentTimeMillis());
    }
}
