package explosionparalela.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import explosionparalela.ExplosionParalelaJuego;

/** Abre la ventana de escritorio y arranca el juego. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        new Lwjgl3Application(new ExplosionParalelaJuego(), configuracion());
    }

    private static Lwjgl3ApplicationConfiguration configuracion() {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Explosión Paralela");
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setWindowedMode(1280, 720);
        config.setWindowSizeLimits(960, 540, -1, -1);
        // suavizado de bordes (MSAA 4x): las imágenes escaladas se ven más limpias
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        config.setWindowIcon("icono.png");
        return config;
    }
}
