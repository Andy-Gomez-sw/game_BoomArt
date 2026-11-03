package explosionparalela.pantallas;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import explosionparalela.ExplosionParalelaJuego;
import explosionparalela.audio.Sonido;
import explosionparalela.core.Juego;
import explosionparalela.graficos.AvatarDibujo;
import explosionparalela.graficos.Particulas;
import explosionparalela.graficos.Recursos;
import explosionparalela.levels.Nivel;
import explosionparalela.model.Avatar;
import explosionparalela.model.Bomba;
import explosionparalela.model.Enemigo;
import explosionparalela.model.Jugador;
import explosionparalela.model.Mapa;
import explosionparalela.model.TipoCelda;
import explosionparalela.model.TipoObjeto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pantalla de la partida. La lógica (mapa, hilos, reglas) vive en core.Juego; aquí solo
 * se DIBUJA lo que esa lógica va decidiendo y se leen el teclado y el mouse.
 *
 * El juego trabaja por casillas; para que se vea fluido, cada personaje se dibuja en una
 * posición "visual" que se desliza suavemente hacia su casilla.
 */
public class PantallaJuego extends ScreenAdapter {
    static final float ANCHO = 1280;
    static final float ALTO = 720;
    static final float HUD_ALTO = 72;
    static final float MAPA_X = 20;
    static final float MAPA_Y = 40;
    static final float MAPA_ANCHO = ANCHO - 40;
    static final float MAPA_ALTO = ALTO - HUD_ALTO - 40 - 12;

    private static final Color DORADO = new Color(1f, 0.784f, 0.341f, 1f);
    private static final Color TEXTO_SUAVE = new Color(0.75f, 0.69f, 0.61f, 1f);
    private static final Color ROJO = new Color(1f, 0.38f, 0.38f, 1f);
    private static final Color VERDE_CLARO = new Color(0.45f, 1f, 0.55f, 1f);

    /** Texto que sube y se desvanece (por ejemplo "+1 vida"). */
    private static class Flotante {
        String texto;
        float x, y, vida;
        Color color;
    }

    private final ExplosionParalelaJuego app;
    private final Recursos recursos;
    private final Batch batch;
    private final Viewport viewport = new FitViewport(ANCHO, ALTO);
    private final String nombre;
    private final Avatar avatar;
    private final Particulas particulas = new Particulas();

    private Juego juego;
    private volatile String mensajeFinal = null;

    // estado visual
    private final Map<Object, float[]> visuales = new IdentityHashMap<>();
    private final Set<Bomba> explotadas = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Enemigo> muertos = Collections.newSetFromMap(new IdentityHashMap<>());
    private Nivel nivelVisto;
    private int vidasVistas;
    private int monedasVistas;
    private int objetosVistos;
    private float tiempo = 0;
    private float temblor = 0;
    private float destelloRojo = 0;
    private boolean inventarioAbierto = false;
    private boolean pausaPorInventario = false;
    private final List<Flotante> flotantes = new ArrayList<>();
    private float relojDestello = 0;
    private int segundosVistos = 0;

    // geometría del mapa actual (se recalcula cada cuadro)
    private float casilla, origenX, origenY;
    private int filas, columnas;

    public PantallaJuego(ExplosionParalelaJuego app, String nombre, Avatar avatar) {
        this.app = app;
        this.recursos = app.recursos;
        this.batch = app.batch;
        this.nombre = nombre;
        this.avatar = avatar;
        iniciarPartida();
    }

    private void iniciarPartida() {
        if (juego != null) juego.detener();
        mensajeFinal = null;
        inventarioAbierto = false;
        pausaPorInventario = false;
        visuales.clear();
        explotadas.clear();
        muertos.clear();
        particulas.limpiar();
        flotantes.clear();
        relojDestello = 0;
        temblor = 0;
        destelloRojo = 0;

        juego = new Juego(nombre, avatar);
        juego.setEscucha((victoria, mensaje) -> mensajeFinal = mensaje);
        juego.iniciar();
        nivelVisto = juego.getNivel();
        vidasVistas = juego.getJugador().getVidas();
        monedasVistas = 0;
        objetosVistos = 0;
        segundosVistos = juego.getSegundosRestantes();
    }

