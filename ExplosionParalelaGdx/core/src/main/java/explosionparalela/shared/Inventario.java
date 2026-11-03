package explosionparalela.shared;

import explosionparalela.model.TipoObjeto;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Inventario COMPARTIDO: objetos, monedas, puntuación e historial.
 * Lo usan varios hilos (jugador, bombas, eventos) -> métodos synchronized.
 */
public class Inventario {
    private final Map<TipoObjeto, Integer> cantidades = new EnumMap<>(TipoObjeto.class);
    private final List<String> historial = new ArrayList<>();
    private int puntuacion = 0;
    private final long inicio = System.currentTimeMillis();

    public synchronized void agregar(TipoObjeto tipo, String origen) {
        cantidades.merge(tipo, 1, Integer::sum);
        puntuacion += tipo.getPuntos();
        registrar("Recogiste: " + tipo.getNombre() + " (+" + tipo.getPuntos() + ") - " + origen);
    }

    public synchronized void sumarPuntos(int puntos, String motivo) {
        puntuacion += puntos;
        registrar(motivo + " (+" + puntos + ")");
    }

    /** Agrega una línea al historial (por ejemplo, el uso de una ayuda) sin dar puntos. */
    public synchronized void anotar(String texto) {
        registrar(texto);
    }

    private void registrar(String texto) {
        long s = (System.currentTimeMillis() - inicio) / 1000;
        historial.add(String.format("[%02d:%02d] %s", s / 60, s % 60, texto));
    }

    public synchronized int cantidad(TipoObjeto tipo) {
        return cantidades.getOrDefault(tipo, 0);
    }

    public synchronized int getMonedas() { return cantidad(TipoObjeto.MONEDA); }
    public synchronized int getPuntuacion() { return puntuacion; }

    public synchronized List<String> getHistorial() {
        return new ArrayList<>(historial);
    }

    /** Resumen de todo lo recolectado (se muestra al final de la partida). */
    public synchronized String resumen() {
        StringBuilder sb = new StringBuilder();
        for (TipoObjeto t : TipoObjeto.values()) {
            int n = cantidades.getOrDefault(t, 0);
            if (n > 0) sb.append(t.getNombre()).append(": ").append(n).append("\n");
        }
        if (sb.length() == 0) sb.append("No recolectaste objetos\n");
        sb.append("Puntuación: ").append(puntuacion);
        return sb.toString();
    }
}
