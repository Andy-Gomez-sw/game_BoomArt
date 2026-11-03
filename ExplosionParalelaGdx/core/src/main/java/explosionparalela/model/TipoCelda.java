package explosionparalela.model;

/** Tipos de celda del mapa. */
public enum TipoCelda {
    VACIO,            // piso
    INDESTRUCTIBLE,   // barda que no se rompe
    DESTRUCTIBLE,     // barda que se rompe con una explosión
    PUERTA,           // se puede cruzar, pero la explosión no la atraviesa
    TRAMPA            // el jugador pierde una vida al pisarla (los enemigos la evitan)
}
