package explosionparalela.graficos;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Efectos de partículas sencillos: chispas de explosión, destellos al recoger
 * objetos y humo. Las posiciones están en coordenadas del mundo (píxeles virtuales).
 */
public class Particulas {
    private static class Particula {
        float x, y, vx, vy, vida, total, tam, r, g, b, gravedad, freno;
        boolean aditiva;
    }

    private final Array<Particula> lista = new Array<>(false, 512);

    /** Explosión de jarrito: chispas naranjas y amarillas y un poco de humo. */
    public void explosion(float cx, float cy, float casilla) {
        for (int i = 0; i < 46; i++) {
            float angulo = MathUtils.random(MathUtils.PI2);
            float vel = MathUtils.random(0.8f, 4.2f) * casilla;
            Particula p = nueva(cx, cy, MathUtils.cos(angulo) * vel, MathUtils.sin(angulo) * vel,
                    MathUtils.random(0.35f, 0.85f), MathUtils.random(0.14f, 0.34f) * casilla, true);
            float mezcla = MathUtils.random();
            p.r = 1f;
            p.g = 0.35f + 0.6f * mezcla;
            p.b = 0.05f + 0.3f * mezcla * mezcla;
            p.freno = 2.2f;
        }
        for (int i = 0; i < 14; i++) {
            float angulo = MathUtils.random(MathUtils.PI2);
            float vel = MathUtils.random(0.2f, 1.1f) * casilla;
            Particula p = nueva(cx, cy, MathUtils.cos(angulo) * vel, MathUtils.sin(angulo) * vel + 0.4f * casilla,
                    MathUtils.random(0.6f, 1.2f), MathUtils.random(0.35f, 0.7f) * casilla, false);
            p.r = p.g = p.b = MathUtils.random(0.25f, 0.45f);
            p.freno = 1.2f;
        }
    }

    /** Destello de colores (moneda, objeto encontrado). */
    public void destello(float cx, float cy, float casilla, float r, float g, float b, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            float angulo = MathUtils.random(MathUtils.PI2);
            float vel = MathUtils.random(0.6f, 2.2f) * casilla;
            Particula p = nueva(cx, cy, MathUtils.cos(angulo) * vel, MathUtils.sin(angulo) * vel,
                    MathUtils.random(0.4f, 0.9f), MathUtils.random(0.10f, 0.22f) * casilla, true);
            p.r = r;
            p.g = g;
            p.b = b;
            p.gravedad = 1.2f * casilla;
            p.freno = 2.5f;
        }
    }

    /** Nubecita blanca (cuando desaparece un enemigo). */
    public void nube(float cx, float cy, float casilla) {
        for (int i = 0; i < 16; i++) {
            float angulo = MathUtils.random(MathUtils.PI2);
            float vel = MathUtils.random(0.3f, 1.6f) * casilla;
            Particula p = nueva(cx, cy, MathUtils.cos(angulo) * vel, MathUtils.sin(angulo) * vel,
                    MathUtils.random(0.4f, 0.8f), MathUtils.random(0.25f, 0.5f) * casilla, false);
            p.r = p.g = p.b = 0.95f;
            p.freno = 3f;
        }
    }

    private Particula nueva(float x, float y, float vx, float vy, float vida, float tam, boolean aditiva) {
        Particula p = new Particula();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.vida = vida;
        p.total = vida;
        p.tam = tam;
        p.aditiva = aditiva;
        lista.add(p);
        return p;
    }

    public void actualizar(float dt) {
        for (int i = lista.size - 1; i >= 0; i--) {
            Particula p = lista.get(i);
            p.vida -= dt;
            if (p.vida <= 0) {
                lista.removeIndex(i);
                continue;
            }
            float freno = Math.max(0f, 1f - p.freno * dt);
            p.vx *= freno;
            p.vy = p.vy * freno - p.gravedad * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }
    }

    public void dibujar(Batch b, TextureRegion chispa) {
        if (chispa == null || lista.size == 0) return;
        for (int pasada = 0; pasada < 2; pasada++) {
            boolean aditivo = pasada == 1;                  // primero el humo, después las chispas
            if (aditivo) b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            for (Particula p : lista) {
                if (p.aditiva != aditivo) continue;
                float vida = p.vida / p.total;
                float tam = p.tam * (aditivo ? 0.4f + vida : 1.6f - vida * 0.6f);
                b.setColor(p.r, p.g, p.b, aditivo ? vida : vida * 0.55f);
                b.draw(chispa, p.x - tam / 2f, p.y - tam / 2f, tam, tam);
            }
            if (aditivo) b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        b.setColor(1f, 1f, 1f, 1f);
    }

    public void limpiar() { lista.clear(); }
}
