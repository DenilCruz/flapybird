package com.graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.*;

/**
 * Genera y reproduce efectos de sonido sintéticos (PCM) sin archivos externos.
 * Cada sonido se reproduce en un hilo separado para no bloquear el game loop.
 */
public class SoundManager {

    public enum Sound {
        SALTO, PUNTO, MUERTE, GAME_OVER
    }

    private static final int SAMPLE_RATE = 44100;

    private static final ExecutorService pool = Executors.newFixedThreadPool(4);

    /** Reproduce un sonido en segundo plano. */
    public static void play(Sound sound) {
        pool.submit(() -> {
            try {
                byte[] data;
                switch (sound) {
                    case SALTO -> data = tono(880, 80, 0.4f);
                    case PUNTO -> data = melodiaPunto();
                    case MUERTE -> data = tono(300, 150, 0.5f);
                    case GAME_OVER -> data = gameOverSound();
                    default -> data = tono(440, 100, 0.3f);
                }
                reproducir(data);
            } catch (Exception ignored) {
                /* No crashear si el audio falla */ }
        });
    }

    // ── Generadores de sonido ─────────────────────────────────────────────

    /**
     * Genera un tono sinusoidal simple.
     * 
     * @param freq       frecuencia en Hz
     * @param duracionMs duración en milisegundos
     * @param volumen    amplitud 0.0–1.0
     */
    private static byte[] tono(int freq, int duracionMs, float volumen) {
        int muestras = SAMPLE_RATE * duracionMs / 1000;
        byte[] buf = new byte[muestras * 2]; // 16-bit mono
        for (int i = 0; i < muestras; i++) {
            // Envolvente simple: fade-out en el 20% final
            float env = i < muestras * 0.8f ? 1.0f
                    : (float) (muestras - i) / (muestras * 0.2f);
            double angulo = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
            short s = (short) (Math.sin(angulo) * 32000 * volumen * env);
            buf[i * 2] = (byte) (s & 0xFF);
            buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return buf;
    }

    /** Melodía ascendente corta para punto anotado. */
    private static byte[] melodiaPunto() {
        int[] notas = { 523, 659, 784 }; // Do, Mi, Sol (C5, E5, G5)
        int notaMs = 55;
        int total = notas.length * notaMs;
        int muestras = SAMPLE_RATE * total / 1000;
        byte[] buf = new byte[muestras * 2];
        int offset = 0;
        for (int freq : notas) {
            byte[] segmento = tono(freq, notaMs, 0.35f);
            System.arraycopy(segmento, 0, buf, offset, segmento.length);
            offset += segmento.length;
        }
        return buf;
    }

    /** Sonido descendente grave para game over. */
    private static byte[] gameOverSound() {
        int durMs = 600;
        int muestras = SAMPLE_RATE * durMs / 1000;
        byte[] buf = new byte[muestras * 2];
        for (int i = 0; i < muestras; i++) {
            float t = (float) i / muestras; // 0 → 1
            int freq = (int) (400 - t * 280); // 400 → 120 Hz (glide)
            float env = 1.0f - t;
            double ang = 2.0 * Math.PI * freq * i / SAMPLE_RATE;
            short s = (short) (Math.sin(ang) * 32000 * 0.5f * env);
            buf[i * 2] = (byte) (s & 0xFF);
            buf[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return buf;
    }

    // ── Reproducción PCM ──────────────────────────────────────────────────

    private static void reproducir(byte[] data) throws LineUnavailableException {
        AudioFormat formato = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, formato);
        if (!AudioSystem.isLineSupported(info))
            return;

        try (SourceDataLine linea = (SourceDataLine) AudioSystem.getLine(info)) {
            linea.open(formato);
            linea.start();
            linea.write(data, 0, data.length);
            linea.drain();
        }
    }

    public static void shutdown() {
        pool.shutdown(); // No acepta nuevas tareas
        try {
            if (!pool.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Fuerza cierre
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
    }
}
