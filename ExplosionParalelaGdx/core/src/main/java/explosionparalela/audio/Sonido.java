package explosionparalela.audio;

/**
 * Punto único para pedir sonidos desde la lógica del juego (hilos de bombas,
 * enemigos, etc.). No reproduce nada por sí mismo: avisa a un "backend" (en libGDX,
 * GdxSonido) que los reproduce en el hilo gráfico. Así la lógica no depende de
 * ninguna librería de audio y se puede probar sin sonido.
 */
public final class Sonido {
    public enum Efecto {
        COLOCAR_BOMBA("colocar_bomba"),
        EXPLOSION("explosion"),
        MONEDA("moneda"),
        OBJETO("objeto"),
        GOLPE("golpe"),
        AYUDA("ayuda"),
        ENEMIGO_ELIMINADO("enemigo_eliminado"),
        NIVEL_COMPLETO("nivel_completo"),
        VICTORIA("victoria"),
        DERROTA("derrota");

        private final String archivo;
        Efecto(String archivo) { this.archivo = archivo; }

        /** Nombre del archivo en assets/sonidos (sin extensión). */
        public String getArchivo() { return archivo; }
    }

    public interface Backend {
        void efecto(Efecto efecto);
        void iniciarMusica(int nivel);
        void detenerMusica();
        void pausarMusica();
        void reanudarMusica();
        void silencio(boolean silenciado);
    }

    private static final Backend SIN_SONIDO = new Backend() {
        @Override public void efecto(Efecto efecto) { }
        @Override public void iniciarMusica(int nivel) { }
        @Override public void detenerMusica() { }
        @Override public void pausarMusica() { }
        @Override public void reanudarMusica() { }
        @Override public void silencio(boolean silenciado) { }
    };

    private static volatile Backend backend = SIN_SONIDO;
    private static volatile boolean silenciado = false;

    private Sonido() { }

    public static void setBackend(Backend nuevo) {
        backend = nuevo == null ? SIN_SONIDO : nuevo;
    }

    public static void reproducir(Efecto efecto) {
        if (!silenciado) backend.efecto(efecto);
    }

    public static void iniciarMusica(int nivel) { backend.iniciarMusica(nivel); }

    public static void detenerMusica() { backend.detenerMusica(); }

    public static void pausarMusica() { backend.pausarMusica(); }

    public static void reanudarMusica() { backend.reanudarMusica(); }

    /** Tecla M: activa o desactiva todo el sonido. */
    public static void alternarSilencio() {
        silenciado = !silenciado;
        backend.silencio(silenciado);
    }

    public static boolean isSilenciado() { return silenciado; }
}
