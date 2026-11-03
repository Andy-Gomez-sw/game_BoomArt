package explosionparalela.graficos;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import explosionparalela.model.Avatar;

/**
 * Dibuja un avatar con tres capas: el cuerpo (que se tiñe con el color elegido),
 * la cara y el accesorio. Las imágenes son assets/imagenes/avatar_cuerpo_*.png,
 * avatar_cara.png y avatar_acc_*.png; las tres tienen el mismo tamaño de lienzo, que
 * deja espacio arriba para los accesorios que sobresalen (por eso es más alto que ancho).
 */
public final class AvatarDibujo {
    /** Alto del lienzo / ancho del lienzo. */
    public static final float PROPORCION = 56f / 40f;

    private static final Color TINTE = new Color();

    private AvatarDibujo() { }

    /**
     * @param x,y    esquina inferior izquierda de la casilla (el accesorio sale por arriba)
     * @param tam    ancho de la casilla
     * @param escalaX,escalaY  para el efecto de "aplastarse" al saltar o moverse (origen: pies)
     */
    public static void dibujar(Batch b, Recursos r, Avatar avatar, float x, float y, float tam,
                               float escalaX, float escalaY, float rotacion) {
        float alto = tam * PROPORCION;
        float alfa = b.getColor().a;
        Color antes = new Color(b.getColor());

        TextureRegion cuerpo = r.region("avatar_cuerpo_" + avatar.getForma().getArchivo());
        if (cuerpo != null) {
            Color.rgb888ToColor(TINTE, avatar.getColor());
            TINTE.a = alfa;
            b.setColor(TINTE);                                    // el cuerpo es blanco: se tiñe
            b.draw(cuerpo, x, y, tam / 2f, 0, tam, alto, escalaX, escalaY, rotacion);
        }
        b.setColor(antes.r, antes.g, antes.b, alfa);
        TextureRegion cara = r.region("avatar_cara");
        if (cara != null) b.draw(cara, x, y, tam / 2f, 0, tam, alto, escalaX, escalaY, rotacion);

        String acc = avatar.getAccesorio().getArchivo();
        if (acc != null) {
            TextureRegion a = r.region("avatar_acc_" + acc);
            if (a != null) b.draw(a, x, y, tam / 2f, 0, tam, alto, escalaX, escalaY, rotacion);
        }
        b.setColor(antes);
    }
}
