package explosionparalela.model;

/**
 * Objetos que se pueden encontrar en los niveles.
 * Los objetos normales (peluche, zapato...) se guardan en el inventario. Las AYUDAS
 * (corazón, reloj, escudo, fuego extra) se usan al recogerlas: no se guardan.
 */
public enum TipoObjeto {
    MONEDA("Moneda", 10, false),
    PELUCHE("Peluche", 100, false),
    ZAPATO("Zapato", 100, false),
    CHALECO("Chaleco", 100, false),
    JARRITO("Jarrito de barro", 100, false),

    CORAZON("Corazón", 25, true),         // +1 vida (máximo 3)
    RELOJ("Reloj", 25, true),             // +30 s (sin pasar de 4 minutos)
    ESCUDO("Escudo", 25, true),           // no te hacen daño durante unos segundos
    ALCANCE("Fuego extra", 25, true);     // +1 casilla de alcance en las explosiones (este nivel)

    private final String nombre;
    private final int puntos;
    private final boolean ayuda;

    TipoObjeto(String nombre, int puntos, boolean ayuda) {
        this.nombre = nombre;
        this.puntos = puntos;
        this.ayuda = ayuda;
    }

    public String getNombre() { return nombre; }
    public int getPuntos() { return puntos; }

    /** true si es un ítem de ayuda (se aplica al recogerlo en vez de guardarse). */
    public boolean esAyuda() { return ayuda; }
}
