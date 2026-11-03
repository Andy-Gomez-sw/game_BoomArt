import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * Genera los dibujos y sonidos que trae el juego por defecto (carpetas assets/imagenes y
 * assets/sonidos). No hace falta ejecutarlo: los archivos ya vienen generados. Sirve para
 * regenerarlos o para ver cómo están hechos y cambiarlos.
 *
 *   Desde la carpeta del proyecto:   java tools/GeneradorAssets.java
 *
 * Todo se dibuja con Java2D; los sonidos se sintetizan. Para usar tus propias imágenes o
 * música NO necesitas esto: reemplaza el archivo con el mismo nombre dentro de assets.
 */
public class GeneradorAssets {
    static final File IMAGENES = new File("assets/imagenes");
    static final File SONIDOS = new File("assets/sonidos");

    // colores de cada nivel: suelo, muro fijo, muro rompible (los mismos de levels/Nivel.java)
    static final int[][] COLORES_NIVEL = {
            {0xF6E7EC, 0x8E5A6E, 0xF48FB1},
            {0xE9DFCB, 0x4E342E, 0xA1887F},
            {0xE8EBF2, 0x455A64, 0xB0BEC5},
            {0xE8D8B0, 0x4A3A2E, 0xC8794B}
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        IMAGENES.mkdirs();
        SONIDOS.mkdirs();
        interfaz();
        avatar();
        for (int n = 1; n <= 4; n++) {
            suelo(n);
            muroFijo(n);
            muroRompible(n);
            fondoNivel(n);
        }
        puerta();
        trampa();
        objetos();
        enemigos();
        fuego();
        fondoInicio();
        Sonidos.generarTodo(SONIDOS);
        System.out.println("Listo: assets/imagenes y assets/sonidos generados.");
    }

    // ============================================================ utilidades

    static BufferedImage lienzo(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }

    static Graphics2D pincel(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    static void guardar(BufferedImage img, String nombre) throws Exception {
        ImageIO.write(img, "png", new File(IMAGENES, nombre + ".png"));
    }

    static Color c(int rgb) { return new Color(rgb); }

    static Color c(int rgb, int alfa) { return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alfa); }

    static Color mezcla(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static Color claro(Color a, double t) { return mezcla(a, Color.WHITE, t); }

    static Color oscuro(Color a, double t) { return mezcla(a, Color.BLACK, t); }

    static void relleno(Graphics2D g, Shape s, Color arriba, Color abajo, double y0, double y1) {
        g.setPaint(new GradientPaint(0, (float) y0, arriba, 0, (float) y1, abajo));
        g.fill(s);
    }

    static void borde(Graphics2D g, Shape s, Color color, double grosor) {
        g.setColor(color);
        g.setStroke(new BasicStroke((float) grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static void brillo(Graphics2D g, double cx, double cy, double rx, double ry, int alfa) {
        g.setColor(new Color(255, 255, 255, alfa));
        g.fill(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
    }

    static void sombraSuave(Graphics2D g, double cx, double cy, double rx, double ry, int alfa) {
        AffineTransform t = g.getTransform();
        g.translate(cx, cy);
        g.scale(rx, ry);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(0, 0), 1f, new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, alfa), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(-1, -1, 2, 2));
        g.setTransform(t);
    }

    // ================================================================ interfaz

    static void interfaz() throws Exception {
        BufferedImage img = lienzo(8, 8);
        Graphics2D g = pincel(img);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 8, 8);
        g.dispose();
        guardar(img, "blanco");

        // panel redondeado (con un degradado suave para que tenga relieve al teñirlo)
        img = lienzo(96, 96);
        g = pincel(img);
        RoundRectangle2D p = new RoundRectangle2D.Double(1, 1, 94, 94, 40, 40);
        relleno(g, p, Color.WHITE, new Color(0xDCDCDC), 0, 96);
        g.dispose();
        guardar(img, "panel");

        img = lienzo(96, 96);
        g = pincel(img);
        borde(g, new RoundRectangle2D.Double(3, 3, 90, 90, 38, 38), Color.WHITE, 5);
        g.dispose();
        guardar(img, "panel_borde");

        img = lienzo(128, 128);
        g = pincel(img);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(4, 4, 120, 120));
        g.dispose();
        guardar(img, "circulo");

        img = lienzo(64, 64);
        g = pincel(img);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(32, 32), 32f, new float[]{0f, 0.35f, 1f},
                new Color[]{new Color(255, 255, 255, 255), new Color(255, 255, 255, 120), new Color(255, 255, 255, 0)}));
        g.fill(new Rectangle2D.Double(0, 0, 64, 64));
        g.dispose();
        guardar(img, "chispa");

        img = lienzo(128, 48);
        g = pincel(img);
        sombraSuave(g, 64, 24, 62, 22, 170);
        g.dispose();
        guardar(img, "sombra");

        img = lienzo(4, 256);       // negro abajo -> transparente arriba (se tiñe al dibujar)
        for (int y = 0; y < 256; y++) {
            double t = y / 255.0;
            int alfa = (int) Math.round(255 * Math.pow(t, 1.4));
            for (int x = 0; x < 4; x++) img.setRGB(x, y, (alfa << 24) | 0xFFFFFF);
        }
        guardar(img, "degradado");

        img = lienzo(512, 32);      // barra de matices
        g = pincel(img);
        g.setClip(new RoundRectangle2D.Double(0, 0, 512, 32, 32, 32));
        for (int x = 0; x < 512; x++) {
            g.setColor(Color.getHSBColor(x / 511f, 0.82f, 1f));
            g.fillRect(x, 0, 1, 32);
        }
        g.setClip(null);
        borde(g, new RoundRectangle2D.Double(1.5, 1.5, 509, 29, 30, 30), new Color(255, 255, 255, 110), 3);
        g.dispose();
        guardar(img, "matiz");

        img = lienzo(64, 64);       // flecha hacia la derecha
        g = pincel(img);
        Path2D f = new Path2D.Double();
        f.moveTo(18, 8);
        f.lineTo(52, 32);
        f.lineTo(18, 56);
        f.closePath();
        g.setColor(Color.WHITE);
        g.fill(f);
        borde(g, f, Color.WHITE, 8);
        g.dispose();
        guardar(img, "flecha");

        // corazón
        img = lienzo(128, 128);
        g = pincel(img);
        Path2D h = new Path2D.Double();
        h.moveTo(64, 112);
        h.curveTo(8, 72, 6, 30, 38, 20);
        h.curveTo(54, 15, 64, 28, 64, 36);
        h.curveTo(64, 28, 74, 15, 90, 20);
        h.curveTo(122, 30, 120, 72, 64, 112);
        h.closePath();
        relleno(g, h, c(0xFF6B6B), c(0xC62828), 15, 112);
        borde(g, h, c(0x7F1D1D), 6);
        brillo(g, 42, 40, 11, 7, 150);
        g.dispose();
        guardar(img, "corazon");

