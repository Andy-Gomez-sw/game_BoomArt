package explosionparalela.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import explosionparalela.ExplosionParalelaJuego;
import explosionparalela.graficos.AvatarDibujo;
import explosionparalela.graficos.Recursos;
import explosionparalela.model.Avatar;

import java.util.Arrays;

/**
 * Pantalla de inicio: nombre del jugador y personalización del avatar con carruseles
 * (forma y accesorio), colores y una barra de matiz para elegir cualquier color.
 */
public class PantallaInicio extends ScreenAdapter {
    public static final float ANCHO = 1280;
    public static final float ALTO = 720;

    // ---- diseño (coordenadas del mundo virtual, origen abajo a la izquierda)
    static final float[] ESCENARIO = {70, 250, 360, 330};
    static final float[] CAMPO_NOMBRE = {70, 170, 360, 64};
    static final float[] BARRA_MATIZ = {70, 52, 360, 28};
    static final float FILA_COLORES_Y = 113;
    static final float COLOR_RADIO = 17;
    static final float COLOR_PASO = 42;
    static final float CARRUSEL_X = 870;
    static final float CARRUSEL_ANCHO = 700;
    static final float CARRUSEL_FORMA_Y = 500;
    static final float CARRUSEL_ACC_Y = 292;
    static final float[] BOTON_SORPRESA = {560, 70, 270, 70};
    static final float[] BOTON_JUGAR = {870, 56, 340, 92};

    private static final int[] PALETA = {
            0x2E86DE, 0xEE5253, 0x10AC84, 0xFF9F43, 0x8E44AD, 0xFF6B9D, 0xF5C518, 0x00B8D4, 0xECEFF1
    };
    private static final int MAX_NOMBRE = 16;
    private static final String[] DECORACION = {
            "objeto_moneda", "objeto_peluche", "objeto_zapato", "objeto_chaleco", "objeto_jarrito"
    };

    private static final Color DORADO = new Color(1f, 0.784f, 0.341f, 1f);
    private static final Color TERRACOTA = new Color(0.784f, 0.475f, 0.294f, 1f);
    private static final Color VERDE = new Color(0.263f, 0.627f, 0.278f, 1f);
    private static final Color TEXTO_SUAVE = new Color(0.75f, 0.69f, 0.61f, 1f);
    private static final Color ERROR = new Color(1f, 0.42f, 0.42f, 1f);

    private enum Foco { NOMBRE, FORMA, ACCESORIO }

    private final ExplosionParalelaJuego juego;
    private final Recursos recursos;
    private final Batch batch;
    private final Viewport viewport = new FitViewport(ANCHO, ALTO);
    private final Vector2 puntero = new Vector2();

    private Carrusel<Avatar.Forma> carruselForma;           // no son final: los pintores se refieren entre sí
    private Carrusel<Avatar.Accesorio> carruselAccesorio;
    private final StringBuilder nombre = new StringBuilder();

    private Foco foco = Foco.NOMBRE;
    private int colorRgb = PALETA[0];
    private float matiz = -1;               // -1 = se eligió un color de la paleta
    private boolean arrastrandoMatiz = false;
    private int botonPresionado = -1;       // 0 sorpréndeme, 1 jugar
    private float tiempo = 0;
    private float errorTiempo = 0;
    private float borradoRepetir = 0;

    public PantallaInicio(ExplosionParalelaJuego juego) {
        this.juego = juego;
        this.recursos = juego.recursos;
        this.batch = juego.batch;

        carruselForma = new Carrusel<>(recursos, Arrays.asList(Avatar.Forma.values()),
                (b, forma, x, y, w, h) -> pintarAvatar(b,
                        new Avatar(colorRgb, forma, carruselAccesorio.getSeleccionado()), x, y, w, h),
                CARRUSEL_X, CARRUSEL_FORMA_Y, CARRUSEL_ANCHO);
        carruselAccesorio = new Carrusel<>(recursos, Arrays.asList(Avatar.Accesorio.values()),
                (b, acc, x, y, w, h) -> pintarAvatar(b,
                        new Avatar(colorRgb, carruselForma.getSeleccionado(), acc), x, y, w, h),
                CARRUSEL_X, CARRUSEL_ACC_Y, CARRUSEL_ANCHO);
    }

