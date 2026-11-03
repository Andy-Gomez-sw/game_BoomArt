package explosionparalela.core;

import explosionparalela.audio.Sonido;
import explosionparalela.levels.Nivel;
import explosionparalela.model.Avatar;
import explosionparalela.model.Bomba;
import explosionparalela.model.Enemigo;
import explosionparalela.model.Jugador;
import explosionparalela.model.Mapa;
import explosionparalela.model.TipoCelda;
import explosionparalela.model.TipoObjeto;
import explosionparalela.shared.ControlPausa;
import explosionparalela.shared.Inventario;
import explosionparalela.threads.HiloBomba;
import explosionparalela.threads.HiloEnemigo;
import explosionparalela.threads.HiloEventos;
import explosionparalela.threads.HiloTemporizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Controlador de la partida completa (4 niveles). Las vidas, el tiempo (4 min),
 * el inventario y la puntuación se conservan de un nivel a otro; el mapa, los
 * enemigos, las bombas y sus hilos se renuevan en cada nivel.
 */
public class Juego {
    public enum Estado { JUGANDO, PAUSADO, TRANSICION, GANADO, PERDIDO }

    public interface Escucha {
        /** Se llama desde un hilo que NO es el de Swing: usar invokeLater en la UI. */
        void alTerminar(boolean victoria, String mensaje);
    }

    public static final int TIEMPO_TOTAL = 240;   // 4 minutos para TODO el juego
    public static final int ALCANCE = 2;          // casillas que alcanza la explosión
    public static final int MAX_BOMBAS = 3;
    public static final int SEGUNDOS_RELOJ = 30;      // lo que da el ítem reloj
    public static final long MS_ESCUDO = 6000;        // lo que dura el escudo
    public static final int MAX_ALCANCE_EXTRA = 3;

    /** Mensaje que la pantalla muestra flotando cuando recoges una ayuda ("+1 vida", "+30 s"...). */
    public static final class Aviso {
        public final TipoObjeto tipo;
        public final String texto;
        Aviso(TipoObjeto tipo, String texto) {
            this.tipo = tipo;
            this.texto = texto;
        }
    }

    private final Jugador jugador;
    private final Inventario inventario = new Inventario();
    private final Object cerrojo = new Object();
    private final Set<TipoObjeto> encontrados =
            Collections.synchronizedSet(EnumSet.noneOf(TipoObjeto.class));

    // controlGlobal: temporizador (dura toda la partida)
    // controlNivel: bombas, enemigos y eventos (se renueva en cada nivel)
    private final ControlPausa controlGlobal = new ControlPausa();
    private volatile ControlPausa controlNivel = new ControlPausa();

    private volatile Nivel nivel;
    private volatile Mapa mapa;
    private volatile List<Enemigo> enemigos = new CopyOnWriteArrayList<>();
    private volatile List<Bomba> bombas = new CopyOnWriteArrayList<>();

    private HiloTemporizador temporizador;
    private volatile Estado estado = Estado.TRANSICION;
    private volatile String mensajeTransicion = "";
    private volatile Escucha escucha;
    private volatile long ultimoMovimiento = 0;
    private final Queue<Aviso> avisos = new ConcurrentLinkedQueue<>();
    private volatile int alcanceExtra = 0;

    public Juego(String nombre, Avatar avatar) {
        jugador = new Jugador(nombre, avatar, 1, 1);
        cargarNivel(1, "¡Prepárate, " + nombre + "!");
    }

    /** Arranca el temporizador. Los demás hilos arrancan en comenzarNivel(). */
    public void iniciar() {
        temporizador = new HiloTemporizador(this, controlGlobal, TIEMPO_TOTAL);   // Hilo 1
        temporizador.start();
    }

    /** Detiene todos los hilos (para reiniciar o salir). */
    public void detener() {
        controlGlobal.detener();
        controlNivel.detener();
        Sonido.detenerMusica();
    }

    // ----------------------------------------------------------- niveles

