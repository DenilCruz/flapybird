package com.graphics;

/**
 * Estado completo de un pájaro jugador.
 * Incluye posición, física, animaciones (aleteo, parpadeo) y puntaje.
 */
public class Bird {

    // ── Dimensiones del pájaro (en NDC) ──────────────────────────────────
    public static final float ANCHO = 0.10f;
    public static final float ALTO = 0.10f;

    // ── Constantes de física ──────────────────────────────────────────────
    public static final float GRAVEDAD = -1.9f;
    public static final float IMPULSO_SALTO = 0.85f;
    public static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // ── Identificación y color ────────────────────────────────────────────
    public final String nombre;
    public final float cr, cg, cb; // color principal (RGB)

    // ── Física ───────────────────────────────────────────────────────────
    public final float x; // posición horizontal fija
    public float y;
    public float velY;

    // ── Estado ───────────────────────────────────────────────────────────
    public boolean vivo;
    public int puntaje;

    // ── Animación ────────────────────────────────────────────────────────
    /** Ángulo del ala (oscila con seno del tiempo de juego). */
    public float anguloAla;
    /** Temporizador de parpadeo al morir. */
    public float timerParpadeo;
    /** Visibilidad durante parpadeo. */
    public boolean visible = true;

    /**
     * @param x      posición horizontal fija en NDC
     * @param cr     componente rojo del color
     * @param cg     componente verde del color
     * @param cb     componente azul del color
     * @param nombre etiqueta del jugador (P1 / P2)
     */
    public Bird(float x, float cr, float cg, float cb, String nombre) {
        this.x = x;
        this.cr = cr;
        this.cg = cg;
        this.cb = cb;
        this.nombre = nombre;
        reset();
    }

    /** Reinicia el pájaro a su estado inicial (posición y=0, vivo, score=0). */
    public void reset() {
        y = 0.0f;
        velY = 0.0f;
        vivo = true;
        puntaje = 0;
        anguloAla = 0f;
        timerParpadeo = 0f;
        visible = true;
    }

    /** Aplica el impulso de salto si el pájaro está vivo. */
    public void saltar() {
        if (vivo) {
            velY = IMPULSO_SALTO;
        }
    }

    /**
     * Actualiza física y animaciones del pájaro.
     * 
     * @param dt     delta de tiempo en segundos
     * @param tiempo tiempo global de juego (para animación del ala)
     */
    public void actualizar(float dt, float tiempo) {
        if (!vivo) {
            // Parpadeo rápido al morir (10 Hz)
            timerParpadeo += dt;
            visible = ((int) (timerParpadeo * 10) % 2) == 0;
            return;
        }

        // Animación del ala: oscilación sinusoidal
        anguloAla = (float) Math.sin(tiempo * 8.0) * 0.35f;

        // Física de gravedad
        velY += GRAVEDAD * dt;
        if (velY < VELOCIDAD_MAX_CAIDA)
            velY = VELOCIDAD_MAX_CAIDA;
        y += velY * dt;
    }

    /** @return ángulo de inclinación en radianes según velocidad vertical. */

    public float obtenerInclinacion() {
        // Mapea velocidad (VELOCIDAD_MAX_CAIDA..IMPULSO_SALTO) a (-0.5..+0.5) rad
        float rango = IMPULSO_SALTO - VELOCIDAD_MAX_CAIDA;
        float norm = (velY - VELOCIDAD_MAX_CAIDA) / rango; // 0..1
        return (norm - 0.5f) * 0.7f;
    }

    /** @return true si el pájaro toca el techo o el suelo. */
    public boolean fueraDeRango() {
        float top = y + ALTO * 0.5f;
        float bot = y - ALTO * 0.5f;
        return top >= 0.92f || bot <= -0.88f; // márgenes para suelo/techo visual
    }
}
