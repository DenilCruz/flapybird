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

/**
 * Este programa hace tres cosas:
 *  1.Abre una ventana con GLFW
 *  2.Difinir un triangulo y unos shaders 
 *  3.En un bucle, dibujar el triangulo una y otra vez
 * 
 * CONCEPTOS CLAVES
 * -OpenGL es una libreria con un conjunto de funciones para dibujar usando el GPU
 * -GLFW: Bliblioteca que crea la ventana y amneja el teclado y el raton
 * -GPU: tarjeta garfica
 * -SHADER: Codigo que dice como transformar los vertices y que colores poner en cada pixel
 * -Vertice: un punto en 3D (x,y,z), Un triangulo tiene 3 vertices
 * -VAO/VBO: formas de guardar en la gpu los datos de los vertices (posicion, color, etc)
 */
public class AppMovimientoTeclado 
{
    //referencia a nuestra ventana, GLFW devuelve un handle(numero largo o ID) para idenrificarlo  
    private long window;
    //Nuestro programa de shaders: es el vertex shader + fragment shader unidos
    //la GPU usa este programa para dibujar cada frame
    private int programa;
    //vao:vertex array object: es como un array que guarda todas las configuraciones de un objeto
    private int vao;
    //VBO: vertex buffer object: es un bloque de memoria en la GPU donde guardamos las posiciones de los vertices del triangulo (x,y,z)
    private int vbo;
    //tamaño de la ventana en pixeles
    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    // Es un Id interno del uniform en el shader
    private int uOffsetLocation;    

    // Offset del triangulo
    private float offsetX = 0.0f;   
    private float offsetY = 0.0f;  

    // Velocidad en unidades de OpenGL
    private static final float VELOCIDAD = 1.2f;    
    // Limite del desplazamiento
    private static final float LIMITE = 0.9f;    


    public void run(){
        init(); //Crear la ventana, cargar OpenGl, crear los shaders y el triangulo
        loop(); //Bucle que dibuja cada frame hasta cerrar la ventana
        cleanup();  //liberar la memoria y cerrar el GLFW
    }