    /** Prepara el nivel (mapa, enemigos, jugador en el inicio) y espera ENTER. */
    private void cargarNivel(int numero, String mensaje) {
        controlNivel.detener();               // detiene los hilos del nivel anterior
        Sonido.detenerMusica();
        controlNivel = new ControlPausa();
        Nivel nuevo = Nivel.crear(numero);
        nivel = nuevo;
        mapa = nuevo.getMapa();
        enemigos = new CopyOnWriteArrayList<>(nuevo.getEnemigos());
        bombas = new CopyOnWriteArrayList<>();
        encontrados.clear();
        alcanceExtra = 0;                     // el "fuego extra" dura solo un nivel
        avisos.clear();
        jugador.setPosicion(nuevo.getInicioFila(), nuevo.getInicioColumna());
        mensajeTransicion = mensaje;
        controlGlobal.pausar();               // el reloj no corre entre niveles
        estado = Estado.TRANSICION;
    }

    /** ENTER: empieza el nivel que está cargado (arranca los hilos del nivel). */
    public void comenzarNivel() {
        synchronized (cerrojo) {
            if (estado != Estado.TRANSICION) return;
            estado = Estado.JUGANDO;
            ControlPausa cn = controlNivel;
            for (Enemigo e : enemigos) {
                new HiloEnemigo(this, cn, e).start();         // Hilo 3
            }
            new HiloEventos(this, cn).start();                // Hilo 4
            controlGlobal.reanudar();
            Sonido.iniciarMusica(nivel.getNumero());
        }
    }

    /** Nivel terminado cuando no quedan enemigos y se encontraron todos los objetos. */
    private void comprobarVictoria() {
        synchronized (cerrojo) {
            if (estado != Estado.JUGANDO) return;
            for (Enemigo e : enemigos) {
                if (e.estaVivo()) return;
            }
            if (!encontrados.containsAll(nivel.getRequeridos())) return;

            int actual = nivel.getNumero();
            inventario.sumarPuntos(200, "Nivel " + actual + " completado");
            if (actual >= Nivel.TOTAL) {
                terminar(true, "¡Completaste los 4 niveles!");
            } else {
                Sonido.reproducir(Sonido.Efecto.NIVEL_COMPLETO);
                cargarNivel(actual + 1, "¡Nivel " + actual + " completado!");
            }
        }
    }

    // ----------------------------------------------------------- jugador

    /** Lo llama la interfaz (hilo de Swing) al presionar una flecha o WASD. */
    public void moverJugador(int df, int dc) {
        if (estado != Estado.JUGANDO) return;
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoMovimiento < 110) return;   // velocidad del jugador
        ultimoMovimiento = ahora;

        Mapa m = mapa;
        int nf = jugador.getFila() + df;
        int nc = jugador.getColumna() + dc;
        if (!m.esTransitable(nf, nc)) return;
        jugador.setPosicion(nf, nc);

