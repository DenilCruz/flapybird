package com.graphics;

/**
 * tubería: posición horizontal, centro del hueco,
 * y seguimiento de puntuación por jugador para evitar doble conteo.
 */
public class Pipe {
    /** Posición horizontal del centro de la tubería. */
    public float x;
    /** Centro vertical del hueco entre las dos partes de la tubería. */
    public float gapCentroY;
    /** Indica si el Jugador 1 ya pasó por esta tubería. */
    public boolean scoredP1;
    /** Indica si el Jugador 2 ya pasó por esta tubería. */
    public boolean scoredP2;

    /** constructor de la clase Pipe */
    public Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
        this.scoredP1 = false;
        this.scoredP2 = false;
    }
}
