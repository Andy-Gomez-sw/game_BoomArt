package explosionparalela.levels;

/**
 * Mapas fijos de los laberintos (niveles 3 y 4). Puedes editarlos a mano.
 *
 * Leyenda:  #  muro indestructible     .  piso
 *           D  puerta (la explosión no la atraviesa)     P  inicio del jugador
 *
 * Los muros destructibles, los objetos, las bombas ocultas y los enemigos se
 * colocan al azar encima de este plano cada vez que empieza el nivel.
 * Todas las filas de un mapa deben medir lo mismo.
 */
public final class Laberintos {
    private Laberintos() { }

    /** Nivel 3 - Tianguistenco: 15 filas x 23 columnas, 3 habitaciones con puertas */
    public static final String[] TIANGUISTENCO = {
            "#######################",
            "#P.............#####..#",
            "#.#.#.###.###.##...##.#",
            "#.#.....#...#..D...##.#",
            "#.#####.#.#.#..#...##.#",
            "#.....#.###D##.##D##..#",
            "#.#.###.##...##.#.##.##",
            "#.#.#....D...##.#.....#",
            "#.#.#.####...##.#.###.#",
            "#.###D#########.#.#...#",
            "#.##...####...#.###.#.#",
            "#..#...D....#.#...#.#.#",
            "#.##...######.###.#.#.#",
            "#..#####......#.....#.#",
            "#######################"
    };

    /** Nivel 4 - Metepec: 17 filas x 25 columnas, 4 habitaciones con puertas */
    public static final String[] METEPEC = {
            "#########################",
            "#P..........#......######",
            "#######.#.#.#.###.##...##",
            "#.#...#.#.....#....D...##",
            "#.#.#.#.#.#.#.#.####...##",
            "#...#...#.#...#.#..##D###",
            "#.#######.##D##.#.###.###",
            "#.#...#.#.#...#.....#...#",
            "#.#.#.#.#.#...D.#####.#.#",
            "#.#.#.#...#...#.#.......#",
            "#.#...#.#######.#.##.##.#",
            "#.##D##.......#...##D##.#",
            "#.#...###.###.#####...#.#",
            "#.#...D...#...#...D...#.#",
            "#.#...#.#.#.###.#.#...#.#",
            "#.#####.#.......#.#####.#",
            "#########################"
    };
}
