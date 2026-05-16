package com.graphics;

/**
 * Dibuja un pájaro compuesto por varias figuras geométricas OpenGL:
 *
 *   - Cuerpo     → rectángulo (color principal del jugador)
 *   - Ala        → rectángulo animado con seno del tiempo (aleteo)
 *   - Pico       → triángulo apuntando a la derecha
 *   - Cola       → triángulo apuntando a la izquierda
 *   - Ojo blanco → círculo (triangle-fan)
 *   - Pupila     → círculo más pequeño (negro)
 *
 * Toda la composición rota coherentemente según la velocidad vertical
 * del pájaro (Bird.obtenerInclinacion()).
 *
 * La rotación de cada parte se implementa calculando el offset local
 * rotado en CPU para obtener la posición mundial, y luego pasando el
 * mismo ángulo al shader para rotar la figura sobre su propio centro.
 */
public class BirdRenderer {

    private final Renderer r;

    public BirdRenderer(Renderer renderer) {
        this.r = renderer;
    }

    /**
     * Dibuja el pájaro completo en su posición actual.
     * @param bird  estado del pájaro a dibujar
     */
    public void dibujar(Bird bird) {
        if (!bird.visible) return; // parpadeo al morir

        float bx   = bird.x;
        float by   = bird.y;
        float tilt = bird.obtenerInclinacion();
        float ala  = bird.anguloAla; // oscila entre -0.35 y +0.35 rad

        // ── Cuerpo principal ──────────────────────────────────────────────
        r.drawRect(bx, by, Bird.ANCHO, Bird.ALTO, tilt,
                   bird.cr, bird.cg, bird.cb);

        // ── Ala (ligeramente encima del cuerpo, animada) ──────────────────
        // Offset local del ala antes de rotar: arriba del centro
        float alaLocX = 0.0f;
        float alaLocY = Bird.ALTO * 0.28f;
        float[] wPos  = rotar(alaLocX, alaLocY, tilt);
        float alaH    = Bird.ALTO * 0.28f * (1f + 0.5f * (float)Math.abs(Math.sin(ala * 2)));
        r.drawRect(bx + wPos[0], by + wPos[1],
                   Bird.ANCHO * 0.70f, alaH,
                   tilt + ala,
                   Math.min(1f, bird.cr + 0.15f),
                   Math.min(1f, bird.cg + 0.15f),
                   Math.min(1f, bird.cb + 0.15f));

        // ── Pico (triángulo apuntando a la derecha) ───────────────────────
        float picLocX = Bird.ANCHO * 0.58f;
        float picLocY = Bird.ALTO * 0.05f;
        float[] pPos  = rotar(picLocX, picLocY, tilt);
        r.drawTriangle(bx + pPos[0], by + pPos[1],
                       Bird.ANCHO * 0.45f, Bird.ALTO * 0.38f,
                       tilt, 0.95f, 0.45f, 0.05f);

        // ── Cola (triángulo apuntando a la izquierda, rotado 180°) ────────
        float colLocX = -Bird.ANCHO * 0.58f;
        float colLocY =  Bird.ALTO * 0.00f;
        float[] cPos  = rotar(colLocX, colLocY, tilt);
        r.drawTriangle(bx + cPos[0], by + cPos[1],
                       Bird.ANCHO * 0.40f, Bird.ALTO * 0.32f,
                       tilt + (float)Math.PI, // girar 180° para apuntar izq
                       0.90f, 0.40f, 0.05f);

        // ── Ojo (blanco) ─────────────────────────────────────────────────
        float ojoLocX = Bird.ANCHO * 0.22f;
        float ojoLocY = Bird.ALTO * 0.15f;
        float[] oPos  = rotar(ojoLocX, ojoLocY, tilt);
        float radioOjo = Bird.ANCHO * 0.17f;
        r.drawCircle(bx + oPos[0], by + oPos[1],
                     radioOjo, 1f, 1f, 1f);

        // ── Pupila (negro, ligeramente desplazada hacia adelante) ─────────
        float pupLocX = ojoLocX + Bird.ANCHO * 0.05f;
        float pupLocY = ojoLocY - Bird.ALTO * 0.02f;
        float[] puPos = rotar(pupLocX, pupLocY, tilt);
        r.drawCircle(bx + puPos[0], by + puPos[1],
                     radioOjo * 0.52f, 0.08f, 0.08f, 0.08f);
    }

    /**
     * Rota un punto local (lx, ly) por el ángulo dado.
     * @return {rx, ry} coordenadas rotadas
     */
    private float[] rotar(float lx, float ly, float angulo) {
        float c = (float) Math.cos(angulo);
        float s = (float) Math.sin(angulo);
        return new float[]{ c * lx - s * ly, s * lx + c * ly };
    }
}
