package com.graphics;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Envuelve todos los recursos OpenGL:
 *   - Shaders (con soporte de rotación uniforme)
 *   - Tres VAOs: quad (rectángulo), triángulo, circle-fan (círculo)
 *   - Helpers drawRect / drawTriangle / drawCircle
 *
 * Todos los objetos se dibujan en coordenadas NDC usando
 * uOffset (traslación), uScale (escala) y uRotation (rotación en radianes).
 */
public class Renderer {

    // Ubicaciones de uniforms del shader
    private int uOffset, uScale, uColor, uRotation;

    // Identificadores de shaders y VAOs
    private int programa;
    private int vaoQuad, vboQuad;
    private int vaoTri,  vboTri;
    private int vaoCirc, vboCirc;
    private int circVertCount; // número de vértices del circle-fan

    // ── Inicialización ────────────────────────────────────────────────────

    /** Compila shaders y crea los tres VAOs base. */
    public void init() {
        compilarShaders();
        crearQuad();
        crearTriangulo();
        crearCirculo(32);
    }

    private void compilarShaders() {
        // Vertex shader: escala → rota → traslada
        String vertSrc = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            uniform vec2  uOffset;
            uniform vec2  uScale;
            uniform float uRotation;
            void main() {
                vec2 scaled = aPos.xy * uScale;
                float c = cos(uRotation);
                float s = sin(uRotation);
                vec2 rotated = vec2(c * scaled.x - s * scaled.y,
                                    s * scaled.x + c * scaled.y);
                gl_Position = vec4(rotated + uOffset, 0.0, 1.0);
            }
            """;

        // Fragment shader: color sólido uniforme
        String fragSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        int vs = compilar(vertSrc, GL20.GL_VERTEX_SHADER,   "Vertex");
        int fs = compilar(fragSrc, GL20.GL_FRAGMENT_SHADER, "Fragment");

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vs);
        GL20.glAttachShader(programa, fs);
        GL20.glLinkProgram(programa);
        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException("Link error: " + GL20.glGetProgramInfoLog(programa));

        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);

        uOffset   = GL20.glGetUniformLocation(programa, "uOffset");
        uScale    = GL20.glGetUniformLocation(programa, "uScale");
        uColor    = GL20.glGetUniformLocation(programa, "uColor");
        uRotation = GL20.glGetUniformLocation(programa, "uRotation");
    }

    private int compilar(String src, int tipo, String nombre) {
        int id = GL20.glCreateShader(tipo);
        GL20.glShaderSource(id, src);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE)
            throw new RuntimeException(nombre + " shader error: " + GL20.glGetShaderInfoLog(id));
        return id;
    }

    /** Quad unitario centrado en origen, -0.5..+0.5 en x e y (2 triángulos). */
    private void crearQuad() {
        float[] v = {
            -0.5f,-0.5f,0,  0.5f,-0.5f,0,  0.5f, 0.5f,0,
            -0.5f,-0.5f,0,  0.5f, 0.5f,0, -0.5f, 0.5f,0
        };
        int[] ids = subirVBO(v);
        vaoQuad = ids[0]; vboQuad = ids[1];
    }

    /**
     * Triángulo isósceles apuntando a la derecha,
     * centrado en (0,0), altura y base = 1.0.
     * Se usa para pico (→) y cola (←ротado).
     */
    private void crearTriangulo() {
        float[] v = {
            -0.5f,-0.5f,0,
             0.5f, 0.0f,0,
            -0.5f, 0.5f,0
        };
        int[] ids = subirVBO(v);
        vaoTri = ids[0]; vboTri = ids[1];
    }

    /** Circle-fan con N segmentos (radio 0.5, centrado en origen). */
    private void crearCirculo(int N) {
        circVertCount = N + 2; // centro + N perímetro + cerrar
        float[] v = new float[circVertCount * 3];
        v[0] = 0; v[1] = 0; v[2] = 0; // centro
        for (int i = 0; i <= N; i++) {
            float ang = (float)(2 * Math.PI * i / N);
            v[(i + 1) * 3]     = (float) Math.cos(ang) * 0.5f;
            v[(i + 1) * 3 + 1] = (float) Math.sin(ang) * 0.5f;
            v[(i + 1) * 3 + 2] = 0;
        }
        int[] ids = subirVBO(v);
        vaoCirc = ids[0]; vboCirc = ids[1];
    }

    /** Sube un array de vértices a un nuevo VAO+VBO. @return {vaoId, vboId} */
    private int[] subirVBO(float[] v) {
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    // ── Activación del pipeline ───────────────────────────────────────────

    /** Activa el programa de shader. Llamar antes de cualquier draw. */
    public void activar() {
        GL20.glUseProgram(programa);
    }

    // ── Helpers de dibujo ─────────────────────────────────────────────────

    /**
     * Dibuja un rectángulo (usando el quad unitario).
     * @param rot rotación en radianes alrededor del centro del quad
     */
    public void drawRect(float x, float y, float ancho, float alto,
                         float rot, float r, float g, float b) {
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform1f(uRotation, rot);
        GL20.glUniform3f(uColor, r, g, b);
        GL30.glBindVertexArray(vaoQuad);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    /** Sobrecarga sin rotación. */
    public void drawRect(float x, float y, float ancho, float alto,
                         float r, float g, float b) {
        drawRect(x, y, ancho, alto, 0f, r, g, b);
    }

    /**
     * Dibuja el triángulo base (apunta a la derecha en rot=0).
     * @param rot rotación en radianes para orientarlo en cualquier dirección
     */
    public void drawTriangle(float x, float y, float ancho, float alto,
                             float rot, float r, float g, float b) {
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, ancho, alto);
        GL20.glUniform1f(uRotation, rot);
        GL20.glUniform3f(uColor, r, g, b);
        GL30.glBindVertexArray(vaoTri);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    /**
     * Dibuja un círculo (approximado con triangle-fan).
     * @param radio radio en NDC (se pasa como escala uniforme)
     */
    public void drawCircle(float x, float y, float radio,
                           float r, float g, float b) {
        GL20.glUniform2f(uOffset, x, y);
        GL20.glUniform2f(uScale, radio * 2, radio * 2);
        GL20.glUniform1f(uRotation, 0f);
        GL20.glUniform3f(uColor, r, g, b);
        GL30.glBindVertexArray(vaoCirc);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, circVertCount);
    }

    // ── Limpieza ─────────────────────────────────────────────────────────

    /** Libera todos los recursos OpenGL. */
    public void cleanup() {
        GL30.glDeleteVertexArrays(vaoQuad);
        GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoTri);
        GL15.glDeleteBuffers(vboTri);
        GL30.glDeleteVertexArrays(vaoCirc);
        GL15.glDeleteBuffers(vboCirc);
        GL20.glDeleteProgram(programa);
    }
}
