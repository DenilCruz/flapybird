package com.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * AppFlappyBird — Versión Parcial 1
 * ===================================
 * Juego estilo Flappy Bird con OpenGL 3.3 Core Profile (LWJGL).
 *
 * Requerimientos implementados:
 * R2.1 - Pájaro compuesto: cuerpo, ala animada, pico, cola, ojo con pupila
 * R2.2 - Dos jugadores simultáneos (P1=SPACE, P2=W/↑), tuberías compartidas
 * R2.3 - Velocidad progresiva por nivel (nivel 0-10 según mejor puntaje)
 * R2.4 - Fondo con cielo degradado, nubes parallax, montañas, suelo,
 * pantallas de inicio/game-over, sonidos sintéticos PCM
 *
 * Organización:
 * AppFlappyBird → orquestador (game loop, ventana)
 * GameState → lógica del juego (FSM, física, colisiones, dificultad)
 * Bird → estado de cada pájaro
 * BirdRenderer → dibujo geométrico del pájaro
 * Renderer → recursos OpenGL (shaders, VAOs, draw helpers)
 * InputManager → teclado con detección de flanco
 * SoundManager → efectos de sonido PCM sintéticos
 * Pipe → modelo de tubería
 */
public class AppFlappyBird {

    // ── Configuración de ventana ──────────────────────────────────────────
    private static final int ANCHO = 900;
    private static final int ALTO = 700;

    // ── Recursos GLFW / OpenGL ────────────────────────────────────────────
    private long window;
    private Renderer renderer;
    private BirdRenderer birdRenderer;
    private InputManager input;
    private GameState estado;

    // ── Fondo: nubes en parallax ──────────────────────────────────────────
    /** Posiciones X de las nubes (se desplazan lentamente a la izquierda). */
    private final float[] nubeX = { 0.8f, 0.2f, -0.3f, -0.7f, 0.5f, -0.1f };
    private final float[] nubeY = { 0.72f, 0.60f, 0.78f, 0.65f, 0.50f, 0.70f };
    private final float[] nubeW = { 0.30f, 0.22f, 0.28f, 0.24f, 0.20f, 0.26f };
    private static final float SPEED_NUBE = 0.09f; // velocidad de parallax

    // ─────────────────────────────────────────────────────────────────────

    /** Punto de entrada de la aplicación. */
    public static void main(String[] args) {
        new AppFlappyBird().run();
    }

    public void run() {
        iniciarVentana();
        iniciarRecursos();
        bucle();
        limpiar();
    }

    // ── Inicialización GLFW ───────────────────────────────────────────────

