package com.legostudio.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders 2D UI overlays like text hints and control panels.
 * Uses a simple bitmap font rendered as quads.
 */
public class UIRenderer {
    private static final String UI_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec2 aPos;

            uniform vec2 screenSize;

            void main() {
                // Convert pixel coordinates to NDC (-1 to 1)
                vec2 ndc = (aPos / screenSize) * 2.0 - 1.0;
                ndc.y = -ndc.y; // Flip Y so origin is top-left
                gl_Position = vec4(ndc, 0.0, 1.0);
            }
            """;

    private static final String UI_FRAGMENT_SHADER = """
            #version 330 core
            uniform vec4 color;
            out vec4 FragColor;

            void main() {
                FragColor = color;
            }
            """;

    // Simple 5x7 bitmap font (same patterns as TextMesh3D but for 2D)
    private static final int[][] FONT = new int[128][];
    private static final int CHAR_WIDTH = 5;
    private static final int CHAR_HEIGHT = 7;

    static {
        // Initialize font patterns
        FONT[' '] = new int[]{0, 0, 0, 0, 0, 0, 0};
        FONT['A'] = new int[]{0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
        FONT['B'] = new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10001, 0b10001, 0b11110};
        FONT['C'] = new int[]{0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110};
        FONT['D'] = new int[]{0b11110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b11110};
        FONT['E'] = new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b11111};
        FONT['F'] = new int[]{0b11111, 0b10000, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000};
        FONT['G'] = new int[]{0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110};
        FONT['H'] = new int[]{0b10001, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001};
        FONT['I'] = new int[]{0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110};
        FONT['J'] = new int[]{0b00111, 0b00010, 0b00010, 0b00010, 0b00010, 0b10010, 0b01100};
        FONT['K'] = new int[]{0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001};
        FONT['L'] = new int[]{0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111};
        FONT['M'] = new int[]{0b10001, 0b11011, 0b10101, 0b10101, 0b10001, 0b10001, 0b10001};
        FONT['N'] = new int[]{0b10001, 0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001};
        FONT['O'] = new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
        FONT['P'] = new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000};
        FONT['Q'] = new int[]{0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10010, 0b01101};
        FONT['R'] = new int[]{0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001};
        FONT['S'] = new int[]{0b01111, 0b10000, 0b10000, 0b01110, 0b00001, 0b00001, 0b11110};
        FONT['T'] = new int[]{0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100};
        FONT['U'] = new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110};
        FONT['V'] = new int[]{0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100};
        FONT['W'] = new int[]{0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b10101, 0b01010};
        FONT['X'] = new int[]{0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001};
        FONT['Y'] = new int[]{0b10001, 0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100};
        FONT['Z'] = new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111};
        // Lowercase (same as uppercase for simplicity)
        for (char c = 'A'; c <= 'Z'; c++) {
            FONT[Character.toLowerCase(c)] = FONT[c];
        }
        // Numbers
        FONT['0'] = new int[]{0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110};
        FONT['1'] = new int[]{0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110};
        FONT['2'] = new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111};
        FONT['3'] = new int[]{0b01110, 0b10001, 0b00001, 0b00110, 0b00001, 0b10001, 0b01110};
        FONT['4'] = new int[]{0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010};
        FONT['5'] = new int[]{0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110};
        FONT['6'] = new int[]{0b00110, 0b01000, 0b10000, 0b11110, 0b10001, 0b10001, 0b01110};
        FONT['7'] = new int[]{0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000};
        FONT['8'] = new int[]{0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110};
        FONT['9'] = new int[]{0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00010, 0b01100};
        // Punctuation
        FONT[':'] = new int[]{0b00000, 0b00100, 0b00100, 0b00000, 0b00100, 0b00100, 0b00000};
        FONT['-'] = new int[]{0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000};
        FONT['/'] = new int[]{0b00001, 0b00010, 0b00010, 0b00100, 0b01000, 0b01000, 0b10000};
        FONT['['] = new int[]{0b01110, 0b01000, 0b01000, 0b01000, 0b01000, 0b01000, 0b01110};
        FONT[']'] = new int[]{0b01110, 0b00010, 0b00010, 0b00010, 0b00010, 0b00010, 0b01110};
        FONT[','] = new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b00100, 0b01000};
        FONT['.'] = new int[]{0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100};
        FONT['('] = new int[]{0b00010, 0b00100, 0b01000, 0b01000, 0b01000, 0b00100, 0b00010};
        FONT[')'] = new int[]{0b01000, 0b00100, 0b00010, 0b00010, 0b00010, 0b00100, 0b01000};
    }

    private int shaderProgram;
    private int vao;
    private int vbo;
    private int screenSizeLoc;
    private int colorLoc;

    private int screenWidth;
    private int screenHeight;

    public void init() {
        // Create shader program
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, UI_VERTEX_SHADER);
        glCompileShader(vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("UI Vertex shader error: " + glGetShaderInfoLog(vertexShader));
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, UI_FRAGMENT_SHADER);
        glCompileShader(fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("UI Fragment shader error: " + glGetShaderInfoLog(fragmentShader));
        }

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("UI Shader link error: " + glGetProgramInfoLog(shaderProgram));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        screenSizeLoc = glGetUniformLocation(shaderProgram, "screenSize");
        colorLoc = glGetUniformLocation(shaderProgram, "color");

        System.out.println("UI Renderer initialized: program=" + shaderProgram +
                ", screenSizeLoc=" + screenSizeLoc + ", colorLoc=" + colorLoc);

        // Create VAO and VBO for dynamic quad rendering
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 6 * 2 * Float.BYTES * 1000, GL_DYNAMIC_DRAW); // Reserve space

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
    }

    public void setScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void beginRender() {
        glUseProgram(shaderProgram);
        glUniform2f(screenSizeLoc, screenWidth, screenHeight);
        glBindVertexArray(vao);

        // Disable depth test and culling for UI
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void endRender() {
        glBindVertexArray(0);
        glUseProgram(0);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }

    /**
     * Draw a filled rectangle.
     */
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        glUniform4f(colorLoc, r, g, b, a);

        float[] vertices = {
                x, y,
                x + width, y,
                x + width, y + height,
                x, y,
                x + width, y + height,
                x, y + height
        };

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glDrawArrays(GL_TRIANGLES, 0, 6);

        MemoryUtil.memFree(buffer);
    }

    /**
     * Draw text at the given position with the given scale.
     * @param text The text to draw
     * @param x X position in pixels
     * @param y Y position in pixels
     * @param scale Pixel size of each "bit" in the font
     * @param r Red component (0-1)
     * @param g Green component (0-1)
     * @param b Blue component (0-1)
     * @param a Alpha component (0-1)
     */
    public void drawText(String text, float x, float y, float scale, float r, float g, float b, float a) {
        glUniform4f(colorLoc, r, g, b, a);

        float[] vertices = new float[text.length() * CHAR_WIDTH * CHAR_HEIGHT * 6 * 2];
        int vertexCount = 0;

        float cursorX = x;

        for (char c : text.toCharArray()) {
            int[] pattern = FONT[c];
            if (pattern == null) {
                pattern = FONT[' '];
            }

            for (int row = 0; row < CHAR_HEIGHT; row++) {
                for (int col = 0; col < CHAR_WIDTH; col++) {
                    if ((pattern[row] & (1 << (4 - col))) != 0) {
                        float px = cursorX + col * scale;
                        float py = y + row * scale;
                        float pw = scale;
                        float ph = scale;

                        // Two triangles for the quad
                        vertices[vertexCount++] = px;
                        vertices[vertexCount++] = py;
                        vertices[vertexCount++] = px + pw;
                        vertices[vertexCount++] = py;
                        vertices[vertexCount++] = px + pw;
                        vertices[vertexCount++] = py + ph;

                        vertices[vertexCount++] = px;
                        vertices[vertexCount++] = py;
                        vertices[vertexCount++] = px + pw;
                        vertices[vertexCount++] = py + ph;
                        vertices[vertexCount++] = px;
                        vertices[vertexCount++] = py + ph;
                    }
                }
            }

            cursorX += (CHAR_WIDTH + 1) * scale; // +1 for spacing
        }

        if (vertexCount > 0) {
            FloatBuffer buffer = MemoryUtil.memAllocFloat(vertexCount);
            buffer.put(vertices, 0, vertexCount).flip();

            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            glDrawArrays(GL_TRIANGLES, 0, vertexCount / 2);

            MemoryUtil.memFree(buffer);
        }
    }

    /**
     * Get the width of text in pixels at the given scale.
     */
    public float getTextWidth(String text, float scale) {
        return text.length() * (CHAR_WIDTH + 1) * scale - scale;
    }

    /**
     * Get the height of text in pixels at the given scale.
     */
    public float getTextHeight(float scale) {
        return CHAR_HEIGHT * scale;
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        glDeleteProgram(shaderProgram);
    }
}