    //===========================================
    //Paso 1: Inicializar GLFW y crear la ventana
    //===========================================
    private void init(){
        GLFWErrorCallback.createPrint(System.err).set();
        //inicializar el GLFW, sino, no podemos crear la ventana
        if (!GLFW.glfwInit()){
            throw new IllegalStateException("No se pudo inicializar el GLFW");  
        }
        //restablece todas las opciones por defecto 
        GLFW.glfwDefaultWindowHints();
        //ocultar la ventana al principio; la mostramos cuando GLFW este listo
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        // Permitir que el usuario pueda redimensionar la ventana.
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        // Indicar a OpenGL que queremos usar la versión 3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        // "Core profile": solo funciones modernas de OpenGL (sin compatibilidad con código muy antiguo
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        // Activar el "forward compatibility" para usar el core profile
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        
        
        // Crear la ventana
        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "OpenGL Triángulo", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        // Atajo nuevo: cerrar ventana al presionar ESC
        GLFW.glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                GLFW.glfwSetWindowShouldClose(w, true);
            }
        });

        // Decimos a OpenGL que ejecute ahora todas las funciones en esta ventana 
        GLFW.glfwMakeContextCurrent(window);

        // Sincronizar el dibujo con el monitor
        GLFW.glfwSwapInterval(1);

        // Hacer visible la ventana
        GLFW.glfwShowWindow(window);

        // Cargar en Java las funciones de OPENGL según la versión que pedimos
        GL.createCapabilities();

        crearShaders();
        crearTriangulo();
    } 

    private void crearShaders() {
        // -- vertexSrc: código del GLSL (OpenGL Shading Language) del VertexShader
        // Recibe la posición de cada vertice (aPos) y la asignación a gl_position
        // "layout (location = 0) in vec3 aPos" = entrada en canal 0, 3 floats (x, y, z)

        String vertexSrc = 
            "#version 330 core\n" +
            "layout (location = 0) in vec3 aPos;\n" +
            "uniform vec2 uOffset;\n" +
            "void main() {\n" +
            "    vec3 pos = vec3(aPos.xy + uOffset, aPos.z);\n" +
            "    gl_Position = vec4(pos, 1.0);\n" +
            "}\n";

        // -- fragmentSrc: Código en GLSL del FragmentShader
        // se esta ejecutando para cada pixel del triangulo, la salida va a ser el fragColor (color del pixel)
        // vec4 (R, G, B, A) ROJO, VERDE, AZUL, ALPHA (transparencia)
        String fragmentSrc =   
            "#version 330 core\n"   
            + "out vec4 fragColor;\n"
            + "void main () { fragColor = vec4(0.2, 0.8, 0.4, 0.1); } \n";
        //Crear el objeto "vertex shader" en la GPU y compilar el código
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        // Crear el objeto "fragment shader" en la GPU y compilar el código
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        // Crear el programa
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        // Buscar la direccion o ID del uniform del uOffset
        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        if (uOffsetLocation == -1) {
            throw new RuntimeException("No se encontro el uniform uOffset");
        }

        // Los shaders ya estan copiados dentro del programa; podemos borrarlos
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " Shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    private void crearTriangulo() {
        // 3 vertices y cada uno va tener la posición (x, y, z)
        float[] vertices = {    // Variable vertices is never read
            0.0f,    0.5f,    0.0f, // vertice 1: arriba al centro
            -0.5f,   -0.5f,    0.0f, // vertice 2: abajo a la izquierda
            0.5f,   -0.5f,    0.0f, // vertice 3: abajo a la derecha
        };

        // VAO: objeto donde guardamos la configuración de los buffers y atributos
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // VBO: buffer en la GPU donde guardamos los numeros (coordenadas)
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // OpenGL espera los datos en un FloatBuffer, no es un float[] de java
        // Copiamos el array al buffer y flip para dejarlo listo para la lectura
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();

        // Enviamos los datos al VBO
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Decir a OpenGL como leer el buffer
        // atributo en location 0
        // 3 floats por vertice (x, y, z)
        // tipo GL_FLOAT
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);

        GL20.glEnableVertexAttribArray(0);

        // Desenlazar VBO y VAO. El VAO ya guardo la configuración y no hace falta dejarlos activos
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);

    }

    private void procesarInput(float deltaTime) {
        // Distancia recorrida por frame
        // velocidad * tiempo del frame
        float paso = VELOCIDAD * deltaTime;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT) == GLFW.GLFW_PRESS) {
            offsetX -= paso;
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT) == GLFW.GLFW_PRESS) {
            offsetX += paso;
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS) {
            offsetY += paso;
        }

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_DOWN) == GLFW.GLFW_PRESS) {
            offsetY -= paso;
        }

        offsetX = Math.max(-LIMITE, Math.min(LIMITE, offsetX));
        offsetY = Math.max(-LIMITE, Math.min(LIMITE, offsetY));
    }

    //============================================================
    // Bucle Principal: en cada iteración vamos a dibujar un frame
    //============================================================
    // Mientras la ventana no se cierra:
    // 1. Limpiamos la pantalla en un color de fondo
    // 2. Decidimos usar este programa de shaders y este vao (triangulo)
    // 3. Ordenamos dibujar 3 vértices en modo triángulo
    // 4. Intercambiamos los buffers (doble buffer: dibujamos en uno y mostramos el otro)
    // 5. Procesamos los eventos (cerrar ventana, teclado, etc)

    private void loop(){

        float ultimoTiempo = (float)GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {

            float tiempoActual = (float) GLFW.glfwGetTime();
            float deltaTiempo = tiempoActual - ultimoTiempo;
            ultimoTiempo = tiempoActual;

            procesarInput(deltaTiempo);

            // Color con el que limpiará la pantalla (gris oscuro)
            GL11.glClearColor (0.08f, 0.08f, 0.12f, 1.0f);
            // Limpiar el buffer de color 
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            // Activar nuestro programa de shaders (vertex + fragment)
            GL20.glUseProgram (programa);
            // A partir del id de uOffset modificamos los offset del vertices
            GL20.glUniform2f(uOffsetLocation, offsetX, offsetY);
            // Activar el VAO que contiene el triángulo
            GL30.glBindVertexArray (vao);
            // Orden de dibujo
            GL11.glDrawArrays (GL11.GL_TRIANGLES, 0, 3);
            // Double buffers: hemos dibujado un buffer oculto y lo mostramos en pantalla 
            GLFW.glfwSwapBuffers (window);
            // Procesamos los eventos del sistema (si cerro ventana, teclado, mouse, etc)
            GLFW.glfwPollEvents();
        }
    }

    //====================================================
    // Limpieza: liberamos los recursos y cerramos el GLFW
    //====================================================

    private void cleanup() {

        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

    }

    public static void main( String[] args )
    {
        //System.out.println( "Hello World!" );
        new AppMovimientoTeclado().run();
    }
}
