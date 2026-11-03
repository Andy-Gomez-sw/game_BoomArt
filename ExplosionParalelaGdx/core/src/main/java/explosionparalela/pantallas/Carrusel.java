package explosionparalela.pantallas;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import explosionparalela.graficos.Recursos;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Carrusel giratorio de tarjetas: la opción elegida va al centro, más grande y con
 * borde dorado, y las demás se ven a los lados. Gira en círculo con animación suave.
 * Las coordenadas son del mundo virtual (origen abajo a la izquierda).
 */
public class Carrusel<T> {
    /** Dibuja el contenido de una tarjeta dentro del rectángulo (x, y, ancho, alto). */
    public interface Pintor<T> {
        void pintar(Batch b, T elemento, float x, float y, float ancho, float alto);
    }

    public static final float TARJ_W = 132;
    public static final float TARJ_H = 168;

    private static final Color DORADO = new Color(1f, 0.784f, 0.341f, 1f);
    private static final Color BORDE_TENUE = new Color(0.478f, 0.388f, 0.314f, 1f);
    private static final Color TARJETA = new Color(0.30f, 0.23f, 0.18f, 1f);
    private static final Color TEXTO_SUAVE = new Color(0.75f, 0.69f, 0.61f, 1f);

    private final List<T> elementos;
    private final Pintor<T> pintor;
    private final Recursos recursos;
    private final float centroX;
    private final float centroY;
    private final float ancho;           // ancho total del carrusel (las flechas van en los extremos)

    private int indice = 0;
    private float posicion = 0;
    private int flechaEncima = 0;        // -1 izquierda, 1 derecha, 0 ninguna
    private Runnable alCambiar = () -> { };

    public Carrusel(Recursos recursos, List<T> elementos, Pintor<T> pintor,
                    float centroX, float centroY, float ancho) {
        this.recursos = recursos;
        this.elementos = elementos;
        this.pintor = pintor;
        this.centroX = centroX;
        this.centroY = centroY;
        this.ancho = ancho;
    }

    // ---------------------------------------------------------- selección

    public T getSeleccionado() { return elementos.get(indice); }

    public int getIndice() { return indice; }

    public int getCantidad() { return elementos.size(); }

    public void setAlCambiar(Runnable alCambiar) { this.alCambiar = alCambiar; }

    public void siguiente() { seleccionar(indice + 1); }

    public void anterior() { seleccionar(indice - 1); }

    public void seleccionar(int i) {
        int n = elementos.size();
        int nuevo = ((i % n) + n) % n;
        if (nuevo == indice) return;
        indice = nuevo;
        alCambiar.run();
    }

    /** Cada cuadro: acerca la posición visible al índice elegido por el camino más corto. */
    public void actualizar(float dt) {
        int n = elementos.size();
        float delta = indice - posicion;
        delta -= n * Math.round(delta / n);
        if (Math.abs(delta) < 0.003f) {
            posicion = indice;
        } else {
            posicion += delta * (1f - (float) Math.exp(-14f * dt));
            posicion = ((posicion % n) + n) % n;
        }
    }

    // ---------------------------------------------------------- geometría

    private float distancia(int i) {
        int n = elementos.size();
        float d = i - posicion;
        return d - n * Math.round(d / n);
    }

    private static float escala(float d) { return 1f - 0.27f * Math.min(Math.abs(d), 2f); }

    private float x(float d) {
        float a = Math.min(Math.abs(d), 2f);
        return centroX + Math.signum(d) * (Math.abs(d) * 150f * (1f - 0.13f * a));
    }

    private static float transparencia(float d) { return Math.max(0f, 1f - 0.36f * Math.abs(d)); }

    private Integer[] ordenDeDibujo() {
        Integer[] orden = new Integer[elementos.size()];
        for (int i = 0; i < orden.length; i++) orden[i] = i;
        Arrays.sort(orden, Comparator.comparingDouble(i -> -Math.abs(distancia(i))));
        return orden;
    }

    /** ¿Está el punto sobre el carrusel (incluye las flechas)? */
    public boolean contiene(float px, float py) {
        return Math.abs(px - centroX) <= ancho / 2f && Math.abs(py - centroY) <= TARJ_H / 2f + 8;
    }

    private int tarjetaEn(float px, float py) {
        Integer[] orden = ordenDeDibujo();
        for (int k = orden.length - 1; k >= 0; k--) {
            int i = orden[k];
            float d = distancia(i);
            if (Math.abs(d) > 2.4f) continue;
            float esc = escala(d);
            if (Math.abs(px - x(d)) <= TARJ_W * esc / 2f && Math.abs(py - centroY) <= TARJ_H * esc / 2f) {
                return i;
            }
        }
        return -1;
    }

