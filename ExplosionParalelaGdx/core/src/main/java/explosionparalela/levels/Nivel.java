package explosionparalela.levels;

import explosionparalela.model.Enemigo;
import explosionparalela.model.Mapa;
import explosionparalela.model.TipoCelda;
import explosionparalela.model.TipoObjeto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Definición de un nivel: mapa, enemigos, objetos por encontrar y colores.
 * Nivel.crear(n) arma un nivel NUEVO cada vez (muros, objetos y enemigos al azar).
 *
 * Para cambiar la dificultad o agregar niveles, edita el switch de crear().
 */
public class Nivel {
    public static final int TOTAL = 4;

    private final int numero;
    private final String municipio;
    private final String producto;
    private final String caracteristicas;
    private final List<TipoObjeto> requeridos;
    private final int colorSuelo;
    private final int colorMuroFijo;
    private final int colorMuroRompible;

    private final Mapa mapa;
    private final List<Enemigo> enemigos = new ArrayList<>();
    private int inicioFila = 1;
    private int inicioColumna = 1;

    /**
     * @param plano       mapa base ('#' fijo, '.' piso, 'D' puerta, 'P' inicio)
     * @param densidad    probabilidad (0 a 1) de que un piso se llene con muro destructible
     * @param vigilantes  cuántos enemigos vigilantes
     * @param rapidos     cuántos enemigos rápidos
     * @param trampas     trampas visibles repartidas por el mapa
     * @param bombasOcultas bombas escondidas dentro de muros destructibles
     * @param monedas     monedas escondidas dentro de muros destructibles
     * @param ayudas      ítems de ayuda (corazón, reloj, escudo, fuego extra) escondidos en muros
     */
    private Nivel(int numero, String municipio, String producto, String caracteristicas,
                  String[] plano, double densidad, int vigilantes, int rapidos,
                  int trampas, int bombasOcultas, int monedas,
                  List<TipoObjeto> requeridos, List<TipoObjeto> ayudas,
                  int colorSuelo, int colorMuroFijo, int colorMuroRompible) {
        this.numero = numero;
        this.municipio = municipio;
        this.producto = producto;
        this.caracteristicas = caracteristicas;
        this.requeridos = Collections.unmodifiableList(new ArrayList<>(requeridos));
        this.colorSuelo = colorSuelo;
        this.colorMuroFijo = colorMuroFijo;
        this.colorMuroRompible = colorMuroRompible;

        Random azar = new Random();
        int filas = plano.length;
        int columnas = plano[0].length();
        mapa = new Mapa(filas, columnas);

        // 1) plano base
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                switch (plano[f].charAt(c)) {
                    case '#': mapa.setTipo(f, c, TipoCelda.INDESTRUCTIBLE); break;
                    case 'D': mapa.setTipo(f, c, TipoCelda.PUERTA); break;
                    case 'P': inicioFila = f; inicioColumna = c; break;
                    default: break;
                }
            }
        }

        // 2) trampas (solo en cruces del mapa, lejos del inicio)
        List<int[]> candidatas = pisos(6);
        Collections.shuffle(candidatas, azar);
        int puestas = 0;
        for (int[] cel : candidatas) {
            if (puestas >= trampas) break;
            if (cel[0] % 2 == 1 && cel[1] % 2 == 1) {
                mapa.setTipo(cel[0], cel[1], TipoCelda.TRAMPA);
                puestas++;
            }
        }

        // 3) muros destructibles al azar (no cerca del inicio)
        List<int[]> destructibles = new ArrayList<>();
        for (int[] cel : pisos(3)) {
            if (azar.nextDouble() < densidad) {
                mapa.setTipo(cel[0], cel[1], TipoCelda.DESTRUCTIBLE);
                destructibles.add(cel);
            }
        }
        int necesarios = requeridos.size() + ayudas.size() + bombasOcultas + monedas;
        if (destructibles.size() < necesarios) {          // por si salieron pocos
            List<int[]> extra = pisos(3);
            Collections.shuffle(extra, azar);
            for (int[] cel : extra) {
                if (destructibles.size() >= necesarios) break;
                mapa.setTipo(cel[0], cel[1], TipoCelda.DESTRUCTIBLE);
                destructibles.add(cel);
            }
        }

        // 4) esconder objetos, bombas y monedas dentro de los muros
        Collections.shuffle(destructibles, azar);
        int i = 0;
        for (TipoObjeto objeto : requeridos) {
            int[] cel = destructibles.get(i++);
            mapa.ocultarObjeto(cel[0], cel[1], objeto);
        }
        for (TipoObjeto ayuda : ayudas) {                  // ítems de ayuda escondidos en los muros
            int[] cel = destructibles.get(i++);
            mapa.ocultarObjeto(cel[0], cel[1], ayuda);
        }
        for (int k = 0; k < bombasOcultas; k++) {
            int[] cel = destructibles.get(i++);
            mapa.ocultarBomba(cel[0], cel[1]);
        }
        for (int k = 0; k < monedas; k++) {
            int[] cel = destructibles.get(i++);
            mapa.ocultarObjeto(cel[0], cel[1], TipoObjeto.MONEDA);
        }

        // 5) enemigos lejos del inicio (cada uno en una casilla distinta)
        java.util.Set<Integer> usadas = new java.util.HashSet<>();
        for (int k = 0; k < vigilantes + rapidos; k++) {
            int[] cel = null;
            for (int intento = 0; intento < 100; intento++) {
                int distMin = intento < 60 ? 8 : 4;
                int[] prueba = mapa.celdaLibreAleatoria(distMin, inicioFila, inicioColumna);
                if (prueba != null && usadas.add(prueba[0] * columnas + prueba[1])) {
                    cel = prueba;
                    break;
                }
            }
            if (cel == null) break;
            Enemigo.Tipo tipo = k < vigilantes ? Enemigo.Tipo.VIGILANTE : Enemigo.Tipo.RAPIDO;
            enemigos.add(new Enemigo(tipo, cel[0], cel[1]));
        }
    }

    /** Pisos libres a distancia (Manhattan) mínima del inicio. */
    private List<int[]> pisos(int distMin) {
        List<int[]> lista = new ArrayList<>();
        for (int f = 0; f < mapa.getFilas(); f++) {
            for (int c = 0; c < mapa.getColumnas(); c++) {
                if (mapa.getTipo(f, c) == TipoCelda.VACIO
                        && Math.abs(f - inicioFila) + Math.abs(c - inicioColumna) >= distMin) {
                    lista.add(new int[]{f, c});
                }
            }
        }
        return lista;
    }

    /** Mapa abierto con pilares fijos tipo Bomberman. */
    private static String[] abierto(int filas, int columnas) {
        String[] plano = new String[filas];
        for (int f = 0; f < filas; f++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < columnas; c++) {
                boolean borde = f == 0 || c == 0 || f == filas - 1 || c == columnas - 1;
                boolean pilar = f % 2 == 0 && c % 2 == 0;
                sb.append(borde || pilar ? '#' : (f == 1 && c == 1 ? 'P' : '.'));
            }
            plano[f] = sb.toString();
        }
        return plano;
    }

    /** Crea el nivel 1..4 (con todo al azar de nuevo). */
    public static Nivel crear(int numero) {
        switch (numero) {
            case 1:   // Xonacatlán - peluches
                return new Nivel(1, "Xonacatlán", "Fabricación de peluches",
                        "Mapa pequeño · Muros destructibles",
                        abierto(13, 15), 0.45, 3, 0, 0, 0, 6,
                        Arrays.asList(TipoObjeto.PELUCHE),
                        Arrays.asList(TipoObjeto.RELOJ, TipoObjeto.CORAZON),
                        0xF6E7EC, 0x8E5A6E, 0xF48FB1);
            case 2:   // San Mateo Atenco - zapatos
                return new Nivel(2, "San Mateo Atenco", "Producción de zapatos",
                        "Mapa más grande · Más muros · Bombas ocultas · Trampas",
                        abierto(15, 19), 0.60, 3, 2, 6, 4, 8,
                        Arrays.asList(TipoObjeto.ZAPATO, TipoObjeto.PELUCHE),
                        Arrays.asList(TipoObjeto.CORAZON, TipoObjeto.RELOJ, TipoObjeto.ESCUDO),
                        0xE9DFCB, 0x4E342E, 0xA1887F);
            case 3:   // Tianguistenco - lana
                return new Nivel(3, "Tianguistenco", "Productos de lana",
                        "Laberinto · Habitaciones · Puertas · Pasadizos · Enemigos rápidos",
                        Laberintos.TIANGUISTENCO, 0.18, 2, 3, 0, 2, 8,
                        Arrays.asList(TipoObjeto.CHALECO, TipoObjeto.PELUCHE, TipoObjeto.ZAPATO),
                        Arrays.asList(TipoObjeto.CORAZON, TipoObjeto.RELOJ, TipoObjeto.ESCUDO, TipoObjeto.ALCANCE),
                        0xE8EBF2, 0x455A64, 0xB0BEC5);
            case 4:   // Metepec - barro
                return new Nivel(4, "Metepec", "Artesanías de barro",
                        "Laberinto · Habitaciones · Puertas · Pasadizos · Enemigos rápidos",
                        Laberintos.METEPEC, 0.20, 2, 4, 0, 3, 10,
                        Arrays.asList(TipoObjeto.JARRITO, TipoObjeto.CHALECO,
                                TipoObjeto.PELUCHE, TipoObjeto.ZAPATO),
                        Arrays.asList(TipoObjeto.CORAZON, TipoObjeto.CORAZON, TipoObjeto.RELOJ,
                                TipoObjeto.ESCUDO, TipoObjeto.ALCANCE),
                        0xE8D8B0, 0x4A3A2E, 0xC8794B);
            default:
                throw new IllegalArgumentException("Nivel inexistente: " + numero);
        }
    }

    // ------------------------------------------------------------ consultas

    /** Texto con lo que hay que hacer para pasar el nivel. */
    public String getObjetivo() {
        StringBuilder sb = new StringBuilder("Elimina a los ");
        sb.append(enemigos.size()).append(" enemigos y encuentra: ");
        for (int i = 0; i < requeridos.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(requeridos.get(i).getNombre());
        }
        return sb.toString();
    }

    public int getNumero() { return numero; }
    public String getMunicipio() { return municipio; }
    public String getProducto() { return producto; }
    public String getCaracteristicas() { return caracteristicas; }
    public List<TipoObjeto> getRequeridos() { return requeridos; }
    public Mapa getMapa() { return mapa; }
    public List<Enemigo> getEnemigos() { return enemigos; }
    public int getInicioFila() { return inicioFila; }
    public int getInicioColumna() { return inicioColumna; }
    public int getColorSuelo() { return colorSuelo; }
    public int getColorMuroFijo() { return colorMuroFijo; }
    public int getColorMuroRompible() { return colorMuroRompible; }
}
