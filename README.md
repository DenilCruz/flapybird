# Flappy Bird – Primer Parcial OpenGL

## Integrantes
- *(Completar con nombre/s del estudiante)*

## Descripción
Juego estilo Flappy Bird construido con **LWJGL + OpenGL 3.3 Core Profile**.  
Soporta **dos jugadores simultáneos**, pájaro compuesto por figuras geométricas,  
velocidad progresiva y mejoras de interfaz visuales y de audio.

---

## Controles

| Jugador | Acción      | Tecla           |
|---------|-------------|-----------------|
| P1      | Saltar      | `SPACE`         |
| P2      | Saltar      | `W` o `↑`       |
| Ambos   | Reiniciar   | `R` o `ENTER`   |
| Ambos   | Salir       | `ESC`           |

---

## Compilación y Ejecución

### Requisitos
- Java 17+
- Maven 3.8+

### Compilar
```bash
mvn compile
```

### Ejecutar
```bash
mvn compile exec:exec "-DmainClass=com.graphics.AppFlappyBird"
```

---

## Estructura del Proyecto

```
src/main/java/com/graphics/
├── AppFlappyBird.java   ← Orquestador principal (game loop, ventana, render)
├── Bird.java            ← Estado de un pájaro (posición, física, animación)
├── BirdRenderer.java    ← Dibuja el pájaro con figuras geométricas OpenGL
├── Pipe.java            ← Modelo de tubería
├── GameState.java       ← FSM del juego, colisiones, dificultad progresiva
├── InputManager.java    ← Teclado con detección de flanco (ambos jugadores)
├── Renderer.java        ← OpenGL: shaders, VAOs (quad/triángulo/círculo)
└── SoundManager.java    ← Efectos de sonido PCM sintéticos (sin archivos externos)
```

---

## Cambios Respecto a la Versión Base

### R2.1 – Pájaro compuesto por figuras geométricas
- **Cuerpo**: rectángulo con el color del jugador.
- **Ala**: rectángulo animado con `sin(tiempo)` — aleteo continuo.
- **Pico**: triángulo naranja apuntando a la derecha.
- **Cola**: triángulo naranja apuntando a la izquierda (rotado 180°).
- **Ojo blanco**: círculo (triangle-fan, 32 segmentos).
- **Pupila**: círculo más pequeño negro.
- El pájaro se **inclina** según su velocidad vertical (`obtenerInclinacion()`).

### R2.2 – Dos jugadores simultáneos
- P1 (amarillo-dorado) en x=-0.45 → `SPACE`.
- P2 (azul-celeste) en x=-0.20 → `W` o `↑`.
- Cada uno tiene su propia posición, velocidad, puntaje y estado vivo/muerto.
- Las tuberías son compartidas; el juego continúa mientras al menos uno viva.
- HUD con bloques de color por jugador en la barra superior.

### R2.3 – Velocidad progresiva
- `nivel = min(10, max(score_P1, score_P2) / 3)` → niveles 0–10.
- Velocidad: **0.62 → 1.50** NDC/s.
- Tiempo entre tuberías: **1.5 → 0.85** s.
- Tamaño del gap: **0.50 → 0.30** NDC.
- El nivel y la velocidad se muestran en el **título de la ventana** y en el HUD (barras naranjas).

### R2.4 – Mejoras de interfaz
- **Fondo degradado**: franjas horizontales de azul oscuro a claro.
- **Nubes en parallax**: se mueven a 1/7 de la velocidad de las tuberías.
- **Montañas**: triángulos en dos capas (lejana/cercana).
- **Suelo**: franja verde con línea de hierba.
- **Tuberías con capuchón y borde oscuro**.
- **Pantalla de inicio**: overlay con instrucciones codificadas como bloques.
- **Pantalla de game over**: panel con puntajes y botón de reinicio parpadeante.
- **Sonidos PCM sintéticos** (sin archivos externos): salto (880 Hz), punto (acorde Do-Mi-Sol), muerte (tono corto), game over (glide descendente).
- **Parpadeo al morir**: el pájaro parpadea a 10 Hz tras colisionar.