        if (m.getTipo(nf, nc) == TipoCelda.TRAMPA) {
            golpearJugador();
        }
        TipoObjeto objeto = m.recogerObjeto(nf, nc);
        if (objeto != null && objeto.esAyuda()) {
            aplicarAyuda(objeto);
        } else if (objeto != null) {
            inventario.agregar(objeto, "Nivel " + nivel.getNumero() + " - " + nivel.getMunicipio());
            Sonido.reproducir(objeto == TipoObjeto.MONEDA ? Sonido.Efecto.MONEDA : Sonido.Efecto.OBJETO);
            if (nivel.getRequeridos().contains(objeto)) {
                encontrados.add(objeto);
            }
            comprobarVictoria();
        }
        for (Enemigo e : enemigos) {
            verificarContacto(e);
        }
    }

    /** Aplica el efecto de un ítem de ayuda (al recogerlo). */
    private void aplicarAyuda(TipoObjeto ayuda) {
        String lugar = "Nivel " + nivel.getNumero();
        String texto;
        switch (ayuda) {
            case CORAZON:
                if (jugador.curar()) {
                    texto = "+1 vida";
                    inventario.anotar("Corazón: recuperaste una vida - " + lugar);
                } else {
                    texto = "Vida llena: +" + ayuda.getPuntos() + " puntos";
                    inventario.anotar("Corazón con la vida llena - " + lugar);
                }
                break;
            case RELOJ: {
                HiloTemporizador t = temporizador;
                int sumados = t == null ? 0 : t.sumar(SEGUNDOS_RELOJ, TIEMPO_TOTAL);
                texto = sumados > 0 ? "+" + sumados + " s" : "Tiempo al máximo";
                inventario.anotar("Reloj: recuperaste " + sumados + " s - " + lugar);
                break;
            }
            case ESCUDO:
                jugador.darEscudo(MS_ESCUDO);
                texto = "Escudo " + (MS_ESCUDO / 1000) + " s";
                inventario.anotar("Escudo: sin daño por " + (MS_ESCUDO / 1000) + " s - " + lugar);
                break;
            default:   // ALCANCE
                if (alcanceExtra < MAX_ALCANCE_EXTRA) alcanceExtra++;
                texto = "Fuego extra +" + alcanceExtra;
                inventario.anotar("Fuego extra: explosiones más largas - " + lugar);
        }
        inventario.sumarPuntos(ayuda.getPuntos(), "Ayuda: " + ayuda.getNombre());
        Sonido.reproducir(Sonido.Efecto.AYUDA);
        avisos.add(new Aviso(ayuda, texto));
    }

    /** Siguiente aviso pendiente de mostrar (o null). Lo consulta la pantalla en cada cuadro. */
    public Aviso sacarAviso() { return avisos.poll(); }

    public int getAlcanceExtra() { return alcanceExtra; }

    /** Coloca un jarrito de barro en la posición del jugador. */
    public void colocarBomba() {
        synchronized (cerrojo) {
            if (estado != Estado.JUGANDO) return;
            int activas = 0;
            for (Bomba b : bombas) {
                if (b.estaEnLlamas()) continue;
                activas++;
                if (b.getFila() == jugador.getFila() && b.getColumna() == jugador.getColumna()) {
                    return;   // ya hay una bomba aquí
                }
            }
            if (activas >= MAX_BOMBAS) return;
            lanzarBomba(jugador.getFila(), jugador.getColumna());
            Sonido.reproducir(Sonido.Efecto.COLOCAR_BOMBA);
        }
    }

    /** Crea una bomba y su hilo (HILO 2). La usan el jugador y las bombas ocultas. */
    private void lanzarBomba(int fila, int columna) {
        Bomba bomba = new Bomba(fila, columna);
        bombas.add(bomba);
        new HiloBomba(this, controlNivel, bomba).start();
    }

    // -------------------------------------------------------- explosiones

    /** La llama el HiloBomba al cumplirse los 4 segundos. */
    public void explotarBomba(Bomba bomba) {
        synchronized (cerrojo) {
            if (controlNivel.isDetenido()) return;   // bomba de un nivel que ya terminó
            Mapa m = mapa;
            int alcance = ALCANCE + alcanceExtra;
            List<int[]> celdas = new ArrayList<>();
            celdas.add(new int[]{bomba.getFila(), bomba.getColumna()});

            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                for (int i = 1; i <= alcance; i++) {
                    int f = bomba.getFila() + d[0] * i;
                    int c = bomba.getColumna() + d[1] * i;
                    TipoCelda tipo = m.getTipo(f, c);
                    if (tipo == TipoCelda.INDESTRUCTIBLE || tipo == TipoCelda.PUERTA) break;
                    celdas.add(new int[]{f, c});
                    if (tipo == TipoCelda.DESTRUCTIBLE) {
                        boolean teniaBomba = m.quitarBombaOculta(f, c);
                        m.destruirMuro(f, c);              // puede soltar moneda u objeto
                        if (teniaBomba) lanzarBomba(f, c); // ¡bomba oculta!
                        break;
                    }
                }
            }
            bomba.explotar(celdas);
            Sonido.reproducir(Sonido.Efecto.EXPLOSION);

            for (Enemigo e : enemigos) {
                if (!e.estaVivo()) continue;
                for (int[] celda : celdas) {
                    if (e.getFila() == celda[0] && e.getColumna() == celda[1]) {
                        e.matar();
                        Sonido.reproducir(Sonido.Efecto.ENEMIGO_ELIMINADO);
                        inventario.sumarPuntos(50, "Enemigo eliminado");
                        break;
                    }
                }
            }
            for (int[] celda : celdas) {
                if (jugador.getFila() == celda[0] && jugador.getColumna() == celda[1]) {
                    golpearJugador();
                    break;
                }
            }
            comprobarVictoria();
        }
    }

    public void retirarBomba(Bomba bomba) {
        bombas.remove(bomba);
    }

    // ------------------------------------------------- reglas del juego

    /** Casillas por las que pueden caminar los enemigos. */
    public boolean puedeMover(int fila, int columna) {
        return mapa.esCaminoEnemigo(fila, columna);
    }

    /** Si el enemigo toca al jugador, éste pierde una vida. */
    public void verificarContacto(Enemigo e) {
        if (e.estaVivo() && e.getFila() == jugador.getFila()
                && e.getColumna() == jugador.getColumna()) {
            golpearJugador();
        }
    }

    private void golpearJugador() {
        if (jugador.quitarVida()) {
            Sonido.reproducir(Sonido.Efecto.GOLPE);
            if (jugador.getVidas() <= 0) {
                terminar(false, "Te quedaste sin vidas");
            }
        }
    }

    public void terminar(boolean victoria, String mensaje) {
        synchronized (cerrojo) {
            if (estado == Estado.GANADO || estado == Estado.PERDIDO) return;
            estado = victoria ? Estado.GANADO : Estado.PERDIDO;
        }
        controlGlobal.detener();
        controlNivel.detener();
        Sonido.detenerMusica();
        Sonido.reproducir(victoria ? Sonido.Efecto.VICTORIA : Sonido.Efecto.DERROTA);
        Escucha l = escucha;
        if (l != null) l.alTerminar(victoria, mensaje);
    }

    /** Pausar / reanudar (tecla P). */
    public void alternarPausa() {
        synchronized (cerrojo) {
            if (estado == Estado.JUGANDO) {
                estado = Estado.PAUSADO;
                controlGlobal.pausar();
                controlNivel.pausar();
                Sonido.pausarMusica();
            } else if (estado == Estado.PAUSADO) {
                estado = Estado.JUGANDO;
                controlGlobal.reanudar();
                controlNivel.reanudar();
                Sonido.reanudarMusica();
            }
        }
    }

    // --------------------------------------------------------- consultas

    public boolean estaActivo() {
        Estado e = estado;
        return e == Estado.JUGANDO || e == Estado.PAUSADO || e == Estado.TRANSICION;
    }

    public int getSegundosRestantes() {
        return temporizador == null ? TIEMPO_TOTAL : temporizador.getRestantes();
    }

    public int enemigosVivos() {
        int n = 0;
        for (Enemigo e : enemigos) if (e.estaVivo()) n++;
        return n;
    }

    public int getObjetosEncontrados() { return encontrados.size(); }

    public Estado getEstado() { return estado; }
    public Nivel getNivel() { return nivel; }
    public String getMensajeTransicion() { return mensajeTransicion; }
    public Mapa getMapa() { return mapa; }
    public Jugador getJugador() { return jugador; }
    public Inventario getInventario() { return inventario; }
    public List<Enemigo> getEnemigos() { return enemigos; }
    public List<Bomba> getBombas() { return bombas; }
    public void setEscucha(Escucha escucha) { this.escucha = escucha; }
}
