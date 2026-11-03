package explosionparalela.model;

/**
 * Apariencia del jugador: color, forma del cuerpo y accesorio. Solo son datos
 * (inmutables); quien dibuja el avatar es la parte gráfica (libGDX).
 */
public final class Avatar {
    public enum Forma {
        REDONDO("Redondo", "redondo"),
        CUADRADO("Cuadrado", "cuadrado"),
        HEXAGONO("Hexágono", "hexagono");

        private final String nombre;
        private final String archivo;
        Forma(String nombre, String archivo) {
            this.nombre = nombre;
            this.archivo = archivo;
        }
        /** Nombre del sprite: imagenes/avatar_cuerpo_&lt;archivo&gt;.png */
        public String getArchivo() { return archivo; }
        @Override public String toString() { return nombre; }
    }

    public enum Accesorio {
        NINGUNO("Ninguno", null),
        SOMBRERO_CHARRO("Sombrero de charro", "sombrero_charro"),
        GORRO_LANA("Gorro de lana", "gorro_lana"),
        MONO("Moño", "mono"),
        GORRA("Gorra", "gorra"),
        CORONA("Corona", "corona");

        private final String nombre;
        private final String archivo;
        Accesorio(String nombre, String archivo) {
            this.nombre = nombre;
            this.archivo = archivo;
        }
        /** Nombre del sprite: imagenes/avatar_acc_&lt;archivo&gt;.png (null = sin accesorio). */
        public String getArchivo() { return archivo; }
        @Override public String toString() { return nombre; }
    }

    private final int color;          // 0xRRGGBB
    private final Forma forma;
    private final Accesorio accesorio;

    public Avatar(int color, Forma forma, Accesorio accesorio) {
        this.color = color & 0xFFFFFF;
        this.forma = forma;
        this.accesorio = accesorio;
    }

    public static Avatar porDefecto() {
        return new Avatar(0x2E86DE, Forma.REDONDO, Accesorio.NINGUNO);
    }

    public int getColor() { return color; }
    public Forma getForma() { return forma; }
    public Accesorio getAccesorio() { return accesorio; }
}