    private boolean enFlecha(float px, int lado) {
        float borde = lado < 0 ? centroX - ancho / 2f : centroX + ancho / 2f;
        return Math.abs(px - borde) < 40f;
    }

    // ------------------------------------------------------------- entrada

    /** Clic o toque. Devuelve true si el carrusel lo usó. */
    public boolean tocar(float px, float py) {
        if (!contiene(px, py)) return false;
        if (enFlecha(px, -1)) {
            anterior();
        } else if (enFlecha(px, 1)) {
            siguiente();
        } else {
            int i = tarjetaEn(px, py);
            if (i >= 0) seleccionar(i);
        }
        return true;
    }

    public void mover(float px, float py) {
        flechaEncima = !contiene(px, py) ? 0 : (enFlecha(px, -1) ? -1 : (enFlecha(px, 1) ? 1 : 0));
    }

    // -------------------------------------------------------------- dibujo

    public void dibujar(Batch b, boolean enfocado) {
        for (int i : ordenDeDibujo()) {
            float d = distancia(i);
            if (Math.abs(d) > 2.4f) continue;
            dibujarTarjeta(b, i, d);
        }
        dibujarFlecha(b, -1);
        dibujarFlecha(b, 1);
        if (enfocado) {   // línea dorada tenue abajo cuando tiene el foco del teclado
            recursos.rect(b, centroX - 60, centroY - TARJ_H / 2f - 14, 120, 3, new Color(1f, 0.784f, 0.341f, 0.85f));
        }
    }

    private void dibujarTarjeta(Batch b, int i, float d) {
        float esc = escala(d);
        float alfa = transparencia(d);
        float sel = Math.max(0f, 1f - Math.abs(d));
        float w = TARJ_W * esc;
        float h = TARJ_H * esc;
        float x = x(d) - w / 2f;
        float y = centroY - h / 2f;

        if (sel > 0.05f) {                                   // resplandor dorado
            b.setColor(DORADO.r, DORADO.g, DORADO.b, 0.28f * sel);
            recursos.panelBorde(b, x - 7, y - 7, w + 14, h + 14);
        }
        b.setColor(TARJETA.r, TARJETA.g, TARJETA.b, alfa);
        recursos.panel(b, x, y, w, h);
        float mr = BORDE_TENUE.r + (DORADO.r - BORDE_TENUE.r) * sel;
        float mg = BORDE_TENUE.g + (DORADO.g - BORDE_TENUE.g) * sel;
        float mb = BORDE_TENUE.b + (DORADO.b - BORDE_TENUE.b) * sel;
        b.setColor(mr, mg, mb, alfa);
        recursos.panelBorde(b, x, y, w, h);

        b.setColor(1f, 1f, 1f, alfa);
        pintor.pintar(b, elementos.get(i), x, y, w, h);

        // nombre abajo, en una o dos líneas
        String texto = String.valueOf(elementos.get(i));
        int tam = Math.max(14, Math.round(22 * esc));
        float anchoMax = w - 12;
        String[] lineas = {texto, ""};
        if (recursos.anchoTexto(tam, texto) > anchoMax) {
            int corte = texto.lastIndexOf(' ');
            if (corte > 0) lineas = new String[]{texto.substring(0, corte), texto.substring(corte + 1)};
        }
        Color color = new Color(
                TEXTO_SUAVE.r + (DORADO.r - TEXTO_SUAVE.r) * sel,
                TEXTO_SUAVE.g + (DORADO.g - TEXTO_SUAVE.g) * sel,
                TEXTO_SUAVE.b + (DORADO.b - TEXTO_SUAVE.b) * sel, alfa);
        float ty = lineas[1].isEmpty() ? y + 8 + tam * 1.05f : y + 6 + tam * 2.1f;
        for (String linea : lineas) {
            if (linea.isEmpty()) continue;
            recursos.texto(b, tam, linea, x + w / 2f, ty, Recursos.CENTRO, color);
            ty -= tam * 1.05f;
        }
        b.setColor(1f, 1f, 1f, 1f);
    }

    private void dibujarFlecha(Batch b, int lado) {
        TextureRegion flecha = recursos.region("flecha");
        if (flecha == null) return;
        boolean encima = flechaEncima == lado;
        float tam = encima ? 46 : 38;
        float x = lado < 0 ? centroX - ancho / 2f + 22 : centroX + ancho / 2f - 22;
        b.setColor(DORADO.r, DORADO.g, DORADO.b, encima ? 1f : 0.65f);
        b.draw(flecha, x - tam / 2f, centroY - tam / 2f, tam / 2f, tam / 2f, tam, tam, lado < 0 ? -1f : 1f, 1f, 0f);
        b.setColor(1f, 1f, 1f, 1f);
    }
}
