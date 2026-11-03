package explosionparalela.graficos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Carga (una sola vez) las imágenes de assets/imagenes y las fuentes de assets/fuentes,
 * y trae unas funciones para dibujar texto, paneles y fondos.
 *
 * Todas las imágenes son opcionales: si una no existe, region(...) devuelve null y
 * quien dibuja la omite o usa otra. Para cambiar un diseño basta con reemplazar el
 * archivo (png, jpg o jpeg) con el mismo nombre dentro de assets/imagenes.
 */
public class Recursos implements Disposable {
    private static final String[] EXTENSIONES = {".jpg", ".jpeg", ".png"};
    private static final String CARACTERES = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóúüñÁÉÍÓÚÜÑ¡¿·…—«»°";

    private final Map<String, TextureRegion> regiones = new HashMap<>();
    private final Set<String> ausentes = new HashSet<>();
    private final Map<Integer, BitmapFont> fuentes = new HashMap<>();
    private FreeTypeFontGenerator generador;
    private NinePatch panel;
    private NinePatch panelBorde;
    private boolean panelListo = false;
    private final Color auxiliar = new Color();

    public Recursos() {
        FileHandle ttf = Gdx.files.internal("fuentes/LilitaOne-Regular.ttf");
        if (ttf.exists()) {
            generador = new FreeTypeFontGenerator(ttf);
        } else {
            Gdx.app.error("Recursos", "Falta assets/fuentes/LilitaOne-Regular.ttf: uso la fuente por defecto");
        }
    }

    // ----------------------------------------------------------- imágenes

    /** Primera imagen que exista entre los nombres (sin extensión), o null. */
    public TextureRegion region(String... nombres) {
        for (String nombre : nombres) {
            TextureRegion r = cargar(nombre);
            if (r != null) return r;
        }
        return null;
    }

    private TextureRegion cargar(String nombre) {
        TextureRegion r = regiones.get(nombre);
        if (r != null) return r;
        if (ausentes.contains(nombre)) return null;
        for (String ext : EXTENSIONES) {
            FileHandle archivo = Gdx.files.internal("imagenes/" + nombre + ext);
            if (!archivo.exists()) continue;
            try {
                Texture t = new Texture(archivo, true);          // con mipmaps: se ve bien al reducirla
                t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                r = new TextureRegion(t);
                regiones.put(nombre, r);
                return r;
            } catch (RuntimeException e) {
                Gdx.app.error("Recursos", "No pude leer " + archivo.path() + ": " + e.getMessage());
            }
        }
        ausentes.add(nombre);
        return null;
    }

    /** Dibuja una imagen escalada para CUBRIR el rectángulo (recorta lo que sobre, centrado). */
    public void cubrir(Batch b, TextureRegion r, float x, float y, float ancho, float alto) {
        if (r == null) return;
        float escala = Math.max(ancho / r.getRegionWidth(), alto / r.getRegionHeight());
        float cw = ancho / escala;
        float ch = alto / escala;
        int sx = r.getRegionX() + Math.round((r.getRegionWidth() - cw) / 2f);
        int sy = r.getRegionY() + Math.round((r.getRegionHeight() - ch) / 2f);
        b.draw(r.getTexture(), x, y, ancho, alto, sx, sy, Math.round(cw), Math.round(ch), false, false);
    }

    /** Rectángulo de un color (usa la imagen blanco.png). */
    public void rect(Batch b, float x, float y, float ancho, float alto, Color color) {
        TextureRegion blanco = region("blanco");
        if (blanco == null) return;
        Color antes = b.getColor();
        float r = antes.r, g = antes.g, bl = antes.b, a = antes.a;
        b.setColor(color);
        b.draw(blanco, x, y, ancho, alto);
        b.setColor(r, g, bl, a);
    }

    /** Panel redondeado (relleno), teñido con el color del batch. */
    public void panel(Batch b, float x, float y, float ancho, float alto) {
        prepararPaneles();
        if (panel != null) panel.draw(b, x, y, ancho, alto);
    }

    /** Solo el borde redondeado del panel, teñido con el color del batch. */
    public void panelBorde(Batch b, float x, float y, float ancho, float alto) {
        prepararPaneles();
        if (panelBorde != null) panelBorde.draw(b, x, y, ancho, alto);
    }

    private void prepararPaneles() {
        if (panelListo) return;
        panelListo = true;
        TextureRegion p = region("panel");
        TextureRegion pb = region("panel_borde");
        if (p != null) panel = new NinePatch(p, 32, 32, 32, 32);
        if (pb != null) panelBorde = new NinePatch(pb, 32, 32, 32, 32);
    }

    // -------------------------------------------------------------- texto

    /** Fuente del tamaño pedido (con borde oscuro y sombra). Se crea una vez y se reutiliza. */
    public BitmapFont fuente(int tam) {
        BitmapFont f = fuentes.get(tam);
        if (f != null) return f;
        if (generador != null) {
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.size = tam;
            p.characters = CARACTERES;
            p.color = Color.WHITE;
            p.borderWidth = Math.max(1.5f, tam / 14f);
            p.borderColor = new Color(0.10f, 0.07f, 0.04f, 1f);
            p.shadowOffsetY = Math.max(2, tam / 16);
            p.shadowColor = new Color(0f, 0f, 0f, 0.35f);
            p.minFilter = Texture.TextureFilter.Linear;
            p.magFilter = Texture.TextureFilter.Linear;
            f = generador.generateFont(p);
        } else {
            f = new BitmapFont();
            f.getData().setScale(tam / 15f);
        }
        f.setUseIntegerPositions(false);
        fuentes.put(tam, f);
        return f;
    }

    /**
     * Escribe texto. (x, y) es el punto de la esquina superior del texto según la
     * alineación: Align.left (x = borde izquierdo), Align.center (x = centro) o
     * Align.right (x = borde derecho).
     */
    public void texto(Batch b, int tam, String texto, float x, float y, int alineacion, Color color) {
        BitmapFont f = fuente(tam);
        f.setColor(color);
        f.draw(b, texto, x, y, 0, alineacion, false);
        f.setColor(Color.WHITE);
    }

    public void texto(Batch b, int tam, String texto, float x, float y, int alineacion) {
        texto(b, tam, texto, x, y, alineacion, Color.WHITE);
    }

    /** Ancho del texto con la fuente de ese tamaño. */
    public float anchoTexto(int tam, String texto) {
        com.badlogic.gdx.graphics.g2d.GlyphLayout medida = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
        medida.setText(fuente(tam), texto);
        return medida.width;
    }

    /** Convierte 0xRRGGBB en un Color (reutiliza el mismo objeto: no lo guardes). */
    public Color color(int rgb) {
        Color.rgb888ToColor(auxiliar, rgb);
        auxiliar.a = 1f;
        return auxiliar;
    }

    public static final int IZQ = Align.left;
    public static final int CENTRO = Align.center;
    public static final int DER = Align.right;

    @Override
    public void dispose() {
        for (TextureRegion r : regiones.values()) r.getTexture().dispose();
        regiones.clear();
        for (BitmapFont f : fuentes.values()) f.dispose();
        fuentes.clear();
        if (generador != null) generador.dispose();
    }
}
