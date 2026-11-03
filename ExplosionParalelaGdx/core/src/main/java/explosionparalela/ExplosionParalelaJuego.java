package explosionparalela;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import explosionparalela.audio.GdxSonido;
import explosionparalela.audio.Sonido;
import explosionparalela.graficos.Recursos;
import explosionparalela.pantallas.PantallaInicio;

/** Aplicación libGDX: guarda lo que comparten las pantallas (recursos, batch y sonido). */
public class ExplosionParalelaJuego extends Game {
    public Recursos recursos;
    public SpriteBatch batch;
    private GdxSonido sonido;

    @Override
    public void create() {
        recursos = new Recursos();
        batch = new SpriteBatch();
        sonido = new GdxSonido();
        Sonido.setBackend(sonido);
        setScreen(new PantallaInicio(this));
    }

    @Override
    public void render() {
        sonido.procesar();      // reproduce los sonidos que pidieron los hilos del juego
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        Sonido.setBackend(null);
        sonido.dispose();
        batch.dispose();
        recursos.dispose();
    }
}
