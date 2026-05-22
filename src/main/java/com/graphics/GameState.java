package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Estado global del juego: máquina de estados (FSM), tuberías,
 * dificultad progresiva y ambos pájaros.
 *
 * Estados:
 * JUGANDO → GAME_OVER → JUGANDO (tras reiniciar)
 */
public class GameState {

    /** Fases del juego. */
    public enum Estado {
        JUGANDO, GAME_OVER
    }

    // ── Dimensiones de las tuberías ───────────────────────────────────────
    public static final float TUBERIA_ANCHO = 0.18f;
    public static final float GAP_MIN_CENTRO = -0.45f;
    public static final float GAP_MAX_CENTRO = 0.45f;

    // ── Parámetros de dificultad base ─────────────────────────────────────
    public static final float VEL_BASE = 0.62f;
    public static final float VEL_MAX = 1.50f;
    public static final float TIEMPO_BASE = 1.5f;
    public static final float TIEMPO_MIN = 0.85f;
    public static final float GAP_ALTO_BASE = 0.50f;
    public static final float GAP_ALTO_MIN = 0.30f;

    // ── Estado actual ─────────────────────────────────────────────────────
    public Estado estado = Estado.JUGANDO;

    // ── Jugadores ─────────────────────────────────────────────────────────
    /** Jugador 1: amarillo-dorado, tecla SPACE. */
    public final Bird pajaro1 = new Bird(-0.45f, 0.98f, 0.82f, 0.15f, "P1");
    /** Jugador 2: azul-celeste, tecla W / ARRIBA. */
    public final Bird pajaro2 = new Bird(-0.20f, 0.25f, 0.72f, 0.98f, "P2");

    public final Bird pajaro3 = new Bird(-0.0f, 0.56f, 0.34f, 0.78f, "P3");

    // ── Obstáculos ────────────────────────────────────────────────────────
    public final List<Pipe> tuberias = new ArrayList<>();

    // ── Temporización ─────────────────────────────────────────────────────
    public float timerSpawn = 0f;
    public float timerGameOver = 0f; // tiempo transcurrido desde el game over

    // ── Dificultad dinámica ───────────────────────────────────────────────
    public int nivel = 0;
    public float velocidadActual = VEL_BASE;
    public float tiempoEntreTuberias = TIEMPO_BASE;
    public float gapAlto = GAP_ALTO_BASE;

    // ── Tiempo global de juego (para animaciones) ─────────────────────────
    public float tiempoJuego = 0f;

    private final Random random = new Random();

    // ─────────────────────────────────────────────────────────────────────

    /** Reinicia la partida completa. */
    public void reiniciar() {
        pajaro1.reset();
        pajaro2.reset();
        pajaro3.reset();
        tuberias.clear();
        timerSpawn = 0f;
        timerGameOver = 0f;
        tiempoJuego = 0f;
        nivel = 0;
        velocidadActual = VEL_BASE;
        tiempoEntreTuberias = TIEMPO_BASE;
        gapAlto = GAP_ALTO_BASE;
        estado = Estado.JUGANDO;
    }