    // ------------------------------------------------------------- ciclo

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new Entrada());
    }

    @Override
    public void hide() {
        if (juego != null) juego.detener();
    }

    @Override
    public void resize(int ancho, int alto) {
        viewport.update(ancho, alto, true);
    }

    @Override
    public void render(float dt) {
        dt = Math.min(dt, 0.05f);          // si hubo un tirón, no avanza de golpe
        tiempo += dt;
        leerMovimiento();
        calcularGeometria();
        detectarEventos();
        actualizarVisuales(dt);
        particulas.actualizar(dt);
        actualizarFlotantes(dt);
        temblor = Math.max(0f, temblor - 70f * dt);
        destelloRojo = Math.max(0f, destelloRojo - 1.6f * dt);

        ScreenUtils.clear(0.08f, 0.06f, 0.04f, 1f);
        viewport.apply();
        Camera camara = viewport.getCamera();
        batch.begin();

        // fondo y barra superior (fijos)
        camara.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camara.update();
        batch.setProjectionMatrix(camara.combined);
        dibujarFondo();

        // el mapa tiembla un poco con las explosiones y los golpes
        float tx = temblor > 0 ? MathUtils.random(-1f, 1f) * temblor * 0.5f : 0f;
        float ty = temblor > 0 ? MathUtils.random(-1f, 1f) * temblor * 0.5f : 0f;
        camara.position.set(ANCHO / 2f + tx, ALTO / 2f + ty, 0f);
        camara.update();
        batch.setProjectionMatrix(camara.combined);
        dibujarMapa();
        dibujarBombasYFuego();
        dibujarEnemigos();
        dibujarJugador();
        particulas.dibujar(batch, recursos.region("chispa"));
        dibujarFlotantes();

        camara.position.set(ANCHO / 2f, ALTO / 2f, 0f);
        camara.update();
        batch.setProjectionMatrix(camara.combined);
        if (destelloRojo > 0) {
            recursos.rect(batch, 0, 0, ANCHO, ALTO, new Color(1f, 0.1f, 0.1f, 0.35f * destelloRojo));
        }
        dibujarHud();
        dibujarSuperposiciones();
        batch.end();
    }

    // -------------------------------------------------------- entrada y estado

    private void leerMovimiento() {
        if (juego.getEstado() != Juego.Estado.JUGANDO || inventarioAbierto) return;
        Input in = Gdx.input;
        // Juego limita la velocidad del jugador, así que se puede llamar cada cuadro
        if (in.isKeyPressed(Input.Keys.LEFT) || in.isKeyPressed(Input.Keys.A)) juego.moverJugador(0, -1);
        else if (in.isKeyPressed(Input.Keys.RIGHT) || in.isKeyPressed(Input.Keys.D)) juego.moverJugador(0, 1);
        else if (in.isKeyPressed(Input.Keys.UP) || in.isKeyPressed(Input.Keys.W)) juego.moverJugador(-1, 0);
        else if (in.isKeyPressed(Input.Keys.DOWN) || in.isKeyPressed(Input.Keys.S)) juego.moverJugador(1, 0);
    }

    private void alternarInventario() {
        if (!inventarioAbierto) {
            inventarioAbierto = true;
            if (juego.getEstado() == Juego.Estado.JUGANDO) {
                juego.alternarPausa();
                pausaPorInventario = true;
            }
        } else {
            inventarioAbierto = false;
            if (pausaPorInventario && juego.getEstado() == Juego.Estado.PAUSADO) juego.alternarPausa();
            pausaPorInventario = false;
        }
    }

    private class Entrada extends InputAdapter {
        @Override
        public boolean keyDown(int tecla) {
            switch (tecla) {
                case Input.Keys.SPACE:
                    if (!inventarioAbierto) juego.colocarBomba();
                    return true;
                case Input.Keys.P:
                    if (!inventarioAbierto) juego.alternarPausa();
                    return true;
                case Input.Keys.ENTER:
                    juego.comenzarNivel();
                    return true;
                case Input.Keys.I:
                    alternarInventario();
                    return true;
                case Input.Keys.M:
                    Sonido.alternarSilencio();
                    return true;
                case Input.Keys.R:
                    iniciarPartida();
                    return true;
                case Input.Keys.ESCAPE:
                    if (inventarioAbierto) alternarInventario();
                    else app.setScreen(new PantallaInicio(app));
                    return true;
                default:
                    return false;
            }
        }
    }

    // ---------------------------------------------------- geometría y eventos

    private void calcularGeometria() {
        Mapa mapa = juego.getMapa();
        filas = mapa.getFilas();
        columnas = mapa.getColumnas();
        casilla = Math.min(MAPA_ANCHO / columnas, MAPA_ALTO / filas);
        origenX = MAPA_X + (MAPA_ANCHO - columnas * casilla) / 2f;
        origenY = MAPA_Y + (MAPA_ALTO - filas * casilla) / 2f;
    }

    private float mundoX(float columna) { return origenX + columna * casilla; }

    private float mundoY(float fila) { return origenY + (filas - 1 - fila) * casilla; }

    /** Compara el estado del juego con el cuadro anterior para lanzar efectos. */
    private void detectarEventos() {
        Nivel nivel = juego.getNivel();
        if (nivel != nivelVisto) {                 // empezó otro nivel
            nivelVisto = nivel;
            visuales.clear();
            explotadas.clear();
            muertos.clear();
            particulas.limpiar();
            flotantes.clear();
            objetosVistos = 0;
        }
        Jugador j = juego.getJugador();
        float[] pj = visual(j, j.getColumna(), j.getFila());

        for (Bomba b : juego.getBombas()) {
            if (b.estaEnLlamas() && explotadas.add(b)) {
                particulas.explosion(mundoX(b.getColumna()) + casilla / 2f, mundoY(b.getFila()) + casilla / 2f, casilla);
                for (int[] celda : b.getFuego()) {
                    particulas.destello(mundoX(celda[1]) + casilla / 2f, mundoY(celda[0]) + casilla / 2f,
                            casilla, 1f, 0.55f, 0.1f, 3);
                }
                temblor = Math.max(temblor, 16f);
            }
        }
        for (Enemigo e : juego.getEnemigos()) {
            if (!e.estaVivo() && muertos.add(e)) {
                float[] v = visual(e, e.getColumna(), e.getFila());
                particulas.nube(mundoX(v[0]) + casilla / 2f, mundoY(v[1]) + casilla / 2f, casilla);
            }
        }
        int vidas = j.getVidas();
        if (vidas < vidasVistas) {
            temblor = Math.max(temblor, 22f);
            destelloRojo = 1f;
        }
        vidasVistas = vidas;

        int monedas = juego.getInventario().getMonedas();
        if (monedas > monedasVistas) {
            particulas.destello(mundoX(pj[0]) + casilla / 2f, mundoY(pj[1]) + casilla / 2f, casilla, 1f, 0.85f, 0.2f, 14);
        }
        monedasVistas = monedas;
        int objetos = juego.getObjetosEncontrados();
        if (objetos > objetosVistos) {
            particulas.destello(mundoX(pj[0]) + casilla / 2f, mundoY(pj[1]) + casilla / 2f, casilla, 1f, 0.6f, 0.85f, 34);
        }
        objetosVistos = objetos;

        // ayudas recogidas: destello del color de la ayuda y texto flotante
        Juego.Aviso aviso;
        while ((aviso = juego.sacarAviso()) != null) {
            Color c = colorAyuda(aviso.tipo);
            float px = mundoX(pj[0]) + casilla / 2f;
            float py = mundoY(pj[1]) + casilla / 2f;
            particulas.destello(px, py, casilla, c.r, c.g, c.b, 30);
            Flotante f = new Flotante();
            f.texto = aviso.texto;
            f.x = px;
            f.y = py + casilla;
            f.vida = 1.5f;
            f.color = c;
            flotantes.add(f);
        }
        int segundos = juego.getSegundosRestantes();
        if (segundos > segundosVistos + 1) relojDestello = 1f;     // subió el tiempo: se resalta el reloj
        segundosVistos = segundos;
    }

    private static Color colorAyuda(TipoObjeto tipo) {
        switch (tipo) {
            case CORAZON: return new Color(1f, 0.45f, 0.55f, 1f);
            case RELOJ: return new Color(0.45f, 0.85f, 1f, 1f);
            case ESCUDO: return new Color(0.55f, 0.78f, 1f, 1f);
            default: return new Color(1f, 0.68f, 0.2f, 1f);        // fuego extra
        }
    }

    private void actualizarFlotantes(float dt) {
        relojDestello = Math.max(0f, relojDestello - dt * 1.2f);
        for (int i = flotantes.size() - 1; i >= 0; i--) {
            Flotante f = flotantes.get(i);
            f.vida -= dt;
            f.y += casilla * 0.9f * dt;
            if (f.vida <= 0) flotantes.remove(i);
        }
    }

    private void dibujarFlotantes() {
        for (Flotante f : flotantes) {
            float alfa = Math.min(1f, f.vida / 0.6f);
            recursos.texto(batch, 32, f.texto, f.x, f.y, Recursos.CENTRO,
                    new Color(f.color.r, f.color.g, f.color.b, alfa));
        }
    }

    /** Resplandor que hace notar los ítems de ayuda en el suelo. */
    private void dibujarAura(float x, float y, TipoObjeto objeto) {
        TextureRegion chispa = recursos.region("chispa");
        if (chispa == null) return;
        Color c = colorAyuda(objeto);
        float pulso = 0.75f + 0.25f * MathUtils.sin(tiempo * 5f + x);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(c.r, c.g, c.b, 0.65f * pulso);
        batch.draw(chispa, x - casilla * 0.45f, y - casilla * 0.4f, casilla * 1.9f, casilla * 1.9f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private float[] visual(Object o, float columna, float fila) {
        float[] v = visuales.get(o);
        if (v == null) {
            v = new float[]{columna, fila};
            visuales.put(o, v);
        }
        return v;
    }

    /** Desliza cada personaje hacia su casilla lógica. */
    private void actualizarVisuales(float dt) {
        float k = 1f - (float) Math.exp(-17f * dt);
        Jugador j = juego.getJugador();
        deslizar(visual(j, j.getColumna(), j.getFila()), j.getColumna(), j.getFila(), k);
        for (Enemigo e : juego.getEnemigos()) {
            if (e.estaVivo()) deslizar(visual(e, e.getColumna(), e.getFila()), e.getColumna(), e.getFila(), k);
        }
    }

    private static void deslizar(float[] v, float columna, float fila, float k) {
        v[0] += (columna - v[0]) * k;
        v[1] += (fila - v[1]) * k;
        if (Math.abs(columna - v[0]) < 0.002f) v[0] = columna;
        if (Math.abs(fila - v[1]) < 0.002f) v[1] = fila;
    }

    // -------------------------------------------------------------- dibujo

    private void dibujarFondo() {
        Nivel nivel = juego.getNivel();
        TextureRegion fondo = recursos.region("nivel" + nivel.getNumero() + "_fondo");
        recursos.cubrir(batch, fondo, 0, 0, ANCHO, ALTO);
        recursos.rect(batch, 0, 0, ANCHO, ALTO, new Color(0.06f, 0.04f, 0.02f, fondo != null ? 0.66f : 0.9f));
    }

    private void dibujarMapa() {
        Mapa mapa = juego.getMapa();
        String pre = "nivel" + juego.getNivel().getNumero() + "_";
        TextureRegion suelo = recursos.region(pre + "suelo", "suelo");
        TextureRegion fijo = recursos.region(pre + "muro_fijo", "muro_fijo");
        TextureRegion rompible = recursos.region(pre + "muro_rompible", "muro_rompible");
        TextureRegion puerta = recursos.region("puerta");
        TextureRegion trampa = recursos.region("trampa");
        TextureRegion sombra = recursos.region("sombra");

        // 1) piso en todas las casillas (misma imagen: no hay cambios de textura)
        if (suelo != null) {
            for (int f = 0; f < filas; f++) {
                for (int c = 0; c < columnas; c++) {
                    if (mapa.getTipo(f, c) != TipoCelda.INDESTRUCTIBLE) {
                        batch.draw(suelo, mundoX(c), mundoY(f), casilla, casilla);
                    }
                }
            }
        }
        // 2) puertas y trampas sobre el piso
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                TipoCelda tipo = mapa.getTipo(f, c);
                if (tipo == TipoCelda.PUERTA && puerta != null) {
                    batch.draw(puerta, mundoX(c), mundoY(f), casilla, casilla);
                } else if (tipo == TipoCelda.TRAMPA && trampa != null) {
                    float pulso = 0.94f + 0.06f * MathUtils.sin(tiempo * 5f + c + f);
                    float t = casilla * pulso;
                    batch.draw(trampa, mundoX(c) + (casilla - t) / 2f, mundoY(f) + (casilla - t) / 2f, t, t);
                }
            }
        }
        // 3) objetos en el suelo (flotan suavemente)
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                TipoObjeto objeto = mapa.getObjetoSuelo(f, c);
                if (objeto == null) continue;
                TextureRegion r = recursos.region("objeto_" + objeto.name().toLowerCase());
                float flota = MathUtils.sin(tiempo * 4f + c * 1.3f + f) * casilla * 0.05f;
                if (objeto.esAyuda()) dibujarAura(mundoX(c), mundoY(f), objeto);
                if (sombra != null) {
                    batch.setColor(1f, 1f, 1f, 0.55f);
                    batch.draw(sombra, mundoX(c) + casilla * 0.15f, mundoY(f) + casilla * 0.02f, casilla * 0.7f, casilla * 0.25f);
                    batch.setColor(1f, 1f, 1f, 1f);
                }
                if (r != null) {
                    float t = casilla * 0.82f;
                    batch.draw(r, mundoX(c) + (casilla - t) / 2f, mundoY(f) + casilla * 0.1f + flota, t, t);
                }
            }
        }
        // 4) muros (después, para que tapen lo que esté debajo)
        for (int pasada = 0; pasada < 2; pasada++) {
            TextureRegion r = pasada == 0 ? fijo : rompible;
            TipoCelda buscado = pasada == 0 ? TipoCelda.INDESTRUCTIBLE : TipoCelda.DESTRUCTIBLE;
            if (r == null) continue;
            for (int f = 0; f < filas; f++) {
                for (int c = 0; c < columnas; c++) {
                    if (mapa.getTipo(f, c) == buscado) batch.draw(r, mundoX(c), mundoY(f), casilla, casilla);
                }
            }
        }
    }

    private void dibujarBombasYFuego() {
        TextureRegion jarrito = recursos.region("jarrito");
        TextureRegion fuego = recursos.region("fuego");
        TextureRegion chispa = recursos.region("chispa");
        TextureRegion sombra = recursos.region("sombra");
        List<Bomba> bombas = juego.getBombas();
        for (Bomba b : bombas) {
            float x = mundoX(b.getColumna());
            float y = mundoY(b.getFila());
            if (b.estaEnLlamas()) {
                if (fuego == null) continue;
                for (int[] celda : b.getFuego()) {
                    float fx = mundoX(celda[1]);
                    float fy = mundoY(celda[0]);
                    float parpadeo = 0.92f + 0.12f * MathUtils.sin(tiempo * 40f + celda[0] * 3 + celda[1]);
                    float t = casilla * parpadeo;
                    batch.draw(fuego, fx + (casilla - t) / 2f, fy + (casilla - t) / 2f, t, t);
                    if (chispa != null) {
                        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                        batch.setColor(1f, 0.6f, 0.2f, 0.55f);
                        batch.draw(chispa, fx - casilla * 0.4f, fy - casilla * 0.4f, casilla * 1.8f, casilla * 1.8f);
                        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                        batch.setColor(1f, 1f, 1f, 1f);
                    }
                }
            } else if (jarrito != null) {
                float pulso = 1f + 0.09f * MathUtils.sin(tiempo * 11f + b.getColumna());
                float t = casilla * 0.86f * pulso;
                if (sombra != null) {
                    batch.setColor(1f, 1f, 1f, 0.6f);
                    batch.draw(sombra, x + casilla * 0.12f, y + casilla * 0.0f, casilla * 0.76f, casilla * 0.26f);
                    batch.setColor(1f, 1f, 1f, 1f);
                }
                batch.draw(jarrito, x + (casilla - t) / 2f, y + casilla * 0.07f, t, t);
                if (chispa != null && ((int) (tiempo * 8f)) % 2 == 0) {      // chispa de la mecha
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
                    batch.setColor(1f, 0.8f, 0.2f, 0.95f);
                    batch.draw(chispa, x + casilla * 0.42f, y + casilla * 0.82f, casilla * 0.3f, casilla * 0.3f);
                    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    batch.setColor(1f, 1f, 1f, 1f);
                }
            }
        }
    }

    private void dibujarEnemigos() {
        TextureRegion alerta = recursos.region("alerta");
        TextureRegion sombra = recursos.region("sombra");
        for (Enemigo e : juego.getEnemigos()) {
            if (!e.estaVivo()) continue;
            float[] v = visual(e, e.getColumna(), e.getFila());
            float x = mundoX(v[0]);
            float y = mundoY(v[1]);
            TextureRegion r = recursos.region("enemigo_" + e.getTipo().name().toLowerCase());
            float rapido = e.getTipo() == Enemigo.Tipo.RAPIDO ? 1.6f : 1f;
            float rebote = Math.abs(MathUtils.sin(tiempo * 7f * rapido + e.getColumna())) * casilla * 0.06f;
            if (sombra != null) {
                batch.setColor(1f, 1f, 1f, 0.55f);
                batch.draw(sombra, x + casilla * 0.1f, y + casilla * 0.0f, casilla * 0.8f, casilla * 0.28f);
                batch.setColor(1f, 1f, 1f, 1f);
            }
            if (r != null) {
                float t = casilla * 0.92f;
                batch.draw(r, x + (casilla - t) / 2f, y + casilla * 0.06f + rebote, t, t);
            }
            if (e.estaPersiguiendo() && alerta != null) {                     // "!" sobre la cabeza
                float salto = Math.abs(MathUtils.sin(tiempo * 9f)) * casilla * 0.12f;
                float t = casilla * 0.55f;
                batch.draw(alerta, x + (casilla - t) / 2f, y + casilla * 0.95f + salto, t, t);
            }
        }
    }

    private void dibujarJugador() {
        Jugador j = juego.getJugador();
        if (j.estaInvulnerable() && ((int) (tiempo * 12f)) % 2 == 0) return;       // parpadea tras un golpe
        float[] v = visual(j, j.getColumna(), j.getFila());
        float x = mundoX(v[0]);
        float y = mundoY(v[1]);
        float resta = Math.abs(j.getColumna() - v[0]) + Math.abs(j.getFila() - v[1]);
        boolean moviendo = resta > 0.05f;
        float respiro = 1f + 0.025f * MathUtils.sin(tiempo * 5f);
        float sx = moviendo ? 1.08f : 1f / respiro;
        float sy = moviendo ? 0.93f : respiro;
        float giro = moviendo ? MathUtils.clamp((j.getColumna() - v[0]) * -14f, -12f, 12f) : 0f;
        TextureRegion sombra = recursos.region("sombra");
        if (sombra != null) {
            batch.setColor(1f, 1f, 1f, 0.6f);
            batch.draw(sombra, x + casilla * 0.08f, y - casilla * 0.02f, casilla * 0.84f, casilla * 0.3f);
            batch.setColor(1f, 1f, 1f, 1f);
        }
        AvatarDibujo.dibujar(batch, recursos, j.getAvatar(), x, y + casilla * 0.04f, casilla, sx, sy, giro);
        if (j.tieneEscudo()) {                                          // burbuja protectora
            TextureRegion burbuja = recursos.region("escudo");
            boolean porAcabar = j.escudoRestanteMs() < 1500;
            if (burbuja != null && !(porAcabar && ((int) (tiempo * 10f)) % 2 == 0)) {
                float t = casilla * (1.75f + 0.06f * MathUtils.sin(tiempo * 6f));
                batch.setColor(1f, 1f, 1f, 0.9f);
                batch.draw(burbuja, x + (casilla - t) / 2f, y + casilla * 0.55f - t / 2f, t, t);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private void dibujarHud() {
        Nivel nivel = juego.getNivel();
        Jugador j = juego.getJugador();
        recursos.rect(batch, 0, ALTO - HUD_ALTO, ANCHO, HUD_ALTO, new Color(0.10f, 0.07f, 0.04f, 0.92f));
        recursos.rect(batch, 0, ALTO - HUD_ALTO - 3, ANCHO, 3, new Color(0.784f, 0.475f, 0.294f, 1f));

        recursos.texto(batch, 30, "Nivel " + nivel.getNumero() + ": " + nivel.getMunicipio(), 24, ALTO - 8, Recursos.IZQ, DORADO);
        recursos.texto(batch, 22, j.getNombre(), 26, ALTO - 44, Recursos.IZQ, TEXTO_SUAVE);

        // vidas
        TextureRegion corazon = recursos.region("corazon");
        if (corazon != null) {
            for (int i = 0; i < 3; i++) {
                boolean viva = i < j.getVidas();
                float latido = viva ? 1f + 0.05f * MathUtils.sin(tiempo * 6f + i) : 1f;
                float t = 44 * latido;
                batch.setColor(1f, 1f, 1f, viva ? 1f : 0.22f);
                batch.draw(corazon, 410 + i * 52 + (44 - t) / 2f, ALTO - 62 + (44 - t) / 2f, t, t);
            }
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // tiempo
        int s = Math.max(0, juego.getSegundosRestantes());
        String reloj = String.format("%02d:%02d", s / 60, s % 60);
        boolean urgente = s <= 30 && juego.getEstado() != Juego.Estado.TRANSICION;
        float latido = urgente ? 1f + 0.06f * MathUtils.sin(tiempo * 12f) : 1f;
        if (relojDestello > 0) latido = 1f + 0.18f * relojDestello;
        recursos.fuente(46).getData().setScale(latido);
        recursos.texto(batch, 46, reloj, ANCHO / 2f + 100, ALTO - 6, Recursos.CENTRO,
                relojDestello > 0 ? VERDE_CLARO : (urgente ? ROJO : Color.WHITE));
        recursos.fuente(46).getData().setScale(1f);

        // marcadores
        recursos.texto(batch, 24, "Puntos " + juego.getInventario().getPuntuacion()
                + "    Monedas " + juego.getInventario().getMonedas(), ANCHO - 24, ALTO - 10, Recursos.DER, Color.WHITE);
        recursos.texto(batch, 24, "Enemigos " + juego.enemigosVivos()
                + "    Objetos " + juego.getObjetosEncontrados() + "/" + nivel.getRequeridos().size(),
                ANCHO - 24, ALTO - 42, Recursos.DER, Color.WHITE);

        float chipY = ALTO - HUD_ALTO - 52;
        if (j.tieneEscudo()) {
            chip(24, chipY, "ESCUDO " + (int) Math.ceil(j.escudoRestanteMs() / 1000f) + " s", colorAyuda(TipoObjeto.ESCUDO));
            chipY -= 46;
        }
        if (juego.getAlcanceExtra() > 0) {
            chip(24, chipY, "FUEGO EXTRA +" + juego.getAlcanceExtra(), colorAyuda(TipoObjeto.ALCANCE));
        }

        recursos.texto(batch, 17, "Flechas / WASD mover   ESPACIO jarrito   P pausa   I inventario   M sonido "
                + (Sonido.isSilenciado() ? "(apagado)" : "(activo)") + "   R reiniciar   ESC menú",
                ANCHO / 2f, 30, Recursos.CENTRO, TEXTO_SUAVE);
    }

    /** Etiqueta pequeña con el estado de una ayuda activa. */
    private void chip(float x, float y, String texto, Color color) {
        float w = recursos.anchoTexto(22, texto) + 34;
        batch.setColor(0.08f, 0.06f, 0.04f, 0.85f);
        recursos.panel(batch, x, y, w, 40);
        batch.setColor(color);
        recursos.panelBorde(batch, x, y, w, 40);
        batch.setColor(1f, 1f, 1f, 1f);
        recursos.texto(batch, 22, texto, x + w / 2f, y + 34, Recursos.CENTRO, color);
    }

    // --------------------------------------------------------- superposiciones

    private void dibujarSuperposiciones() {
        Juego.Estado estado = juego.getEstado();
        if (inventarioAbierto) {
            dibujarInventario();
            return;
        }
        if (estado == Juego.Estado.JUGANDO) return;

        if (estado == Juego.Estado.TRANSICION) {
            dibujarIntroNivel();
        } else if (estado == Juego.Estado.PAUSADO) {
            recursos.rect(batch, 0, 0, ANCHO, ALTO - HUD_ALTO, new Color(0f, 0f, 0f, 0.62f));
            recursos.texto(batch, 120, "PAUSA", ANCHO / 2f, 470, Recursos.CENTRO, DORADO);
            recursos.texto(batch, 30, "P: reanudar     I: inventario     R: reiniciar     ESC: menú",
                    ANCHO / 2f, 300, Recursos.CENTRO, Color.WHITE);
        } else {
            dibujarFin(estado == Juego.Estado.GANADO);
        }
    }

    private void dibujarIntroNivel() {
        Nivel nivel = juego.getNivel();
        TextureRegion fondo = recursos.region("nivel" + nivel.getNumero() + "_fondo");
        if (fondo != null) {                                  // el monumento se ve completo arriba
            recursos.cubrir(batch, fondo, 0, 0, ANCHO, ALTO - HUD_ALTO - 3);
            TextureRegion degradado = recursos.region("degradado");
            if (degradado != null) {
                batch.setColor(0.04f, 0.03f, 0.02f, 0.97f);
                batch.draw(degradado, 0, 0, ANCHO, 560);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        } else {
            recursos.rect(batch, 0, 0, ANCHO, ALTO - HUD_ALTO, new Color(0f, 0f, 0f, 0.7f));
        }
        String aviso = juego.getMensajeTransicion();
        if (aviso != null && !aviso.isEmpty()) {
            recursos.texto(batch, 34, aviso, ANCHO / 2f, 520, Recursos.CENTRO, DORADO);
        }
        recursos.texto(batch, 104, "NIVEL " + nivel.getNumero(), ANCHO / 2f, 480, Recursos.CENTRO, Color.WHITE);
        recursos.texto(batch, 40, nivel.getMunicipio() + "  -  " + nivel.getProducto(),
                ANCHO / 2f, 366, Recursos.CENTRO, DORADO);
        recursos.texto(batch, 28, nivel.getObjetivo(), ANCHO / 2f, 296, Recursos.CENTRO, Color.WHITE);
        recursos.texto(batch, 24, nivel.getCaracteristicas(), ANCHO / 2f, 252, Recursos.CENTRO, TEXTO_SUAVE);
        recursos.texto(batch, 22, "Ayudas escondidas: Corazón +1 vida  ·  Reloj +30 s  ·  Escudo  ·  Fuego extra",
                ANCHO / 2f, 224, Recursos.CENTRO, DORADO);
        int s = Math.max(0, juego.getSegundosRestantes());
        recursos.texto(batch, 26, String.format("Tiempo restante %02d:%02d     Vidas %d", s / 60, s % 60,
                juego.getJugador().getVidas()), ANCHO / 2f, 188, Recursos.CENTRO, Color.WHITE);
        float pulso = 1f + 0.05f * MathUtils.sin(tiempo * 5f);
        recursos.fuente(44).getData().setScale(pulso);
        recursos.texto(batch, 44, "ENTER: comenzar", ANCHO / 2f, 132, Recursos.CENTRO, Color.WHITE);
        recursos.fuente(44).getData().setScale(1f);
    }

    private void dibujarFin(boolean victoria) {
        recursos.rect(batch, 0, 0, ANCHO, ALTO - HUD_ALTO, new Color(0f, 0f, 0f, 0.72f));
        recursos.texto(batch, 112, victoria ? "¡GANASTE!" : "PERDISTE", ANCHO / 2f, 590,
                Recursos.CENTRO, victoria ? DORADO : ROJO);
        String mensaje = mensajeFinal;
        if (mensaje != null) recursos.texto(batch, 34, mensaje, ANCHO / 2f, 470, Recursos.CENTRO, Color.WHITE);
        recursos.texto(batch, 26, "Llegaste al nivel " + juego.getNivel().getNumero() + " de " + Nivel.TOTAL,
                ANCHO / 2f, 420, Recursos.CENTRO, TEXTO_SUAVE);
        float y = 360;
        for (String linea : juego.getInventario().resumen().split("\n")) {
            recursos.texto(batch, 28, linea, ANCHO / 2f, y, Recursos.CENTRO, Color.WHITE);
            y -= 38;
        }
        recursos.texto(batch, 34, "R: jugar de nuevo     ESC: menú", ANCHO / 2f, 120, Recursos.CENTRO, DORADO);
    }

    private void dibujarInventario() {
        recursos.rect(batch, 0, 0, ANCHO, ALTO, new Color(0f, 0f, 0f, 0.7f));
        float w = 780, h = 560, x = (ANCHO - w) / 2f, y = 60;
        batch.setColor(0.2f, 0.15f, 0.11f, 1f);
        recursos.panel(batch, x, y, w, h);
        batch.setColor(DORADO);
        recursos.panelBorde(batch, x, y, w, h);
        batch.setColor(1f, 1f, 1f, 1f);
        recursos.texto(batch, 54, "INVENTARIO", ANCHO / 2f, y + h - 18, Recursos.CENTRO, DORADO);

        float ty = y + h - 96;
        for (String linea : juego.getInventario().resumen().split("\n")) {
            recursos.texto(batch, 28, linea, x + 40, ty, Recursos.IZQ, Color.WHITE);
            ty -= 34;
        }
        ty -= 10;
        recursos.texto(batch, 24, "Historial", x + 40, ty, Recursos.IZQ, DORADO);
        ty -= 34;
        List<String> historial = juego.getInventario().getHistorial();
        if (historial.isEmpty()) {
            recursos.texto(batch, 22, "Aún no has recogido nada.", x + 40, ty, Recursos.IZQ, TEXTO_SUAVE);
        } else {
            int desde = Math.max(0, historial.size() - 8);          // las últimas entradas
            for (int i = desde; i < historial.size(); i++) {
                recursos.texto(batch, 21, historial.get(i), x + 40, ty, Recursos.IZQ, TEXTO_SUAVE);
                ty -= 28;
            }
        }
        recursos.texto(batch, 22, "I o ESC: cerrar", ANCHO / 2f, y + 44, Recursos.CENTRO, Color.WHITE);
    }
}