        // aviso "!" sobre los enemigos
        img = lienzo(128, 128);
        g = pincel(img);
        sombraSuave(g, 64, 70, 50, 50, 90);
        Ellipse2D bola = new Ellipse2D.Double(10, 10, 108, 108);
        relleno(g, bola, c(0xFF6B6B), c(0xD32F2F), 10, 118);
        borde(g, bola, Color.WHITE, 8);
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(55, 26, 18, 50, 14, 14));
        g.fill(new Ellipse2D.Double(54, 84, 20, 20));
        g.dispose();
        guardar(img, "alerta");

        // piso del escenario de la pantalla de inicio (esquinas redondeadas)
        img = lienzo(360, 330);
        g = pincel(img);
        g.setClip(new RoundRectangle2D.Double(0, 0, 360, 330, 40, 40));
        for (int y = 0; y < 330; y += 45) {
            for (int x = 0; x < 360; x += 45) {
                g.setColor(((x / 45 + y / 45) % 2 == 0) ? c(0xEBDDB8) : c(0xE0CE9F));
                g.fillRect(x, y, 45, 45);
            }
        }
        g.setPaint(new GradientPaint(0, 150, new Color(60, 40, 20, 0), 0, 330, new Color(60, 40, 20, 120)));
        g.fillRect(0, 150, 360, 180);
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 70), 0, 90, new Color(255, 255, 255, 0)));
        g.fillRect(0, 0, 360, 90);
        g.dispose();
        guardar(img, "escenario");
    }

    static void fondoInicio() throws Exception {
        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pincel(img);
        g.setPaint(new GradientPaint(0, 0, c(0x4A3A2E), 0, 720, c(0x1E150F)));
        g.fillRect(0, 0, 1280, 720);
        for (int y = 0; y < 720; y += 64) {
            for (int x = ((y / 64) % 2) * 64; x < 1280; x += 128) {
                g.setColor(new Color(255, 255, 255, 9));
                g.fillRect(x, y, 64, 64);
            }
        }
        g.setPaint(new RadialGradientPaint(new Point2D.Double(640, 330), 640f, new float[]{0f, 1f},
                new Color[]{new Color(200, 121, 75, 70), new Color(200, 121, 75, 0)}));
        g.fillRect(0, 0, 1280, 720);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(640, 360), 800f, new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 170)}));
        g.fillRect(0, 0, 1280, 720);
        g.dispose();
        guardar(img, "fondo_inicio");
    }

    // ================================================================ avatar
    // Lienzo de 40 x 56 unidades (x6 = 240 x 336 px): la casilla ocupa las 40 unidades de abajo
    // y las 16 de arriba son para los accesorios que sobresalen. Mismas medidas que AvatarDibujo.

    static final double E = 6;           // píxeles por unidad
    static final double ARRIBA = 16;     // unidades de espacio arriba

    static Graphics2D pincelAvatar(BufferedImage img) {
        Graphics2D g = pincel(img);
        g.scale(E, E);
        g.translate(0, ARRIBA);
        return g;
    }

    static Shape formaCuerpo(String forma) {
        switch (forma) {
            case "cuadrado":
                return new RoundRectangle2D.Double(6, 6, 28, 28, 10, 10);
            case "hexagono":
                Path2D hex = new Path2D.Double();
                hex.moveTo(12, 5);
                hex.lineTo(28, 5);
                hex.lineTo(36, 20);
                hex.lineTo(28, 35);
                hex.lineTo(12, 35);
                hex.lineTo(4, 20);
                hex.closePath();
                return hex;
            default:
                return new Ellipse2D.Double(5, 5, 30, 30);
        }
    }

    static void avatar() throws Exception {
        String[] formas = {"redondo", "cuadrado", "hexagono"};
        for (String forma : formas) {      // cuerpo blanco: el juego lo tiñe con el color elegido
            BufferedImage img = lienzo(240, 336);
            Graphics2D g = pincelAvatar(img);
            Shape cuerpo = formaCuerpo(forma);
            sombraSuave(g, 20, 36.5, 13, 2.6, 60);
            relleno(g, cuerpo, Color.WHITE, new Color(0xD2D2D2), 5, 35);
            borde(g, cuerpo, new Color(0x9A9A9A), 1.6);
            brillo(g, 14, 12, 5.5, 3.2, 90);        // reflejo (se nota más en colores oscuros)
            g.dispose();
            guardar(img, "avatar_cuerpo_" + forma);
        }
        // cara: ojos, mejillas y sonrisa
        BufferedImage img = lienzo(240, 336);
        Graphics2D g = pincelAvatar(img);
        g.setColor(new Color(255, 120, 140, 70));
        g.fill(new Ellipse2D.Double(8.5, 22, 5, 3.2));
        g.fill(new Ellipse2D.Double(26.5, 22, 5, 3.2));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(11.5, 13.5, 7, 9));
        g.fill(new Ellipse2D.Double(21.5, 13.5, 7, 9));
        borde(g, new Ellipse2D.Double(11.5, 13.5, 7, 9), new Color(0, 0, 0, 90), 0.6);
        borde(g, new Ellipse2D.Double(21.5, 13.5, 7, 9), new Color(0, 0, 0, 90), 0.6);
        g.setColor(new Color(0x1B1B1B));
        g.fill(new Ellipse2D.Double(14, 16.5, 3.6, 5));
        g.fill(new Ellipse2D.Double(24, 16.5, 3.6, 5));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(15.1, 17.2, 1.3, 1.3));
        g.fill(new Ellipse2D.Double(25.1, 17.2, 1.3, 1.3));
        g.setColor(new Color(0x3B2A22));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(14, 22.5, 12, 7, 200, 140, Arc2D.OPEN));
        g.dispose();
        guardar(img, "avatar_cara");

        accesorio("sombrero_charro");
        accesorio("gorro_lana");
        accesorio("mono");
        accesorio("gorra");
        accesorio("corona");
    }

    static void accesorio(String nombre) throws Exception {
        BufferedImage img = lienzo(240, 336);
        Graphics2D g = pincelAvatar(img);
        double t = 5;        // la parte de arriba de la cabeza (en las tres formas)
        switch (nombre) {
            case "sombrero_charro": {
                sombraSuave(g, 20, t + 3.5, 19, 3, 70);
                Ellipse2D ala = new Ellipse2D.Double(1, t - 1, 38, 9);
                relleno(g, ala, c(0x7B5444), c(0x4A2F25), t - 1, t + 8);
                borde(g, ala, c(0x3B261E), 0.9);
                Path2D copa = new Path2D.Double();
                copa.moveTo(12, t + 3);
                copa.curveTo(12.5, t - 4, 13, t - 8, 14.5, t - 11);
                copa.quadTo(20, t - 12.5, 25.5, t - 11);
                copa.curveTo(27, t - 8, 27.5, t - 4, 28, t + 3);
                copa.closePath();
                relleno(g, copa, c(0x8D6556), c(0x5D4037), t - 12, t + 3);
                borde(g, copa, c(0x3B261E), 0.9);
                RoundRectangle2D cinta = new RoundRectangle2D.Double(12.4, t - 3.6, 15.2, 3.8, 2, 2);
                relleno(g, cinta, c(0xFFD54F), c(0xFFA000), t - 4, t);
                brillo(g, 17, t - 7, 2.4, 1.6, 90);
                Ellipse2D ala2 = new Ellipse2D.Double(1, t - 1, 38, 9);
                g.setColor(new Color(255, 255, 255, 25));
                g.fill(new Ellipse2D.Double(4, t - 0.5, 32, 3.6));
                break;
            }
            case "gorro_lana": {
                Arc2D copa = new Arc2D.Double(8, t - 9.5, 24, 23, 0, 180, Arc2D.PIE);
                relleno(g, copa, c(0xFF5252), c(0xD32F2F), t - 9, t + 3);
                g.setColor(new Color(255, 255, 255, 50));
                g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 4; i++) {             // hilos de la lana
                    g.draw(new Arc2D.Double(10.5 + i * 4.2, t - 8 + i * 0.4, 5, 10, 90, 180, Arc2D.OPEN));
                }
                RoundRectangle2D dobladillo = new RoundRectangle2D.Double(7, t, 26, 6, 4, 4);
                relleno(g, dobladillo, c(0xC62828), c(0x8E1B1B), t, t + 6);
                g.setColor(new Color(255, 255, 255, 60));
                for (int i = 0; i < 7; i++) g.fill(new Rectangle2D.Double(9 + i * 3.4, t + 1.2, 0.9, 3.6));
                borde(g, copa, c(0x8E1B1B), 0.9);
                Ellipse2D pompon = new Ellipse2D.Double(15.5, t - 16, 9, 9);
                relleno(g, pompon, c(0xFFFFFF), c(0xFFCDD2), t - 16, t - 7);
                borde(g, pompon, c(0xE57373), 0.7);
                break;
            }
            case "mono": {
                Path2D izq = new Path2D.Double();
                izq.moveTo(26, t + 1);
                izq.curveTo(21, t - 7, 15, t - 8, 14.5, t - 4);
                izq.curveTo(14, t + 1, 15, t + 6, 17, t + 9);
                izq.curveTo(20, t + 6, 24, t + 3, 26, t + 1);
                Path2D der = new Path2D.Double();
                der.moveTo(26, t + 1);
                der.curveTo(31, t - 7, 37, t - 8, 37.5, t - 4);
                der.curveTo(38, t + 1, 37, t + 6, 35, t + 9);
                der.curveTo(32, t + 6, 28, t + 3, 26, t + 1);
                relleno(g, izq, c(0xFF80AB), c(0xF50057), t - 8, t + 9);
                relleno(g, der, c(0xFF80AB), c(0xF50057), t - 8, t + 9);
                borde(g, izq, c(0xAD1457), 0.8);
                borde(g, der, c(0xAD1457), 0.8);
                Ellipse2D nudo = new Ellipse2D.Double(23, t - 2, 6, 6);
                relleno(g, nudo, c(0xFF4081), c(0xC2185B), t - 2, t + 4);
                borde(g, nudo, c(0xAD1457), 0.8);
                brillo(g, 18.5, t - 3, 1.8, 1.1, 120);
                break;
            }
            case "gorra": {
                Arc2D domo = new Arc2D.Double(8, t - 8, 24, 20, 0, 180, Arc2D.PIE);
                relleno(g, domo, c(0x42A5F5), c(0x1565C0), t - 8, t + 2);
                g.setColor(new Color(255, 255, 255, 45));
                g.setStroke(new BasicStroke(0.8f));
                g.draw(new Arc2D.Double(15, t - 8, 10, 20, 90, 180, Arc2D.OPEN));
                borde(g, domo, c(0x0D47A1), 0.9);
                RoundRectangle2D visera = new RoundRectangle2D.Double(17, t + 0.4, 21, 5, 5, 5);
                relleno(g, visera, c(0x1E88E5), c(0x0D47A1), t, t + 5.4);
                borde(g, visera, c(0x0B3A82), 0.8);
                Ellipse2D boton = new Ellipse2D.Double(18.5, t - 9, 3.4, 3.4);
                g.setColor(c(0xBBDEFB));
                g.fill(boton);
                break;
            }
            default: {      // corona
                Path2D corona = new Path2D.Double();
                corona.moveTo(9, t + 2);
                corona.lineTo(8.5, t - 8.5);
                corona.lineTo(14.8, t - 3);
                corona.lineTo(20, t - 11);
                corona.lineTo(25.2, t - 3);
                corona.lineTo(31.5, t - 8.5);
                corona.lineTo(31, t + 2);
                corona.closePath();
                relleno(g, corona, c(0xFFE082), c(0xFF8F00), t - 11, t + 2);
                borde(g, corona, c(0xB26A00), 0.9);
                g.setColor(c(0xE53935));
                g.fill(new Ellipse2D.Double(18.3, t - 4.8, 3.4, 3.4));
                g.setColor(c(0x1E88E5));
                g.fill(new Ellipse2D.Double(11.2, t - 2.2, 2.4, 2.4));
                g.fill(new Ellipse2D.Double(26.4, t - 2.2, 2.4, 2.4));
                brillo(g, 14, t - 1, 2.2, 0.9, 150);
            }
        }
        g.dispose();
        guardar(img, "avatar_acc_" + nombre);
    }

    // ============================================================== casillas

    static void suelo(int n) throws Exception {
        Color base = c(COLORES_NIVEL[n - 1][0]);
        BufferedImage img = lienzo(128, 128);
        Graphics2D g = pincel(img);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                g.setColor((i + j) % 2 == 0 ? base : oscuro(base, 0.055));
                g.fillRect(i * 64, j * 64, 64, 64);
            }
        }
        Random r = new Random(n * 31L);
        for (int k = 0; k < 90; k++) {
            g.setColor(new Color(0, 0, 0, r.nextInt(14) + 4));
            int s = r.nextInt(3) + 1;
            g.fillOval(r.nextInt(128), r.nextInt(128), s, s);
        }
        g.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 50), 128, 128, new Color(255, 255, 255, 0)));
        g.fillRect(0, 0, 128, 128);
        g.setColor(new Color(0, 0, 0, 30));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(1, 1, 126, 126);
        g.dispose();
        guardar(img, "nivel" + n + "_suelo");
    }

    static void muroFijo(int n) throws Exception {
        Color col = c(COLORES_NIVEL[n - 1][1]);
        BufferedImage img = lienzo(128, 128);
        Graphics2D g = pincel(img);
        relleno(g, new Rectangle2D.Double(0, 0, 128, 128), claro(col, 0.12), oscuro(col, 0.2), 0, 128);
        g.setColor(oscuro(col, 0.34));                       // cara frontal (da sensación de altura)
        g.fill(new Rectangle2D.Double(0, 98, 128, 30));
        g.setColor(new Color(255, 255, 255, 45));
        g.fill(new Rectangle2D.Double(0, 98, 128, 3));
        RoundRectangle2D tapa = new RoundRectangle2D.Double(8, 8, 112, 84, 18, 18);
        relleno(g, tapa, claro(col, 0.22), col, 8, 92);
        borde(g, tapa, oscuro(col, 0.38), 3);
        Color hilo = claro(col, 0.7);
        g.setColor(hilo);
        switch (n) {
            case 1:      // felpa: puntadas
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{7, 8}, 0));
                g.draw(new RoundRectangle2D.Double(21, 21, 86, 58, 12, 12));
                break;
            case 2:      // cuero: puntadas y botones
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{7, 8}, 0));
                g.draw(new RoundRectangle2D.Double(20, 20, 88, 60, 10, 10));
                for (int i = 0; i < 4; i++) {
                    double bx = i % 2 == 0 ? 28 : 100;
                    double by = i < 2 ? 28 : 72;
                    g.setColor(oscuro(col, 0.3));
                    g.fill(new Ellipse2D.Double(bx - 5, by - 5, 10, 10));
                    g.setColor(claro(col, 0.45));
                    g.fill(new Ellipse2D.Double(bx - 3, by - 4, 5, 4));
                }
                break;
            case 3:      // lana tejida: filas de V
                g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int fila = 0; fila < 4; fila++) {
                    for (int x = 0; x < 6; x++) {
                        double px = 22 + x * 14.5;
                        double py = 24 + fila * 16;
                        Path2D v = new Path2D.Double();
                        v.moveTo(px, py);
                        v.lineTo(px + 7, py + 11);
                        v.lineTo(px + 14, py);
                        g.draw(v);
                    }
                }
                break;
            default:     // barro: grecas
                g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int k = 0; k < 3; k++) {
                    double s = 54 - k * 17;
                    g.draw(new Arc2D.Double(64 - s / 2, 50 - s / 2, s, s, 30 + k * 40, 280, Arc2D.OPEN));
                }
        }
        g.setColor(oscuro(col, 0.5));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(1, 1, 126, 126);
        g.dispose();
        guardar(img, "nivel" + n + "_muro_fijo");
    }

    static void muroRompible(int n) throws Exception {
        Color col = c(COLORES_NIVEL[n - 1][2]);
        BufferedImage img = lienzo(128, 128);
        Graphics2D g = pincel(img);
        g.setColor(oscuro(col, 0.38));                       // mortero
        g.fillRect(0, 0, 128, 128);
        for (int fila = 0; fila < 4; fila++) {               // ladrillos con la mitad corrida
            double y = 4 + fila * 24;
            for (double x = -48 + (fila % 2) * 24; x < 128; x += 48) {
                RoundRectangle2D ladrillo = new RoundRectangle2D.Double(x + 2, y + 2, 44, 20, 7, 7);
                relleno(g, ladrillo, claro(col, 0.2), oscuro(col, 0.1), y, y + 22);
                g.setColor(new Color(255, 255, 255, 70));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new java.awt.geom.Line2D.Double(x + 8, y + 6, x + 38, y + 6));
            }
        }
        g.setColor(oscuro(col, 0.3));
        g.fill(new Rectangle2D.Double(0, 100, 128, 28));
        g.setColor(new Color(255, 255, 255, 50));
        g.fill(new Rectangle2D.Double(0, 100, 128, 3));
        g.setColor(new Color(0, 0, 0, 80));                   // grietas: se nota que se puede romper
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D grieta = new Path2D.Double();
        grieta.moveTo(70, 6);
        grieta.lineTo(62, 28);
        grieta.lineTo(72, 44);
        grieta.lineTo(60, 62);
        g.draw(grieta);
        Path2D grieta2 = new Path2D.Double();
        grieta2.moveTo(18, 70);
        grieta2.lineTo(30, 80);
        grieta2.lineTo(26, 94);
        g.draw(grieta2);
        g.setColor(oscuro(col, 0.45));
        g.setStroke(new BasicStroke(3f));
        g.drawRect(1, 1, 126, 126);
        g.dispose();
        guardar(img, "nivel" + n + "_muro_rompible");
    }

    static void puerta() throws Exception {
        BufferedImage img = lienzo(128, 128);
        Graphics2D g = pincel(img);
        g.setColor(c(0x6D4C41));
        g.fillRect(0, 0, 128, 128);
        for (int x = 0; x < 128; x += 21) {                   // tablones
            g.setColor(new Color(0, 0, 0, 40));
            g.fillRect(x, 0, 2, 128);
        }
        RoundRectangle2D marco = new RoundRectangle2D.Double(14, 8, 100, 116, 28, 28);
        relleno(g, marco, c(0x5D4037), c(0x3E2723), 8, 124);
        borde(g, marco, c(0x2B1B15), 4);
        RoundRectangle2D hoja = new RoundRectangle2D.Double(24, 18, 80, 100, 20, 20);
        relleno(g, hoja, c(0xB58B6B), c(0x8D6648), 18, 118);
        borde(g, hoja, c(0x4E342E), 3);
        g.setColor(new Color(0, 0, 0, 55));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new java.awt.geom.Line2D.Double(64, 22, 64, 114));
        g.draw(new RoundRectangle2D.Double(32, 28, 26, 36, 8, 8));
        g.draw(new RoundRectangle2D.Double(70, 28, 26, 36, 8, 8));
        Ellipse2D perilla = new Ellipse2D.Double(78, 66, 14, 14);
        relleno(g, perilla, c(0xFFE082), c(0xFF8F00), 66, 80);
        borde(g, perilla, c(0x8A5A00), 1.5);
        g.dispose();
        guardar(img, "puerta");
    }

    static void trampa() throws Exception {
        BufferedImage img = lienzo(128, 128);
        Graphics2D g = pincel(img);
        RoundRectangle2D placa = new RoundRectangle2D.Double(8, 8, 112, 112, 20, 20);
        relleno(g, placa, c(0x546E7A), c(0x263238), 8, 120);
        borde(g, placa, c(0x1B2428), 4);
        g.setColor(new Color(255, 82, 82, 55));
        g.fill(new Ellipse2D.Double(14, 14, 100, 100));
        for (int fila = 0; fila < 3; fila++) {
            for (int col = 0; col < 3; col++) {
                double cx = 30 + col * 34;
                double cy = 32 + fila * 34;
                Path2D pua = new Path2D.Double();
                pua.moveTo(cx - 11, cy + 14);
                pua.lineTo(cx, cy - 16);
                pua.lineTo(cx + 11, cy + 14);
                pua.closePath();
                g.setPaint(new GradientPaint((float) (cx - 11), 0, c(0xECEFF1), (float) (cx + 11), 0, c(0x90A4AE)));
                g.fill(pua);
                borde(g, pua, c(0x37474F), 2);
            }
        }
        g.dispose();
        guardar(img, "trampa");
    }

    // ============================================================== objetos

    interface Pintor { void pintar(Graphics2D g); }

    static void sprite(String nombre, Pintor p) throws Exception {
        BufferedImage img = lienzo(128, 128);
        Graphics2D g = pincel(img);
        p.pintar(g);
        g.dispose();
        guardar(img, nombre);
    }

    static void estrella(Graphics2D g, double cx, double cy, double re, double ri, int puntas) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < puntas * 2; i++) {
            double r = i % 2 == 0 ? re : ri;
            double a = -Math.PI / 2 + i * Math.PI / puntas;
            double x = cx + Math.cos(a) * r;
            double y = cy + Math.sin(a) * r;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        g.fill(p);
    }

    static void pintarMoneda(Graphics2D g) {
        Ellipse2D e = new Ellipse2D.Double(14, 14, 100, 100);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(50, 46), 82f, new float[]{0f, 0.55f, 1f},
                new Color[]{c(0xFFF3B0), c(0xF5C518), c(0xB8860B)}));
        g.fill(e);
        borde(g, e, c(0x8A6508), 6);
        borde(g, new Ellipse2D.Double(28, 28, 72, 72), c(0xB8860B, 170), 4);
        g.setColor(c(0xFFE082));
        estrella(g, 64, 66, 24, 10, 5);
        g.setColor(c(0xB8860B, 150));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Arc2D.Double(24, 24, 80, 80, 110, 70, Arc2D.OPEN));
        brillo(g, 42, 36, 12, 6, 140);
    }

    static void pintarPeluche(Graphics2D g) {
        Color pel = c(0xF48FB1), osc = c(0xAD5C78), claro = c(0xFFD1E1);
        Ellipse2D oreja1 = new Ellipse2D.Double(20, 8, 38, 38);
        Ellipse2D oreja2 = new Ellipse2D.Double(70, 8, 38, 38);
        Ellipse2D cuerpo = new Ellipse2D.Double(28, 66, 72, 56);
        Ellipse2D cabeza = new Ellipse2D.Double(22, 18, 84, 76);
        for (Ellipse2D o : new Ellipse2D[]{oreja1, oreja2}) {
            relleno(g, o, claro(pel, 0.15), pel, o.getY(), o.getY() + 38);
            borde(g, o, osc, 4);
            g.setColor(claro);
            g.fill(new Ellipse2D.Double(o.getX() + 9, o.getY() + 9, 20, 20));
        }
        relleno(g, cuerpo, claro(pel, 0.1), pel, 66, 122);
        borde(g, cuerpo, osc, 4);
        relleno(g, cabeza, claro(pel, 0.2), pel, 18, 94);
        borde(g, cabeza, osc, 4);
        Ellipse2D hocico = new Ellipse2D.Double(44, 54, 40, 30);
        relleno(g, hocico, c(0xFFEEF3), c(0xFFC1D6), 54, 84);
        borde(g, hocico, osc, 3);
        g.setColor(c(0x4A2A38));
        g.fill(new Ellipse2D.Double(57, 58, 14, 10));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(52, 64, 12, 12, 200, 140, Arc2D.OPEN));
        g.draw(new Arc2D.Double(64, 64, 12, 12, 200, 140, Arc2D.OPEN));
        g.setColor(c(0x1B1B1B));
        g.fill(new Ellipse2D.Double(42, 40, 11, 14));
        g.fill(new Ellipse2D.Double(75, 40, 11, 14));
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(45, 42, 4, 4));
        g.fill(new Ellipse2D.Double(78, 42, 4, 4));
        g.setColor(new Color(255, 90, 120, 90));
        g.fill(new Ellipse2D.Double(28, 66, 14, 9));
        g.fill(new Ellipse2D.Double(86, 66, 14, 9));
        Path2D mono = new Path2D.Double();                     // moñito
        mono.moveTo(64, 98);
        mono.lineTo(44, 88);
        mono.lineTo(44, 110);
        mono.closePath();
        mono.moveTo(64, 98);
        mono.lineTo(84, 88);
        mono.lineTo(84, 110);
        mono.closePath();
        g.setColor(c(0x4FC3F7));
        g.fill(mono);
        borde(g, mono, c(0x0277BD), 3);
        g.setColor(c(0x0288D1));
        g.fill(new Ellipse2D.Double(58, 92, 12, 12));
    }

    static void pintarZapato(Graphics2D g) {
        Path2D bota = new Path2D.Double();
        bota.moveTo(30, 18);
        bota.lineTo(70, 18);
        bota.lineTo(74, 62);
        bota.curveTo(92, 66, 108, 70, 118, 84);
        bota.lineTo(118, 102);
        bota.lineTo(20, 102);
        bota.lineTo(20, 76);
        bota.closePath();
        relleno(g, bota, c(0xA1735E), c(0x5D4037), 18, 102);
        borde(g, bota, c(0x3E2723), 5);
        RoundRectangle2D suela = new RoundRectangle2D.Double(14, 98, 110, 16, 10, 10);
        relleno(g, suela, c(0x4E342E), c(0x1B100D), 98, 114);
        borde(g, suela, c(0x1B100D), 3);
        Ellipse2D puntera = new Ellipse2D.Double(78, 74, 40, 26);
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(puntera);
        RoundRectangle2D puno = new RoundRectangle2D.Double(26, 12, 48, 14, 8, 8);
        relleno(g, puno, c(0xD7B49E), c(0xA1735E), 12, 26);
        borde(g, puno, c(0x3E2723), 3);
        g.setColor(Color.WHITE);                               // agujetas
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            g.draw(new java.awt.geom.Line2D.Double(54, 34 + i * 12, 68, 40 + i * 12));
            g.draw(new java.awt.geom.Line2D.Double(68, 34 + i * 12, 54, 40 + i * 12));
        }
        brillo(g, 40, 44, 6, 14, 60);
    }

    static void pintarChaleco(Graphics2D g) {
        Path2D v = new Path2D.Double();
        v.moveTo(34, 12);
        v.lineTo(52, 12);
        v.lineTo(64, 44);
        v.lineTo(76, 12);
        v.lineTo(94, 12);
        v.lineTo(114, 36);
        v.lineTo(100, 60);
        v.lineTo(100, 114);
        v.lineTo(28, 114);
        v.lineTo(28, 60);
        v.lineTo(14, 36);
        v.closePath();
        relleno(g, v, c(0xFFB300), c(0xE65100), 12, 114);
        borde(g, v, c(0x8A3B00), 5);
        Path2D cuello = new Path2D.Double();
        cuello.moveTo(52, 12);
        cuello.lineTo(64, 44);
        cuello.lineTo(76, 12);
        cuello.closePath();
        g.setColor(c(0xFFF3E0));
        g.fill(cuello);
        borde(g, cuello, c(0x8A3B00), 3);
        g.setColor(c(0xFFF3E0, 220));                           // franjas reflejantes
        g.fill(new Rectangle2D.Double(29, 66, 70, 8));
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{6, 6}, 0));
        g.draw(new java.awt.geom.Line2D.Double(64, 48, 64, 112));
        g.setStroke(new BasicStroke(3f));
        g.draw(new RoundRectangle2D.Double(34, 82, 24, 20, 6, 6));
        g.draw(new RoundRectangle2D.Double(70, 82, 24, 20, 6, 6));
        brillo(g, 40, 30, 5, 10, 70);
    }

    static void pintarJarrito(Graphics2D g, boolean mecha) {
        Path2D cuerpo = new Path2D.Double();
        cuerpo.moveTo(46, 40);
        cuerpo.curveTo(18, 52, 14, 96, 36, 114);
        cuerpo.lineTo(92, 114);
        cuerpo.curveTo(114, 96, 110, 52, 82, 40);
        cuerpo.closePath();
        relleno(g, cuerpo, c(0xE59A5F), c(0x9C4A1E), 40, 114);
        Shape recorte = g.getClip();
        g.setClip(cuerpo);                                      // decoración de barro pintado
        g.setColor(c(0xFFF3E0));
        g.fill(new Rectangle2D.Double(10, 66, 108, 7));
        g.fill(new Rectangle2D.Double(10, 90, 108, 6));
        g.setColor(c(0x5A2A10));
        Path2D greca = new Path2D.Double();
        greca.moveTo(10, 82);
        for (int x = 10; x < 118; x += 12) {
            greca.lineTo(x + 6, 76);
            greca.lineTo(x + 12, 82);
        }
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(greca);
        g.setClip(recorte);
        borde(g, cuerpo, c(0x5A2A10), 5);
        RoundRectangle2D cuello = new RoundRectangle2D.Double(46, 24, 36, 22, 8, 8);
        relleno(g, cuello, c(0xD98A52), c(0x9C4A1E), 24, 46);
        borde(g, cuello, c(0x5A2A10), 4);
        RoundRectangle2D labio = new RoundRectangle2D.Double(40, 16, 48, 14, 8, 8);
        relleno(g, labio, c(0xE59A5F), c(0xB5622B), 16, 30);
        borde(g, labio, c(0x5A2A10), 4);
        brillo(g, 34, 78, 6, 18, 70);
        if (mecha) {
            g.setColor(c(0xD7CCC8));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D cuerda = new Path2D.Double();
            cuerda.moveTo(64, 18);
            cuerda.curveTo(62, 8, 76, 10, 80, 2);
            g.draw(cuerda);
        } else {
            g.setColor(c(0xFFF59D));
            estrella(g, 100, 22, 11, 4, 4);
            estrella(g, 22, 34, 8, 3, 4);
        }
    }

    static void pintarOvillo(Graphics2D g) {
        Ellipse2D bola = new Ellipse2D.Double(12, 12, 104, 104);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(50, 46), 84f, new float[]{0f, 1f},
                new Color[]{c(0x90CAF9), c(0x3F6FA6)}));
        g.fill(bola);
        Shape clip = g.getClip();
        g.setClip(bola);
        g.setColor(new Color(255, 255, 255, 90));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -2; i < 6; i++) {
            g.draw(new Arc2D.Double(-10 + i * 18, 4, 70, 120, 110, 140, Arc2D.OPEN));
        }
        g.setColor(new Color(0, 0, 0, 45));
        for (int i = -2; i < 6; i++) {
            g.draw(new Arc2D.Double(-2 + i * 18, 4, 70, 120, 110, 140, Arc2D.OPEN));
        }
        g.setClip(clip);
        borde(g, bola, c(0x23466F), 5);
        g.setColor(c(0x64B5F6));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D hebra = new Path2D.Double();
        hebra.moveTo(98, 98);
        hebra.curveTo(120, 110, 100, 124, 80, 118);
        g.draw(hebra);
    }

    static void pintarCorazonAyuda(Graphics2D g) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(64, 66), 62f, new float[]{0f, 1f},
                new Color[]{new Color(255, 160, 190, 150), new Color(255, 160, 190, 0)}));
        g.fill(new Rectangle2D.Double(0, 0, 128, 128));
        Path2D h = new Path2D.Double();
        h.moveTo(64, 112);
        h.curveTo(8, 72, 6, 30, 38, 20);
        h.curveTo(54, 15, 64, 28, 64, 36);
        h.curveTo(64, 28, 74, 15, 90, 20);
        h.curveTo(122, 30, 120, 72, 64, 112);
        h.closePath();
        relleno(g, h, c(0xFF6B6B), c(0xC62828), 15, 112);
        borde(g, h, c(0x7F1D1D), 6);
        brillo(g, 42, 40, 11, 7, 160);
        g.setColor(Color.WHITE);                                  // cruz de "salud"
        g.fill(new RoundRectangle2D.Double(55, 42, 18, 46, 8, 8));
        g.fill(new RoundRectangle2D.Double(41, 56, 46, 18, 8, 8));
    }

    static void pintarReloj(Graphics2D g) {
        for (int lado = -1; lado <= 1; lado += 2) {               // campanitas
            Ellipse2D campana = new Ellipse2D.Double(64 + lado * 38 - 16, 6, 32, 28);
            relleno(g, campana, c(0xFFD54F), c(0xFF8F00), 6, 34);
            borde(g, campana, c(0x8A5A00), 3);
        }
        Ellipse2D aro = new Ellipse2D.Double(10, 14, 108, 108);
        relleno(g, aro, c(0x64B5F6), c(0x1565C0), 14, 122);
        borde(g, aro, c(0x0D3A7A), 5);
        Ellipse2D cara = new Ellipse2D.Double(20, 24, 88, 88);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(56, 56), 70f, new float[]{0f, 1f},
                new Color[]{Color.WHITE, c(0xDDE6F0)}));
        g.fill(cara);
        borde(g, cara, c(0x0D3A7A, 140), 2.5);
        g.setColor(c(0x37474F));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {                            // marcas de las horas
            double a = i * Math.PI / 6;
            double r1 = i % 3 == 0 ? 33 : 37;
            g.draw(new java.awt.geom.Line2D.Double(64 + Math.cos(a) * r1, 68 + Math.sin(a) * r1,
                    64 + Math.cos(a) * 41, 68 + Math.sin(a) * 41));
        }
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(64, 68, 64, 40));  // minutero
        g.setColor(c(0xD32F2F));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Line2D.Double(64, 68, 84, 78));  // horario
        g.setColor(c(0x37474F));
        g.fill(new Ellipse2D.Double(58, 62, 12, 12));
        brillo(g, 40, 38, 14, 6, 120);
    }

    static void pintarEscudo(Graphics2D g) {
        Path2D e = new Path2D.Double();
        e.moveTo(22, 14);
        e.lineTo(106, 14);
        e.curveTo(106, 70, 100, 96, 64, 120);
        e.curveTo(28, 96, 22, 70, 22, 14);
        e.closePath();
        relleno(g, e, c(0x64B5F6), c(0x1565C0), 14, 120);
        borde(g, e, c(0xECEFF1), 9);
        borde(g, e, c(0x0D3A7A), 3);
        g.setColor(c(0xFFE082));
        estrella(g, 64, 62, 30, 13, 5);
        borde(g, new java.awt.geom.Ellipse2D.Double(34, 32, 60, 60), new Color(255, 255, 255, 60), 2);
        brillo(g, 42, 34, 8, 14, 90);
    }

    static void pintarAlcance(Graphics2D g) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(64, 64), 62f, new float[]{0f, 1f},
                new Color[]{new Color(255, 190, 60, 160), new Color(255, 190, 60, 0)}));
        g.fill(new Rectangle2D.Double(0, 0, 128, 128));
        Path2D estallido = new Path2D.Double();
        for (int i = 0; i < 24; i++) {
            double r = i % 2 == 0 ? 58 : 34;
            double a = -Math.PI / 2 + i * Math.PI / 12;
            double x = 64 + Math.cos(a) * r, y = 64 + Math.sin(a) * r;
            if (i == 0) estallido.moveTo(x, y); else estallido.lineTo(x, y);
        }
        estallido.closePath();
        relleno(g, estallido, c(0xFFD54F), c(0xFF6D00), 6, 122);
        borde(g, estallido, c(0xB23C00), 4);
        g.setColor(c(0xFFF8E1));
        g.fill(new Ellipse2D.Double(38, 38, 52, 52));
        g.setColor(c(0xE64A19));
        g.fill(new RoundRectangle2D.Double(58, 44, 12, 40, 6, 6));   // signo "+"
        g.fill(new RoundRectangle2D.Double(44, 58, 40, 12, 6, 6));
    }

    static void escudoBurbuja() throws Exception {
        BufferedImage img = lienzo(256, 256);
        Graphics2D g = pincel(img);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(128, 128), 120f, new float[]{0f, 0.6f, 0.9f, 1f},
                new Color[]{new Color(120, 190, 255, 10), new Color(120, 190, 255, 40),
                        new Color(140, 210, 255, 150), new Color(180, 230, 255, 60)}));
        g.fill(new Ellipse2D.Double(8, 8, 240, 240));
        borde(g, new Ellipse2D.Double(10, 10, 236, 236), new Color(220, 245, 255, 200), 5);
        g.setColor(new Color(255, 255, 255, 140));
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(36, 36, 184, 184, 110, 60, Arc2D.OPEN));
        g.dispose();
        guardar(img, "escudo");
    }

    static void objetos() throws Exception {
        sprite("objeto_corazon", GeneradorAssets::pintarCorazonAyuda);
        sprite("objeto_reloj", GeneradorAssets::pintarReloj);
        sprite("objeto_escudo", GeneradorAssets::pintarEscudo);
        sprite("objeto_alcance", GeneradorAssets::pintarAlcance);
        escudoBurbuja();
        sprite("objeto_moneda", GeneradorAssets::pintarMoneda);
        sprite("objeto_peluche", GeneradorAssets::pintarPeluche);
        sprite("objeto_zapato", GeneradorAssets::pintarZapato);
        sprite("objeto_chaleco", GeneradorAssets::pintarChaleco);
        sprite("objeto_jarrito", g -> pintarJarrito(g, false));
        sprite("jarrito", g -> pintarJarrito(g, true));
    }

    // ============================================================= enemigos

    static void ojosEnojados(Graphics2D g, double y, double separacion) {
        for (int lado = -1; lado <= 1; lado += 2) {
            double cx = 64 + lado * separacion;
            Ellipse2D ojo = new Ellipse2D.Double(cx - 12, y, 24, 28);
            g.setColor(Color.WHITE);
            g.fill(ojo);
            borde(g, ojo, new Color(0, 0, 0, 120), 2.5);
            g.setColor(c(0x1B1B1B));
            g.fill(new Ellipse2D.Double(cx - 6 - lado * 1, y + 8, 12, 16));
            g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));   // cejas enojadas
            g.draw(new java.awt.geom.Line2D.Double(cx - lado * 16, y - 6 - 8, cx + lado * 12, y + 5));
        }
    }

    static void enemigos() throws Exception {
        sprite("enemigo_vigilante", g -> {
            Color col = c(0x8E24AA);
            for (int lado = -1; lado <= 1; lado += 2) {          // cuernitos
                Path2D cuerno = new Path2D.Double();
                cuerno.moveTo(64 + lado * 40, 30);
                cuerno.lineTo(64 + lado * 30, 2);
                cuerno.lineTo(64 + lado * 16, 28);
                cuerno.closePath();
                g.setColor(c(0xFFF3E0));
                g.fill(cuerno);
                borde(g, cuerno, c(0x6A1B4D), 4);
            }
            RoundRectangle2D cuerpo = new RoundRectangle2D.Double(12, 20, 104, 98, 40, 40);
            relleno(g, cuerpo, c(0xBA68C8), c(0x6A1B9A), 20, 118);
            borde(g, cuerpo, c(0x38006B), 5);
            g.setColor(c(0x38006B));
            ojosEnojados(g, 44, 22);
            g.setColor(new Color(0x2A0A3A));
            g.fill(new RoundRectangle2D.Double(36, 82, 56, 22, 10, 10));
            g.setColor(Color.WHITE);                              // dientes
            for (int i = 0; i < 4; i++) {
                Path2D d = new Path2D.Double();
                d.moveTo(40 + i * 12, 82);
                d.lineTo(46 + i * 12, 94);
                d.lineTo(52 + i * 12, 82);
                d.closePath();
                g.fill(d);
            }
            brillo(g, 34, 34, 12, 6, 90);
        });
        sprite("enemigo_rapido", g -> {
            for (int i = 0; i < 12; i++) {                        // picos alrededor
                double a = i * Math.PI / 6;
                Path2D pico = new Path2D.Double();
                pico.moveTo(64 + Math.cos(a - 0.2) * 44, 68 + Math.sin(a - 0.2) * 44);
                pico.lineTo(64 + Math.cos(a) * 62, 68 + Math.sin(a) * 62);
                pico.lineTo(64 + Math.cos(a + 0.2) * 44, 68 + Math.sin(a + 0.2) * 44);
                pico.closePath();
                g.setColor(c(0xB71C1C));
                g.fill(pico);
            }
            Ellipse2D cuerpo = new Ellipse2D.Double(18, 22, 92, 92);
            relleno(g, cuerpo, c(0xFF6F60), c(0xC62828), 22, 114);
            borde(g, cuerpo, c(0x7F0000), 5);
            g.setColor(c(0x7F0000));
            ojosEnojados(g, 48, 20);
            g.setColor(new Color(0x3A0000));
            g.fill(new Arc2D.Double(40, 78, 48, 30, 0, 180, Arc2D.CHORD));
            g.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) {
                Path2D d = new Path2D.Double();
                d.moveTo(48 + i * 14, 92);
                d.lineTo(54 + i * 14, 102);
                d.lineTo(60 + i * 14, 92);
                d.closePath();
                g.fill(d);
            }
            g.setColor(new Color(255, 255, 255, 120));            // líneas de velocidad
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Line2D.Double(2, 50, 18, 50));
            g.draw(new java.awt.geom.Line2D.Double(0, 70, 16, 70));
            brillo(g, 42, 36, 12, 6, 100);
        });
    }

    // ================================================================= fuego

    static void fuego() throws Exception {
        sprite("fuego", g -> {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(64, 72), 66f, new float[]{0f, 0.5f, 1f},
                    new Color[]{new Color(255, 235, 59, 240), new Color(255, 112, 0, 210), new Color(255, 60, 0, 0)}));
            g.fill(new Rectangle2D.Double(0, 0, 128, 128));
            double[][] llamas = {{64, 6, 34}, {34, 36, 24}, {94, 34, 24}, {50, 24, 22}, {80, 22, 22}};
            for (double[] l : llamas) {
                Path2D f = new Path2D.Double();
                f.moveTo(l[0], l[1]);
                f.curveTo(l[0] + l[2], l[1] + 40, l[0] + l[2] * 0.7, l[1] + 90, l[0], l[1] + 96);
                f.curveTo(l[0] - l[2] * 0.7, l[1] + 90, l[0] - l[2], l[1] + 40, l[0], l[1]);
                g.setColor(new Color(255, 143, 0, 200));
                g.fill(f);
            }
            Path2D medio = new Path2D.Double();
            medio.moveTo(64, 28);
            medio.curveTo(92, 66, 88, 100, 64, 108);
            medio.curveTo(40, 100, 36, 66, 64, 28);
            g.setColor(new Color(255, 213, 79, 235));
            g.fill(medio);
            Path2D centro = new Path2D.Double();
            centro.moveTo(64, 56);
            centro.curveTo(80, 80, 76, 100, 64, 104);
            centro.curveTo(52, 100, 48, 80, 64, 56);
            g.setColor(new Color(255, 255, 224, 245));
            g.fill(centro);
        });
    }

    // ======================================================== fondos de nivel

    static void fondoNivel(int n) throws Exception {
        int[][] paleta = {{0x7A4A62, 0x1F1218}, {0x7B5A4D, 0x1F140F}, {0x5F7A88, 0x121C24}, {0xB5683A, 0x2A140A}};
        BufferedImage img = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pincel(img);
        g.setPaint(new GradientPaint(0, 0, c(paleta[n - 1][0]), 0, 720, c(paleta[n - 1][1])));
        g.fillRect(0, 0, 1280, 720);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(820, 280), 620f, new float[]{0f, 1f},
                new Color[]{new Color(255, 220, 170, 90), new Color(255, 220, 170, 0)}));
        g.fillRect(0, 0, 1280, 720);
        // patrón de motivos pequeños
        Random r = new Random(n * 7L);
        for (int i = 0; i < 26; i++) {
            BufferedImage m = lienzo(128, 128);
            Graphics2D gm = pincel(m);
            motivo(n, gm);
            gm.dispose();
            double s = 40 + r.nextInt(60);
            g.setComposite(AlphaComposite.SrcOver.derive(0.07f));
            g.drawImage(m, r.nextInt(1280) - 30, r.nextInt(720) - 30, (int) s, (int) s, null);
        }
        // gran silueta del producto del municipio
        BufferedImage capa = lienzo(512, 512);
        Graphics2D gc = pincel(capa);
        gc.scale(4, 4);
        motivo(n, gc);
        gc.dispose();
        g.setComposite(AlphaComposite.SrcOver.derive(0.23f));
        g.drawImage(capa, 640, 90, 600, 600, null);
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(640, 360), 820f, new float[]{0.5f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 160)}));
        g.fillRect(0, 0, 1280, 720);
        g.dispose();
        guardar(img, "nivel" + n + "_fondo");
    }

    /** Producto típico de cada municipio. */
    static void motivo(int n, Graphics2D g) {
        switch (n) {
            case 1: pintarPeluche(g); break;
            case 2: pintarZapato(g); break;
            case 3: pintarOvillo(g); break;
            default: pintarJarrito(g, false);
        }
    }

    // ================================================================ sonidos

    /** Síntesis de la música y los efectos (sin librerías: ondas, ruido y envolventes). */
    static class Sonidos {
        enum Efecto {
            COLOCAR_BOMBA, EXPLOSION, MONEDA, OBJETO, GOLPE, AYUDA, ENEMIGO_ELIMINADO,
            NIVEL_COMPLETO, VICTORIA, DERROTA
        }

        static final float FREC = 22050f;
        static final AudioFormat FORMATO = new AudioFormat(FREC, 16, 1, true, false);

        static void generarTodo(File carpeta) throws Exception {
            for (Efecto e : Efecto.values()) {
                escribir(new File(carpeta, e.name().toLowerCase() + ".wav"), aBytes(sintetizar(e)));
            }
            for (int n = 1; n <= 4; n++) {
                escribir(new File(carpeta, "musica_nivel" + n + ".wav"), aBytes(componerMusica(n)));
            }
        }

        static void escribir(File archivo, byte[] datos) throws Exception {
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(datos), FORMATO, datos.length / 2),
                    AudioFileFormat.Type.WAVE, archivo);
        }

    // ------------------------------------------------- síntesis de efectos

            static float[] sintetizar(Efecto efecto) {
            switch (efecto) {
                case COLOCAR_BOMBA: {
                    float[] b = new float[muestras(0.18)];
                    barrido(b, 0, 180, 70, 0.16, 0.7, 0, 18);
                    ruido(b, 0, 0.03, 0.25, 60, 0.5f);
                    return b;
                }
                case EXPLOSION: {
                    float[] b = new float[muestras(0.8)];
                    ruido(b, 0, 0.8, 1.0, 4.5, 0.12f);          // retumbo
                    ruido(b, 0, 0.12, 0.6, 25, 0.6f);           // golpe inicial
                    barrido(b, 0, 95, 32, 0.8, 0.8, 0, 4);      // grave que cae
                    return b;
                }
                case MONEDA: {
                    float[] b = new float[muestras(0.26)];
                    nota(b, 0, 988, 0.07, 0.30, 1);
                    nota(b, muestras(0.07), 1319, 0.17, 0.30, 1);
                    return b;
                }
                case OBJETO: {
                    float[] b = new float[muestras(0.62)];
                    double[] f = {523.25, 659.25, 783.99, 1046.5};
                    for (int i = 0; i < f.length; i++) nota(b, muestras(0.09 * i), f[i], 0.2, 0.32, 2);
                    return b;
                }
                case AYUDA: {
                    float[] b = new float[muestras(0.75)];
                    double[] f = {659.25, 783.99, 987.77, 1318.5};
                    for (int i = 0; i < f.length; i++) nota(b, muestras(0.07 * i), f[i], 0.17, 0.28, 2);
                    nota(b, muestras(0.30), 1568.0, 0.42, 0.17, 0);
                    nota(b, muestras(0.30), 2093.0, 0.42, 0.09, 0);
                    return b;
                }
                case GOLPE: {
                    float[] b = new float[muestras(0.4)];
                    barrido(b, 0, 320, 90, 0.35, 0.45, 3, 3);
                    ruido(b, 0, 0.12, 0.4, 20, 0.4f);
                    return b;
                }
                case ENEMIGO_ELIMINADO: {
                    float[] b = new float[muestras(0.25)];
                    barrido(b, 0, 640, 180, 0.2, 0.35, 1, 6);
                    return b;
                }
                case NIVEL_COMPLETO: {
                    float[] b = new float[muestras(1.1)];
                    double[] f = {523.25, 659.25, 783.99, 1046.5};
                    for (int i = 0; i < f.length; i++) nota(b, muestras(0.12 * i), f[i], 0.14, 0.32, 1);
                    nota(b, muestras(0.5), 783.99, 0.55, 0.22, 2);
                    nota(b, muestras(0.5), 1046.5, 0.55, 0.22, 2);
                    nota(b, muestras(0.5), 1318.5, 0.55, 0.18, 2);
                    return b;
                }
                case VICTORIA: {
                    float[] b = new float[muestras(2.0)];
                    double[] f = {523.25, 659.25, 783.99, 1046.5, 1318.5, 1568.0};
                    for (int i = 0; i < f.length; i++) nota(b, muestras(0.13 * i), f[i], 0.16, 0.30, 1);
                    nota(b, muestras(0.85), 523.25, 1.0, 0.22, 2);
                    nota(b, muestras(0.85), 783.99, 1.0, 0.22, 2);
                    nota(b, muestras(0.85), 1046.5, 1.0, 0.22, 2);
                    nota(b, muestras(0.85), 1318.5, 1.0, 0.18, 2);
                    return b;
                }
                default: {   // DERROTA
                    float[] b = new float[muestras(1.5)];
                    double[] f = {392.0, 329.63, 261.63, 196.0};
                    for (int i = 0; i < f.length; i++) nota(b, muestras(0.28 * i), f[i], 0.4, 0.35, 2);
                    return b;
                }
            }
        }

        // ---------------------------------------------------- música de fondo

        /**
         * Melodía corta de 8 compases (estilo "chiptune" festivo). Cada nivel usa otro
         * tono y otro tempo: nivel 1 tranquilo, nivel 4 el más rápido.
         */
            static float[] componerMusica(int nivel) {
            int[] bpm = {116, 128, 140, 152};
            int[] transporte = {0, 2, 5, 7};
            int idx = Math.max(0, Math.min(3, nivel - 1));
            double paso = 60.0 / bpm[idx] / 2.0;          // una corchea
            int t = transporte[idx];

            // semitonos sobre Do central; -1 = silencio
            int[][] melodia = {
                    {4, 7, 7, 4, 7, 9, 7, -1}, {4, 7, 7, 4, 2, 4, 0, -1},
                    {9, 9, 7, 4, 7, 9, 11, 12}, {11, 9, 7, 4, 2, 0, 0, -1},
                    {9, 9, 9, 7, 9, 12, 9, -1}, {7, 7, 7, 4, 7, 9, 7, -1},
                    {5, 5, 7, 9, 12, 11, 9, 7}, {4, 7, 4, 2, 0, -1, 0, -1}
            };
            int[] bajos = {-12, -12, -7, -5, -7, -12, -7, -5};   // C C F G | F C F G

            int pasos = melodia.length * 8;
            float[] b = new float[muestras(pasos * paso)];
            for (int c = 0; c < melodia.length; c++) {
                for (int p = 0; p < 8; p++) {
                    int s = c * 8 + p;
                    int inicio = muestras(s * paso);
                    if (melodia[c][p] >= 0) {
                        nota(b, inicio, frecuencia(melodia[c][p] + t), paso * 0.9, 0.17, 1);
                    }
                    if (p % 2 == 0) {       // bajo "oom-pah": raíz y quinta
                        int semitono = bajos[c] + (p % 4 == 2 ? 7 : 0) + t;
                        nota(b, inicio, frecuencia(semitono), paso * 1.8, 0.28, 2);
                    }
                    if (p % 4 == 0) {       // bombo
                        barrido(b, inicio, 120, 50, 0.10, 0.32, 0, 20);
                    }
                    if (p % 2 == 1) {       // platillo cerrado
                        ruido(b, inicio, 0.03, 0.07, 60, 0.9f);
                    }
                }
            }
            return b;
        }

            static double frecuencia(int semitonosSobreDoCentral) {
            return 261.63 * Math.pow(2, semitonosSobreDoCentral / 12.0);
        }

        // ------------------------------------------------- bloques de síntesis

            static int muestras(double segundos) {
            return (int) Math.round(segundos * FREC);
        }

        /** tipo: 0 seno, 1 cuadrada (25 %), 2 triangular, 3 sierra. */
            static double onda(int tipo, double fase) {
            double f = fase - Math.floor(fase);
            switch (tipo) {
                case 0: return Math.sin(2 * Math.PI * f);
                case 1: return f < 0.25 ? 1 : -1;
                case 2: return 4 * Math.abs(f - 0.5) - 1;
                default: return 2 * f - 1;
            }
        }

        /** Nota con ataque y caída suaves para que no suenen "clics". */
            static void nota(float[] buf, int inicio, double freq, double dur, double vol, int tipo) {
            int n = muestras(dur);
            int ataque = Math.min(muestras(0.004), n / 2);
            int caida = Math.min(muestras(0.04), n / 2);
            for (int i = 0; i < n && inicio + i < buf.length; i++) {
                double env = 1;
                if (i < ataque) env = i / (double) ataque;
                if (i > n - caida) env = Math.min(env, (n - i) / (double) caida);
                buf[inicio + i] += (float) (onda(tipo, freq * i / FREC) * vol * env);
            }
        }

        /** Sonido que cambia de frecuencia f0 -> f1; "caida" mayor = se apaga más rápido. */
            static void barrido(float[] buf, int inicio, double f0, double f1, double dur,
                                    double vol, int tipo, double caida) {
            int n = muestras(dur);
            double fase = 0;
            for (int i = 0; i < n && inicio + i < buf.length; i++) {
                double t = i / (double) n;
                double freq = f0 + (f1 - f0) * t;
                fase += freq / FREC;
                double env = Math.exp(-caida * t) * Math.min(1, i / (double) muestras(0.002) + 0.0001);
                buf[inicio + i] += (float) (onda(tipo, fase) * vol * env);
            }
        }

        /** Ruido blanco filtrado (filtro < 1 = más grave) con caída exponencial. */
            static void ruido(float[] buf, int inicio, double dur, double vol, double caida, float filtro) {
            java.util.Random azar = new java.util.Random(7);
            int n = muestras(dur);
            float previo = 0;
            for (int i = 0; i < n && inicio + i < buf.length; i++) {
                float crudo = azar.nextFloat() * 2 - 1;
                previo += filtro * (crudo - previo);
                double env = Math.exp(-caida * i / (double) n);
                buf[inicio + i] += (float) (previo * vol * env);
            }
        }

        /** float [-1,1] -> PCM de 16 bits (little endian), con tope para evitar distorsión. */
            static byte[] aBytes(float[] buf) {
            byte[] out = new byte[buf.length * 2];
            for (int i = 0; i < buf.length; i++) {
                float v = Math.max(-1f, Math.min(1f, buf[i])) * 0.9f;
                short s = (short) Math.round(v * 32767);
                out[2 * i] = (byte) (s & 0xFF);
                out[2 * i + 1] = (byte) ((s >> 8) & 0xFF);
            }
            return out;
        }
    }
}
