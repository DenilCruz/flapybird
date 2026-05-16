package com.graphics;

import org.lwjgl.glfw.GLFW;

/**
 * Lee el estado del teclado una vez por frame y expone eventos de flanco
 * (pressed = solo el primer frame en que la tecla se presiona).
 *
 * Controles:
 *   Jugador 1 → SPACE
 *   Jugador 2 → W  o  FLECHA ARRIBA
 *   Reiniciar → R  o  ENTER
 *   Salir     → ESC
 */
public class InputManager {

    private final long window;

    // Estado anterior (frame previo)
    private boolean prevSpace, prevW, prevUp, prevR, prevEnter;

    // ── Eventos del frame actual (calculados en poll()) ────────────────────
    /** Jugador 1 presionó saltar este frame. */
    public boolean salto1;
    /** Jugador 2 presionó saltar este frame. */
    public boolean salto2;
    /** Alguno de los dos jugadores saltó este frame. */
    public boolean cualquierSalto;
    /** Se solicitó reiniciar este frame. */
    public boolean reiniciar;
    /** Tecla ESC presionada (cierre de ventana). */
    public boolean salir;

    public InputManager(long window) {
        this.window = window;
    }

    /**
     * Debe llamarse UNA vez al comienzo de cada frame,
     * antes de consultar cualquier propiedad.
     */
    public void poll() {
        boolean space = tecla(GLFW.GLFW_KEY_SPACE);
        boolean w     = tecla(GLFW.GLFW_KEY_W);
        boolean up    = tecla(GLFW.GLFW_KEY_UP);
        boolean r     = tecla(GLFW.GLFW_KEY_R);
        boolean enter = tecla(GLFW.GLFW_KEY_ENTER);

        salto1         = space && !prevSpace;
        salto2         = (w && !prevW) || (up && !prevUp);
        cualquierSalto = salto1 || salto2;
        reiniciar      = (r && !prevR) || (enter && !prevEnter);
        salir          = tecla(GLFW.GLFW_KEY_ESCAPE);

        prevSpace = space;
        prevW     = w;
        prevUp    = up;
        prevR     = r;
        prevEnter = enter;
    }

    private boolean tecla(int keyCode) {
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }
}
