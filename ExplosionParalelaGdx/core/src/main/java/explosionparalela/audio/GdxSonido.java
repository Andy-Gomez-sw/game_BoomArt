package explosionparalela.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Reproduce la música y los efectos con el audio de libGDX.
 *
 * Los archivos se buscan en assets/sonidos con extensión mp3, ogg o wav (en ese orden),
 * así que para usar tu propia música basta con reemplazar el archivo, por ejemplo
 * assets/sonidos/musica_nivel1.mp3.
 *
 * Los hilos del juego (bombas, enemigos...) piden sonidos desde otros hilos; para no
 * tocar el audio fuera del hilo gráfico, las peticiones se guardan en una cola y se
 * ejecutan en procesar(), que se llama una vez por cuadro.
 */
public class GdxSonido implements Sonido.Backend, Disposable {
    private static final String[] EXTENSIONES = {".mp3", ".ogg", ".wav"};
    private static final float VOLUMEN_EFECTOS = 0.9f;
    private static final float VOLUMEN_MUSICA = 0.45f;

    private final ConcurrentLinkedQueue<Runnable> cola = new ConcurrentLinkedQueue<>();

    // Solo se usan desde el hilo gráfico (dentro de procesar)
    private final Map<Sonido.Efecto, Sound> efectos = new EnumMap<>(Sonido.Efecto.class);
    private final Map<Sonido.Efecto, Boolean> faltantes = new EnumMap<>(Sonido.Efecto.class);
    private Music musica;
    private int nivelMusica = 0;
    private boolean pausadaPorJuego = false;
    private boolean silenciado = false;

    /** Ejecuta las peticiones pendientes. Llamar una vez por cuadro desde render(). */
    public void procesar() {
        Runnable tarea;
        while ((tarea = cola.poll()) != null) {
            try {
                tarea.run();
            } catch (RuntimeException e) {
                Gdx.app.error("Sonido", "No pude reproducir un sonido: " + e.getMessage());
            }
        }
    }

    // ------------------------------------------------------ Sonido.Backend

    @Override
    public void efecto(Sonido.Efecto efecto) {
        cola.add(() -> tocar(efecto));
    }

    @Override
    public void iniciarMusica(int nivel) {
        cola.add(() -> {
            cerrarMusica();
            nivelMusica = nivel;
            pausadaPorJuego = false;
            abrirMusica();
        });
    }

    @Override
    public void detenerMusica() {
        cola.add(() -> {
            cerrarMusica();
            nivelMusica = 0;
        });
    }

    @Override
    public void pausarMusica() {
        cola.add(() -> {
            pausadaPorJuego = true;
            aplicarMusica();
        });
    }

    @Override
    public void reanudarMusica() {
        cola.add(() -> {
            pausadaPorJuego = false;
            aplicarMusica();
        });
    }

    @Override
    public void silencio(boolean silenciado) {
        cola.add(() -> {
            this.silenciado = silenciado;
            aplicarMusica();
        });
    }

    // ----------------------------------------------------------- internos

    private static FileHandle buscar(String base) {
        for (String ext : EXTENSIONES) {
            FileHandle f = Gdx.files.internal("sonidos/" + base + ext);
            if (f.exists()) return f;
        }
        return null;
    }

    private void tocar(Sonido.Efecto efecto) {
        if (silenciado || faltantes.containsKey(efecto)) return;
        Sound s = efectos.get(efecto);
        if (s == null) {
            FileHandle f = buscar(efecto.getArchivo());
            if (f == null) {
                faltantes.put(efecto, Boolean.TRUE);
                return;
            }
            s = Gdx.audio.newSound(f);
            efectos.put(efecto, s);
        }
        s.play(VOLUMEN_EFECTOS);
    }

    private void abrirMusica() {
        FileHandle f = buscar("musica_nivel" + nivelMusica);
        if (f == null) return;
        musica = Gdx.audio.newMusic(f);
        musica.setLooping(true);
        musica.setVolume(VOLUMEN_MUSICA);
        aplicarMusica();
    }

    /** Enciende o detiene la música según la pausa y el silencio. */
    private void aplicarMusica() {
        if (musica == null) return;
        boolean debeSonar = nivelMusica > 0 && !pausadaPorJuego && !silenciado;
        if (debeSonar && !musica.isPlaying()) {
            musica.play();
        } else if (!debeSonar && musica.isPlaying()) {
            musica.pause();
        }
    }

    private void cerrarMusica() {
        if (musica != null) {
            musica.stop();
            musica.dispose();
            musica = null;
        }
    }

    @Override
    public void dispose() {
        cerrarMusica();
        for (Sound s : efectos.values()) s.dispose();
        efectos.clear();
    }
}