    private void pintarAvatar(Batch b, Avatar avatar, float x, float y, float w, float h) {
        float tam = w * 0.62f;
        AvatarDibujo.dibujar(b, recursos, avatar, x + (w - tam) / 2f, y + h * 0.20f, tam, 1f, 1f, 0f);
    }

    private Avatar avatarActual() {
        return new Avatar(colorRgb, carruselForma.getSeleccionado(), carruselAccesorio.getSeleccionado());
    }

    // ------------------------------------------------------------- ciclo

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new Entrada());
    }

    @Override
    public void resize(int ancho, int alto) {
        viewport.update(ancho, alto, true);
    }

    @Override
    public void render(float dt) {
        tiempo += dt;
        if (errorTiempo > 0) errorTiempo -= dt;
        carruselForma.actualizar(dt);
        carruselAccesorio.actualizar(dt);
        repetirBorrado(dt);

        puntero.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(puntero);
        carruselForma.mover(puntero.x, puntero.y);
        carruselAccesorio.mover(puntero.x, puntero.y);

        ScreenUtils.clear(0.14f, 0.10f, 0.07f, 1f);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        dibujarFondo();
        dibujarTitulo();
        dibujarEscenario();
        dibujarCampoNombre();
        dibujarColores();
        recursos.texto(batch, 22, "FORMA", CARRUSEL_X - CARRUSEL_ANCHO / 2f + 50, 608, Recursos.IZQ, DORADO);
        carruselForma.dibujar(batch, foco == Foco.FORMA);
        recursos.texto(batch, 22, "ACCESORIO", CARRUSEL_X - CARRUSEL_ANCHO / 2f + 50, 400, Recursos.IZQ, DORADO);
        carruselAccesorio.dibujar(batch, foco == Foco.ACCESORIO);
        dibujarBoton(BOTON_SORPRESA, "SORPRÉNDEME", 30, TERRACOTA, 0);
        dibujarBoton(BOTON_JUGAR, "¡JUGAR!", 46, VERDE, 1);
        recursos.texto(batch, 18, "TAB: cambiar de campo   Flechas izq./der.: girar   ENTER: jugar",
                ANCHO / 2f, 26, Recursos.CENTRO, TEXTO_SUAVE);
        batch.end();
    }

    // ------------------------------------------------------------- dibujo

    private void dibujarFondo() {
        recursos.cubrir(batch, recursos.region("fondo_inicio"), 0, 0, ANCHO, ALTO);
        // objetos flotando suavemente por el fondo
        for (int i = 0; i < 12; i++) {
            TextureRegion r = recursos.region(DECORACION[i % DECORACION.length]);
            if (r == null) continue;
            float tam = 70 + (i * 37) % 50;
            float x = 40 + (i * 211) % 1180;
            float y = 60 + (i * 127) % 600 + MathUtils.sin(tiempo * 0.5f + i) * 16;
            float giro = MathUtils.sin(tiempo * 0.4f + i * 1.7f) * 14;
            batch.setColor(1f, 1f, 1f, 0.12f);
            batch.draw(r, x, y, tam / 2f, tam / 2f, tam, tam, 1f, 1f, giro);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void dibujarTitulo() {
        recursos.texto(batch, 68, "EXPLOSIÓN PARALELA", ANCHO / 2f, 716, Recursos.CENTRO, DORADO);
        recursos.texto(batch, 24, "Artesanías del Estado de México  ·  Elige a tu personaje",
                ANCHO / 2f, 648, Recursos.CENTRO, TEXTO_SUAVE);
    }

    private void dibujarEscenario() {
        float x = ESCENARIO[0], y = ESCENARIO[1], w = ESCENARIO[2], h = ESCENARIO[3];
        batch.setColor(0.2f, 0.15f, 0.11f, 1f);
        recursos.panel(batch, x - 6, y - 6, w + 12, h + 12);
        batch.setColor(1f, 1f, 1f, 1f);
        TextureRegion piso = recursos.region("escenario");
        if (piso != null) batch.draw(piso, x, y, w, h);
        batch.setColor(DORADO);
        recursos.panelBorde(batch, x - 6, y - 6, w + 12, h + 12);
        batch.setColor(1f, 1f, 1f, 1f);

        float flota = MathUtils.sin(tiempo * 3.2f) * 7f;
        TextureRegion sombra = recursos.region("sombra");
        if (sombra != null) {
            float s = 1f - flota * 0.012f;
            batch.setColor(1f, 1f, 1f, 0.9f);
            batch.draw(sombra, x + w / 2f - 95 * s, y + 52 - 18 * s, 190 * s, 56 * s);
            batch.setColor(1f, 1f, 1f, 1f);
        }
        float tam = 178;
        AvatarDibujo.dibujar(batch, recursos, avatarActual(), x + (w - tam) / 2f, y + 70 + flota, tam, 1f, 1f, 0f);
        if (nombre.length() > 0) {
            recursos.texto(batch, 32, nombre.toString(), x + w / 2f, y + 46, Recursos.CENTRO, Color.WHITE);
        }
    }

    private void dibujarCampoNombre() {
        float sacudida = errorTiempo > 0 ? MathUtils.sin(errorTiempo * 60f) * 7f * Math.min(1f, errorTiempo) : 0f;
        float x = CAMPO_NOMBRE[0] + sacudida, y = CAMPO_NOMBRE[1], w = CAMPO_NOMBRE[2], h = CAMPO_NOMBRE[3];
        batch.setColor(0.23f, 0.18f, 0.13f, 1f);
        recursos.panel(batch, x, y, w, h);
        if (errorTiempo > 0) batch.setColor(ERROR);
        else if (foco == Foco.NOMBRE) batch.setColor(DORADO);
        else batch.setColor(0.478f, 0.388f, 0.314f, 1f);
        recursos.panelBorde(batch, x, y, w, h);
        batch.setColor(1f, 1f, 1f, 1f);

        float centroY = y + h / 2f;
        float tope = centroY + recursos.fuente(34).getCapHeight() / 2f;
        if (nombre.length() == 0) {
            boolean falta = errorTiempo > 0;
            recursos.texto(batch, falta ? 28 : 30, falta ? "¡Escribe tu nombre!" : "Escribe tu nombre",
                    x + w / 2f, centroY + recursos.fuente(30).getCapHeight() / 2f,
                    Recursos.CENTRO, falta ? ERROR : TEXTO_SUAVE);
        } else {
            recursos.texto(batch, 34, nombre.toString(), x + w / 2f, tope, Recursos.CENTRO, Color.WHITE);
        }
        if (foco == Foco.NOMBRE && ((int) (tiempo * 2f)) % 2 == 0) {      // cursor parpadeando
            float ancho = nombre.length() == 0 ? 0 : recursos.anchoTexto(34, nombre.toString());
            recursos.rect(batch, x + w / 2f + ancho / 2f + 4, centroY - 18, 3, 36, DORADO);
        }
    }

    private void dibujarColores() {
        recursos.texto(batch, 22, "COLOR", 76, 160, Recursos.IZQ, DORADO);
        TextureRegion circulo = recursos.region("circulo");
        if (circulo != null) {
            for (int i = 0; i < PALETA.length; i++) {
                float cx = 70 + COLOR_RADIO + i * COLOR_PASO;
                boolean elegido = matiz < 0 && PALETA[i] == colorRgb;
                boolean encima = puntero.dst(cx, FILA_COLORES_Y) < COLOR_RADIO + 2;
                float r = COLOR_RADIO * (encima ? 1.12f : 1f);
                if (elegido) {
                    batch.setColor(DORADO);
                    batch.draw(circulo, cx - r - 5, FILA_COLORES_Y - r - 5, (r + 5) * 2, (r + 5) * 2);
                }
                batch.setColor(recursos.color(PALETA[i]));
                batch.draw(circulo, cx - r, FILA_COLORES_Y - r, r * 2, r * 2);
            }
            batch.setColor(1f, 1f, 1f, 1f);
        }
        TextureRegion barra = recursos.region("matiz");
        if (barra != null) {
            batch.draw(barra, BARRA_MATIZ[0], BARRA_MATIZ[1], BARRA_MATIZ[2], BARRA_MATIZ[3]);
            if (matiz >= 0 && circulo != null) {
                float mx = BARRA_MATIZ[0] + matiz * BARRA_MATIZ[2];
                float my = BARRA_MATIZ[1] + BARRA_MATIZ[3] / 2f;
                batch.setColor(Color.WHITE);
                batch.draw(circulo, mx - 17, my - 17, 34, 34);
                batch.setColor(recursos.color(colorRgb));
                batch.draw(circulo, mx - 12, my - 12, 24, 24);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }

    private void dibujarBoton(float[] r, String texto, int tam, Color color, int indice) {
        boolean encima = dentro(r, puntero.x, puntero.y);
        boolean presionado = botonPresionado == indice && encima;
        float baja = presionado ? 4f : 0f;
        float x = r[0], y = r[1] - baja, w = r[2], h = r[3];
        // base oscura (relieve) y cara del botón
        batch.setColor(color.r * 0.45f, color.g * 0.45f, color.b * 0.45f, 1f);
        recursos.panel(batch, x, y - 7, w, h);
        float brillo = encima ? 1.18f : 1f;
        batch.setColor(Math.min(1f, color.r * brillo), Math.min(1f, color.g * brillo), Math.min(1f, color.b * brillo), 1f);
        recursos.panel(batch, x, y, w, h);
        batch.setColor(1f, 1f, 1f, 0.35f);
        recursos.panelBorde(batch, x, y, w, h);
        batch.setColor(1f, 1f, 1f, 1f);
        recursos.texto(batch, tam, texto, x + w / 2f, y + h / 2f + recursos.fuente(tam).getCapHeight() / 2f,
                Recursos.CENTRO, Color.WHITE);
    }

    private static boolean dentro(float[] r, float x, float y) {
        return x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3];
    }

    // ------------------------------------------------------------ acciones

    private void jugar() {
        String texto = nombre.toString().trim();
        if (texto.isEmpty()) {
            errorTiempo = 1.4f;
            foco = Foco.NOMBRE;
            return;
        }
        juego.setScreen(new PantallaJuego(juego, texto, avatarActual()));
    }

    private void sorprender() {
        carruselForma.seleccionar(MathUtils.random(carruselForma.getCantidad() - 1));
        carruselAccesorio.seleccionar(MathUtils.random(carruselAccesorio.getCantidad() - 1));
        colorRgb = PALETA[MathUtils.random(PALETA.length - 1)];
        matiz = -1;
    }

    private void elegirMatiz(float x) {
        matiz = MathUtils.clamp((x - BARRA_MATIZ[0]) / BARRA_MATIZ[2], 0f, 1f);
        colorRgb = Color.rgb888(new Color().fromHsv(matiz * 360f, 0.82f, 1f));
    }

    private void repetirBorrado(float dt) {
        if (foco == Foco.NOMBRE && Gdx.input.isKeyPressed(Input.Keys.BACKSPACE)) {
            borradoRepetir += dt;
            if (borradoRepetir > 0.45f && nombre.length() > 0) {
                nombre.setLength(nombre.length() - 1);
                borradoRepetir = 0.40f;           // después de la pausa inicial, borra rápido
            }
        } else {
            borradoRepetir = 0;
        }
    }

    // ------------------------------------------------------------- entrada

    private class Entrada extends InputAdapter {
        private final Vector2 toque = new Vector2();

        private Vector2 mundo(int pantallaX, int pantallaY) {
            toque.set(pantallaX, pantallaY);
            return viewport.unproject(toque);
        }

        @Override
        public boolean keyDown(int tecla) {
            switch (tecla) {
                case Input.Keys.ENTER:
                    jugar();
                    return true;
                case Input.Keys.TAB:
                    foco = Foco.values()[(foco.ordinal() + 1) % Foco.values().length];
                    return true;
                case Input.Keys.BACKSPACE:
                    if (foco == Foco.NOMBRE && nombre.length() > 0) nombre.setLength(nombre.length() - 1);
                    return true;
                case Input.Keys.LEFT:
                    if (foco == Foco.FORMA) carruselForma.anterior();
                    if (foco == Foco.ACCESORIO) carruselAccesorio.anterior();
                    return true;
                case Input.Keys.RIGHT:
                    if (foco == Foco.FORMA) carruselForma.siguiente();
                    if (foco == Foco.ACCESORIO) carruselAccesorio.siguiente();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean keyTyped(char c) {
            if (foco != Foco.NOMBRE || c < 32 || c == 127 || nombre.length() >= MAX_NOMBRE) return false;
            if (!recursos.fuente(34).getData().hasGlyph(c)) return false;     // letras que la fuente no tiene
            nombre.append(c);
            errorTiempo = 0;
            return true;
        }

        @Override
        public boolean touchDown(int x, int y, int puntero, int boton) {
            Vector2 m = mundo(x, y);
            if (dentro(CAMPO_NOMBRE, m.x, m.y) || dentro(ESCENARIO, m.x, m.y)) {
                foco = Foco.NOMBRE;
            } else if (carruselForma.tocar(m.x, m.y)) {
                foco = Foco.FORMA;
            } else if (carruselAccesorio.tocar(m.x, m.y)) {
                foco = Foco.ACCESORIO;
            } else if (dentro(BARRA_MATIZ, m.x, m.y)) {
                arrastrandoMatiz = true;
                elegirMatiz(m.x);
            } else if (dentro(BOTON_SORPRESA, m.x, m.y)) {
                botonPresionado = 0;
            } else if (dentro(BOTON_JUGAR, m.x, m.y)) {
                botonPresionado = 1;
            } else {
                for (int i = 0; i < PALETA.length; i++) {
                    if (m.dst(70 + COLOR_RADIO + i * COLOR_PASO, FILA_COLORES_Y) <= COLOR_RADIO + 3) {
                        colorRgb = PALETA[i];
                        matiz = -1;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean touchDragged(int x, int y, int puntero) {
            if (arrastrandoMatiz) elegirMatiz(mundo(x, y).x);
            return true;
        }

        @Override
        public boolean touchUp(int x, int y, int puntero, int boton) {
            Vector2 m = mundo(x, y);
            arrastrandoMatiz = false;
            if (botonPresionado == 0 && dentro(BOTON_SORPRESA, m.x, m.y)) sorprender();
            if (botonPresionado == 1 && dentro(BOTON_JUGAR, m.x, m.y)) jugar();
            botonPresionado = -1;
            return true;
        }

        @Override
        public boolean scrolled(float cantidadX, float cantidadY) {
            Vector2 m = mundo(Gdx.input.getX(), Gdx.input.getY());
            Carrusel<?> c = carruselForma.contiene(m.x, m.y) ? carruselForma
                    : (carruselAccesorio.contiene(m.x, m.y) ? carruselAccesorio : null);
            if (c == null) return false;
            if (cantidadY > 0) c.siguiente(); else c.anterior();
            return true;
        }
    }
}