    private void iniciarVentana() {
        if (!GLFW.glfwInit())
            throw new IllegalStateException("No se pudo iniciar GLFW");

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird – 2 Jugadores", 0, 0);
        if (window == 0)
            throw new RuntimeException("No se pudo crear la ventana");

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1); // VSync
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();
    }

    private void iniciarRecursos() {
        renderer = new Renderer();
        renderer.init();
        birdRenderer = new BirdRenderer(renderer);
        input = new InputManager(window);
        estado = new GameState();
        actualizarTitulo();
    }

    // ── Bucle principal ───────────────────────────────────────────────────

    private void bucle() {
        float tPrevio = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt = Math.min(ahora - tPrevio, 0.033f); // cap a ~30 fps mínimo
            tPrevio = ahora;

            input.poll();
            procesarInput();
            estado.actualizar(dt);
            moverNubes(dt);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // ── Lógica de entrada ─────────────────────────────────────────────────

    private void procesarInput() {
        if (input.salir) {
            GLFW.glfwSetWindowShouldClose(window, true);
            return;
        }

        switch (estado.estado) {
            case JUGANDO -> {
                if (input.salto1) {
                    estado.pajaro1.saltar();
                    SoundManager.play(SoundManager.Sound.SALTO);
                }
                if (input.salto2) {
                    estado.pajaro2.saltar();
                    SoundManager.play(SoundManager.Sound.SALTO);
                }
                if (input.salto3) {
                    estado.pajaro3.saltar();
                    SoundManager.play(SoundManager.Sound.SALTO);
                }
                actualizarTitulo();
            }
            case GAME_OVER -> {
                if ((input.reiniciar || input.cualquierSalto) && estado.puedeReiniciar()) {
                    estado.reiniciar();
                    actualizarTitulo();
                }
            }
        }
    }

    // ── Parallax de nubes ─────────────────────────────────────────────────

    private void moverNubes(float dt) {
        float vel = (estado.estado == GameState.Estado.GAME_OVER)
                ? SPEED_NUBE * 0.3f
                : SPEED_NUBE;
        for (int i = 0; i < nubeX.length; i++) {
            nubeX[i] -= vel * dt;
            if (nubeX[i] + nubeW[i] < -1.1f) {
                nubeX[i] = 1.1f; // reaparece por la derecha
            }
        }
    }

    // ── Renderizado ───────────────────────────────────────────────────────

    private void render() {
        GL11.glClearColor(0.18f, 0.42f, 0.78f, 1.0f); // azul oscuro de fondo
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        renderer.activar();

        dibujarFondo();
        dibujarTuberias();
        dibujarPajaros();
    }

    // ── Capas del fondo ───────────────────────────────────────────────────

    private void dibujarFondo() {
        // Cielo degradado: franjas horizontales de azul oscuro a azul claro
        renderer.drawRect(0f, 0.75f, 2f, 0.50f, 0.15f, 0.38f, 0.75f);
        renderer.drawRect(0f, 0.30f, 2f, 0.50f, 0.20f, 0.50f, 0.85f);
        renderer.drawRect(0f, -0.10f, 2f, 0.50f, 0.25f, 0.62f, 0.90f);
        renderer.drawRect(0f, -0.50f, 2f, 0.50f, 0.30f, 0.70f, 0.92f);

        // Nubes (óvalos anchos y bajos, color blanco suave)
        for (int i = 0; i < nubeX.length; i++) {
            renderer.drawRect(nubeX[i], nubeY[i], nubeW[i], 0.07f,
                    0.95f, 0.96f, 0.98f);
            renderer.drawRect(nubeX[i] - 0.05f, nubeY[i] + 0.03f,
                    nubeW[i] * 0.6f, 0.05f,
                    0.95f, 0.96f, 0.98f);
        }

        // Montañas lejanas (triángulos grises/morados, capa trasera)
        renderer.drawTriangle(-0.60f, -0.45f, 0.60f, 0.55f, 1.57f, 0.42f, 0.2f, 0f);
        renderer.drawTriangle(0.10f, -0.42f, 0.55f, 0.52f, 1.57f, 0.4f, 0.22f, 0f);
        renderer.drawTriangle(0.70f, -0.44f, 0.50f, 0.48f, 1.57f, 0.45f, 0.2f, 0f);
        renderer.drawTriangle(-0.15f, -0.40f, 0.45f, 0.45f, 1.57f, 0.4f, 0.28f, 0f);

        // Montañas cercanas (triángulos verdes oscuros, capa delantera)
        renderer.drawTriangle(-0.80f, -0.60f, 0.70f, 0.50f, 1.57f, 0.15f, 0.42f, 0.20f);
        renderer.drawTriangle(0.00f, -0.58f, 0.65f, 0.48f, 1.57f, 0.18f, 0.45f, 0.22f);
        renderer.drawTriangle(0.75f, -0.60f, 0.62f, 0.46f, 1.57f, 0.14f, 0.40f, 0.18f);

        // Suelo: franja verde oscura en la parte inferior
        renderer.drawRect(0f, -0.94f, 2f, 0.14f, 0.12f, 0.35f, 0.10f);
        // Línea de hierba (franja más clara encima del suelo)
        renderer.drawRect(0f, -0.88f, 2f, 0.02f, 0.22f, 0.60f, 0.18f);
    }

    // ── Tuberías ──────────────────────────────────────────────────────────

    private void dibujarTuberias() {
        float ga = estado.gapAlto;

        for (Pipe p : estado.tuberias) {
            float gapTop = p.gapCentroY + ga * 0.5f;
            float gapBottom = p.gapCentroY - ga * 0.5f;

            // Sombra/borde de la tubería (quad ligeramente más ancho y oscuro)
            float borde = GameState.TUBERIA_ANCHO + 0.012f;

            // Parte superior
            float hSup = 1.0f - gapTop;
            if (hSup > 0f) {
                float yCentSup = gapTop + hSup * 0.5f;
                renderer.drawRect(p.x, yCentSup, borde, hSup, 0f, 0f, 0f);
                renderer.drawRect(p.x, yCentSup, GameState.TUBERIA_ANCHO, hSup, 0.58f, 0.48f, 0.82f);
                // Capuchón superior (rectángulo más ancho en la boca de la tubería)
                renderer.drawRect(p.x, gapTop + 0.035f,
                        GameState.TUBERIA_ANCHO + 0.035f, 0.06f,
                        0f, 0f, 0);
                renderer.drawRect(p.x, gapTop + 0.035f,
                        GameState.TUBERIA_ANCHO + 0.022f, 0.05f,
                        0.58f, 0.48f, 0.82f);
            }

            // Parte inferior
            float hInf = gapBottom + 1.0f;
            if (hInf > 0f) {
                float yCentInf = -1.0f + hInf * 0.5f;
                renderer.drawRect(p.x, yCentInf, borde, hInf, 0.08f, 0.42f, 0.10f);
                renderer.drawRect(p.x, yCentInf, GameState.TUBERIA_ANCHO, hInf, 0.18f, 0.68f, 0.22f);
                // Capuchón inferior
                renderer.drawRect(p.x, gapBottom - 0.035f,
                        GameState.TUBERIA_ANCHO + 0.035f, 0.06f,
                        0.08f, 0.45f, 0.12f);
                renderer.drawRect(p.x, gapBottom - 0.035f,
                        GameState.TUBERIA_ANCHO + 0.022f, 0.05f,
                        0.22f, 0.72f, 0.26f);
            }
        }
    }

    // ── Pájaros ───────────────────────────────────────────────────────────

    private void dibujarPajaros() {
        // En game over ambos pájaros se muestran fijos (pantalla congelada).
        boolean gameOver = estado.estado == GameState.Estado.GAME_OVER;

        // P2 detrás de P1 (se dibuja primero)
        if (gameOver || !estado.pajaro2.vivo || estado.pajaro2.visible) {
            birdRenderer.dibujar(estado.pajaro2);
        }
        if (gameOver || !estado.pajaro1.vivo || estado.pajaro1.visible) {
            birdRenderer.dibujar(estado.pajaro1);
        }
        if (gameOver || !estado.pajaro3.vivo || estado.pajaro3.visible) {
            birdRenderer.dibujar(estado.pajaro3);
        }
    }

    // ── Título de ventana ─────────────────────────────────────────────────

    private void actualizarTitulo() {
        String base = String.format("Flappy Bird 2P | P1:%d  P2:%d | P3:%d | Nivel %d | Vel:%.2f" ,
                estado.pajaro1.puntaje, estado.pajaro2.puntaje,
                estado.pajaro3.puntaje, estado.nivel, estado.velocidadActual);
    

        String sufijo = estado.estado == GameState.Estado.GAME_OVER
                ? " | GAME OVER – SPACE/R para reiniciar"
                : "";
                
        GLFW.glfwSetWindowTitle(window, base + sufijo);

    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    private void limpiar() {
        renderer.cleanup();
        SoundManager.shutdown();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
