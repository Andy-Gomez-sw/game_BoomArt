# Explosión Paralela — versión libGDX (Java)

Juego tipo Bomberman con temática de artesanías del Estado de México, hecho en **Java con libGDX 1.14.2**.
Los 4 hilos (temporizador, bombas, enemigos y eventos) y la sincronización del inventario siguen siendo Java puro:
libGDX solo se encarga de **dibujar, el audio y el teclado**.

## Requisitos
- **JDK 17 o 21** (con 24 o más puede fallar Gradle). Comprueba con `java -version`.
- Internet la primera vez (Gradle descarga libGDX).

## Cómo ejecutarlo
Desde la carpeta del proyecto:

    ./gradlew lwjgl3:run          (Linux / macOS)
    gradlew.bat lwjgl3:run        (Windows)

**En IntelliJ IDEA:** File > Open > elige la carpeta (o su `build.gradle`) > espera a que Gradle termine >
abre la clase `Lwjgl3Launcher` (módulo `lwjgl3`) y ejecútala.
Si no abre en macOS, agrega la opción de VM `-XstartOnFirstThread`.

**Un solo .jar para entregar:** `./gradlew lwjgl3:dist` y queda en `lwjgl3/build/libs/ExplosionParalela-1.0.0.jar`
(se abre con `java -jar ExplosionParalela-1.0.0.jar`).

**Si Gradle te da problemas:** genera un proyecto vacío con el generador oficial *gdx-liftoff*
(plataforma: solo Desktop, extensión: **freetype**, versión 1.14.x), borra su código y copia aquí:
`core/src/main/java/explosionparalela` y la carpeta `assets`.

## Controles
Flechas o WASD: mover · ESPACIO: poner el jarrito · ENTER: comenzar el nivel · P: pausa · I: inventario ·
M: sonido · R: reiniciar · ESC: volver al menú.
En el menú: clic en las tarjetas, flechas laterales, rueda del mouse o TAB + flechas del teclado; ENTER para jugar.

## Ítems de ayuda
Aparecen escondidos dentro de los muros que rompes (cada nivel trae los suyos) y, de vez en cuando, el hilo de eventos
aleatorios deja uno en el mapa. Se recogen caminando sobre ellos y brillan en el suelo para distinguirlos de las monedas.

| Ítem | Efecto |
|---|---|
| Corazón | +1 vida (nunca más de 3). Con la vida llena da 25 puntos. Sale más seguido cuando te quedan pocas vidas |
| Reloj | +30 segundos, sin pasar del máximo de 4:00 |
| Escudo | 6 segundos sin recibir daño (burbuja azul alrededor del personaje) |
| Fuego extra | +1 casilla de alcance en las explosiones durante el resto del nivel (hasta +3) |

Al recogerlos sale un texto flotante ("+1 vida", "+30 s"...), suena un efecto y queda anotado en el historial del inventario.
Para ajustarlos: `Juego.java` (SEGUNDOS_RELOJ, MS_ESCUDO, MAX_ALCANCE_EXTRA), `Nivel.java` (cuáles van escondidos en cada nivel)
y `HiloEventos.java` (PROBABILIDAD_AYUDA y qué tanto sale cada una).

## Cómo está organizado (por si te lo piden en la revisión)
    core/src/main/java/explosionparalela/
      model/       datos del juego: Mapa, Jugador, Enemigo, Bomba, Avatar...        (sin libGDX)
      shared/      Inventario (recurso compartido, synchronized) y ControlPausa (monitor wait/notify)
      threads/     los 4 hilos: HiloTemporizador, HiloBomba, HiloEnemigo (+Rutas), HiloEventos
      levels/      Nivel (4 niveles) y Laberintos (mapas de los niveles 3 y 4)
      core/        Juego: reglas, vidas, tiempo, paso de nivel; une todo lo anterior
      audio/       Sonido (fachada) y GdxSonido (reproduce con libGDX)
      graficos/    Recursos (carga imágenes y fuentes), AvatarDibujo, Particulas
      pantallas/   PantallaInicio (menú con carruseles), PantallaJuego, Carrusel
      ExplosionParalelaJuego.java   arranque de libGDX
    lwjgl3/        ventana de escritorio (Lwjgl3Launcher)
    assets/        imágenes, sonidos y fuente (lo que puedes reemplazar)
    tools/         GeneradorAssets.java: dibuja y sintetiza los assets por defecto

La lógica no depende de libGDX; la pantalla solo **lee** su estado cada cuadro y lo dibuja. Los hilos piden sonidos
sin tocar el audio: GdxSonido los guarda en una cola segura (`ConcurrentLinkedQueue`) y los reproduce en el hilo gráfico.

## Cómo cambiar imágenes y sonidos
Mira `assets/LEEME.txt`: basta con **reemplazar el archivo con el mismo nombre** (imágenes png/jpg, música mp3/ogg/wav).
Las fotos de los monumentos van en `assets/imagenes/nivel1_fondo.jpg` … `nivel4_fondo.jpg`.

## Créditos
Fuente "Lilita One" (Juan Montoreano) bajo licencia SIL OFL: ver `assets/fuentes/OFL.txt`.
Las imágenes y sonidos por defecto los genera `tools/GeneradorAssets.java`.
