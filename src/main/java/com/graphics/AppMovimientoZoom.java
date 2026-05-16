package com.graphics;

import java.nio.FloatBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;

public class AppMovimientoZoom 
{
    // Referencia a la ventana
    private long window;
    
    // Programa de shaders
    private int programa;
    
    // VAO y VBO
    private int vao;
    private int vbo;
    
    // Tamaño de la ventana
    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    // Uniform locations
    private int uOffsetLocation;    // Para movimiento
    private int uZoomLocation;       // Para zoom

    // Variables de movimiento
    private float offsetX = 0.0f;   
    private float offsetY = 0.0f;  
    private static final float VELOCIDAD_MOV = 1.2f;    
    private static final float LIMITE = 0.9f;    

    // Variables de zoom
    private float zoom = 1.0f;   
    private static final float ZOOM_MIN = 0.25f;    
    private static final float ZOOM_MAX = 3.00f;   
    private static final float VELOCIDAD_ZOOM = 1.25f; 

    public void run(){
        init();
        loop();
        cleanup();
    }

    //===========================================
    // Inicialización
    //===========================================
    private void init(){
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!GLFW.glfwInit()){
            throw new IllegalStateException("No se pudo inicializar el GLFW");  
        }
        
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        
        // Crear la ventana
        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Movimiento + Zoom", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        // Cerrar ventana con ESC
        GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(w, true);
            }
        });

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        crearShaders();
        crearTriangulo();
    } 

    private void crearShaders() {
        // Vertex Shader - combina movimiento (offset) y zoom
        String vertexSrc = 
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "uniform vec2 uOffset;\n" +
            "uniform float uZoom;\n" +
            "void main() {\n" +
            "    vec3 pos = vec3((aPos.xy + uOffset) * uZoom, aPos.z);\n" +
            "    gl_Position = vec4(pos, 1.0);\n" +
            "}\n";

        // Fragment Shader
        String fragmentSrc =   
            "#version 330 core\n"   
            + "out vec4 fragColor;\n"
            + "void main () { fragColor = vec4(0.2, 0.8, 0.4, 0.1); } \n";
        
        // Compilar vertex shader
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        // Compilar fragment shader
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        // Crear programa
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        // Obtener ubicaciones de los uniforms
        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uZoomLocation = GL20.glGetUniformLocation(programa, "uZoom");
        
        if (uOffsetLocation == -1) {
            throw new RuntimeException("No se encontro el uniform uOffset");
        }
        if (uZoomLocation == -1) {
            throw new RuntimeException("No se encontro el uniform uZoom");
        }

        // Limpiar shaders
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " Shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    private void crearTriangulo() {
        float[] vertices = {
            0.0f,    0.5f,    0.0f,
            -0.5f,   -0.5f,    0.0f,
            0.5f,   -0.5f,    0.0f,
        };

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void procesarInput(float deltaTime) {
        // Movimiento con flechas
        float pasoMov = VELOCIDAD_MOV * deltaTime;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            offsetX -= pasoMov;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            offsetX += pasoMov;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            offsetY += pasoMov;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
            offsetY -= pasoMov;
        }

        // Limitar movimiento
        offsetX = Math.max(-LIMITE, Math.min(LIMITE, offsetX));
        offsetY = Math.max(-LIMITE, Math.min(LIMITE, offsetY));

        // Zoom con teclado numérico
        float pasoZoom = VELOCIDAD_ZOOM * deltaTime;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ADD) == GLFW.GLFW_PRESS) {
            zoom += pasoZoom;
        }
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_SUBTRACT) == GLFW.GLFW_PRESS) {
            zoom -= pasoZoom;
        }

        // Limitar zoom
        zoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoom));
    }

    //============================================================
    // Bucle Principal
    //============================================================
    private void loop(){
        float ultimoTiempo = (float)GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float tiempoActual = (float) GLFW.glfwGetTime();
            float deltaTiempo = tiempoActual - ultimoTiempo;
            ultimoTiempo = tiempoActual;

            procesarInput(deltaTiempo);

            // Limpiar pantalla
            GL11.glClearColor(0.08f, 0.08f, 0.12f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            
            // Usar programa y asignar uniforms
            GL20.glUseProgram(programa);
            GL20.glUniform2f(uOffsetLocation, offsetX, offsetY);
            GL20.glUniform1f(uZoomLocation, zoom);
            
            // Dibujar
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            
            // Intercambiar buffers y procesar eventos
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    //====================================================
    // Limpieza
    //====================================================
    private void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new AppMovimientoZoom().run();
    }
}