    /**
     * Avanza la lógica del juego un frame.
     * 
     * @param dt delta de tiempo en segundos
     */
    public void actualizar(float dt) {
        tiempoJuego += dt;

        if (estado == Estado.GAME_OVER) {
            timerGameOver += dt;
            // Sigue actualizando parpadeo de los pájaros muertos
            if (!pajaro1.vivo)
                pajaro1.actualizar(dt, tiempoJuego);
            if (!pajaro2.vivo)
                pajaro2.actualizar(dt, tiempoJuego);
            if (!pajaro3.vivo)
                pajaro3.actualizar(dt, tiempoJuego);

            return;
        }
        if (estado != Estado.JUGANDO)
            return;

        // Actualizar cada pájaro
        pajaro1.actualizar(dt, tiempoJuego);
        pajaro2.actualizar(dt, tiempoJuego);
        pajaro3.actualizar(dt, tiempoJuego);

        // Verificar colisión con suelo/techo
        if (pajaro1.vivo && pajaro1.fueraDeRango())
            pajaro1.vivo = false;
        if (pajaro2.vivo && pajaro2.fueraDeRango())
            pajaro2.vivo = false;
        if (pajaro3.vivo && pajaro3.fueraDeRango())
            pajaro3.vivo = false;

        // Generar nuevas tuberías
        timerSpawn += dt;
        if (timerSpawn >= tiempoEntreTuberias) {
            timerSpawn = 0f;
            spawnTuberia();
        }

        // Mover y comprobar tuberías
        Iterator<Pipe> it = tuberias.iterator();
        while (it.hasNext()) {
            Pipe p = it.next();
            p.x -= velocidadActual * dt;

            // Conteo de puntuación por jugador (flanco de cruce)
            if (pajaro1.vivo && !p.scoredP1 && p.x + TUBERIA_ANCHO * 0.5f < pajaro1.x) {
                p.scoredP1 = true;
                pajaro1.puntaje++;
                SoundManager.play(SoundManager.Sound.PUNTO);
            }
            if (pajaro2.vivo && !p.scoredP2 && p.x + TUBERIA_ANCHO * 0.5f < pajaro2.x) {
                p.scoredP2 = true;
                pajaro2.puntaje++;
                SoundManager.play(SoundManager.Sound.PUNTO);
            }
            if (pajaro3.vivo && !p.scoredP3 && p.x + TUBERIA_ANCHO * 0.5f < pajaro3.x) {
                p.scoredP3 = true;
                pajaro3.puntaje++;
                SoundManager.play(SoundManager.Sound.PUNTO);
            }

            // Colisión AABB para cada pájaro
            if (pajaro1.vivo && colisiona(pajaro1, p)) {
                pajaro1.vivo = false;
                SoundManager.play(SoundManager.Sound.MUERTE);
            }
            if (pajaro2.vivo && colisiona(pajaro2, p)) {
                pajaro2.vivo = false;
                SoundManager.play(SoundManager.Sound.MUERTE);
            }
            if (pajaro3.vivo && colisiona(pajaro3, p)) {
                pajaro3.vivo = false;
                SoundManager.play(SoundManager.Sound.MUERTE);
            }

            // Eliminar tuberías fuera de pantalla
            if (p.x + TUBERIA_ANCHO * 0.5f < -1.3f) {
                it.remove();
            }
        }

        // Actualizar nivel de dificultad según el mejor puntaje
        int mejorPuntaje = Math.max(pajaro1.puntaje, Math.max(pajaro2.puntaje, pajaro3.puntaje));
        nivel = Math.min(10, mejorPuntaje / 3);
        velocidadActual = Math.min(VEL_MAX, VEL_BASE + nivel * 0.088f);
        tiempoEntreTuberias = Math.max(TIEMPO_MIN, TIEMPO_BASE - nivel * 0.065f);
        gapAlto = Math.max(GAP_ALTO_MIN, GAP_ALTO_BASE - nivel * 0.020f);

        if (pajaro1.puntaje >= 5 || pajaro2.puntaje >= 5 || pajaro3.puntaje >= 5) {
            estado = Estado.GAME_OVER;
            timerGameOver = 0f;
            SoundManager.play(SoundManager.Sound.GAME_OVER);
        }

        // Verificar fin de juego (todos muertos)
        if (!pajaro1.vivo && !pajaro2.vivo && !pajaro3.vivo) {
            estado = Estado.GAME_OVER;
            timerGameOver = 0f;
            SoundManager.play(SoundManager.Sound.GAME_OVER);
        }
    }

    /** Genera una tubería nueva en el borde derecho con gap aleatorio. */
    private void spawnTuberia() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Pipe(1.2f, gapCentro));
    }

    /**
     * Detección de colisión AABB entre un pájaro y una tubería.
     * 
     * @return true si hay colisión
     */
    private boolean colisiona(Bird b, Pipe p) {
        float bL = b.x - Bird.ANCHO * 0.5f;
        float bR = b.x + Bird.ANCHO * 0.5f;
        float bB = b.y - Bird.ALTO * 0.5f;
        float bT = b.y + Bird.ALTO * 0.5f;

        float pL = p.x - TUBERIA_ANCHO * 0.5f;
        float pR = p.x + TUBERIA_ANCHO * 0.5f;

        boolean overlapX = bR > pL && bL < pR;
        if (!overlapX)
            return false;

        float gapTop = p.gapCentroY + gapAlto * 0.5f;
        float gapBottom = p.gapCentroY - gapAlto * 0.5f;
        return bT > gapTop || bB < gapBottom;
    }

    /**
     * @return true si se puede reiniciar (game over y transcurrió al menos 1
     *         segundo).
     */

    public boolean puedeReiniciar() {
        return estado == Estado.GAME_OVER && timerGameOver > 1.0f;
    }
}
