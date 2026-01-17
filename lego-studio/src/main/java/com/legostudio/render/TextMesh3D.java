package com.legostudio.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Generates 3D extruded text mesh using a simple blocky font.
 * Each character is made of rectangular blocks for a pixel/Lego aesthetic.
 */
public class TextMesh3D {
    private int vaoId;
    private int vboVertices;
    private int vboNormals;
    private int eboIndices;
    private int indexCount;

    private static final float CHAR_WIDTH = 1.0f;
    private static final float CHAR_HEIGHT = 1.4f;
    private static final float CHAR_DEPTH = 0.3f;
    private static final float CHAR_SPACING = 0.2f;
    private static final float BLOCK_SIZE = 0.2f;

    // Simple 5x7 pixel font patterns (1 = filled, 0 = empty)
    private static final int[][] FONT = new int[128][];

    static {
        // Initialize font patterns for uppercase letters and space
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
    }

    public TextMesh3D(String text) {
        generateTextMesh(text.toUpperCase());
    }

    private void generateTextMesh(String text) {
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float xOffset = 0;

        for (char c : text.toCharArray()) {
            int[] pattern = FONT[c];
            if (pattern == null) {
                pattern = FONT[' '];
            }

            // Generate blocks for each pixel in the character
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 5; col++) {
                    if ((pattern[row] & (1 << (4 - col))) != 0) {
                        float x = xOffset + col * BLOCK_SIZE;
                        float y = (6 - row) * BLOCK_SIZE; // Flip Y so top is up
                        generateBlock(vertices, normals, indices, x, y, 0, BLOCK_SIZE, BLOCK_SIZE, CHAR_DEPTH);
                    }
                }
            }

            xOffset += CHAR_WIDTH + CHAR_SPACING;
        }

        uploadToGPU(vertices, normals, indices);
    }

    private void generateBlock(List<Float> v, List<Float> n, List<Integer> idx,
                                float x, float y, float z, float w, float h, float d) {
        int baseIndex = v.size() / 3;

        // Front face (z+)
        addQuad(v, n, idx, baseIndex,
                x, y, z + d,
                x + w, y, z + d,
                x + w, y + h, z + d,
                x, y + h, z + d,
                0, 0, 1);
        baseIndex += 4;

        // Back face (z-)
        addQuad(v, n, idx, baseIndex,
                x + w, y, z,
                x, y, z,
                x, y + h, z,
                x + w, y + h, z,
                0, 0, -1);
        baseIndex += 4;

        // Right face (x+)
        addQuad(v, n, idx, baseIndex,
                x + w, y, z + d,
                x + w, y, z,
                x + w, y + h, z,
                x + w, y + h, z + d,
                1, 0, 0);
        baseIndex += 4;

        // Left face (x-)
        addQuad(v, n, idx, baseIndex,
                x, y, z,
                x, y, z + d,
                x, y + h, z + d,
                x, y + h, z,
                -1, 0, 0);
        baseIndex += 4;

        // Top face (y+)
        addQuad(v, n, idx, baseIndex,
                x, y + h, z + d,
                x + w, y + h, z + d,
                x + w, y + h, z,
                x, y + h, z,
                0, 1, 0);
        baseIndex += 4;

        // Bottom face (y-)
        addQuad(v, n, idx, baseIndex,
                x, y, z,
                x + w, y, z,
                x + w, y, z + d,
                x, y, z + d,
                0, -1, 0);
    }

    private void addQuad(List<Float> v, List<Float> n, List<Integer> idx, int base,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float nx, float ny, float nz) {
        v.add(x1); v.add(y1); v.add(z1);
        v.add(x2); v.add(y2); v.add(z2);
        v.add(x3); v.add(y3); v.add(z3);
        v.add(x4); v.add(y4); v.add(z4);

        for (int i = 0; i < 4; i++) {
            n.add(nx); n.add(ny); n.add(nz);
        }

        idx.add(base); idx.add(base + 1); idx.add(base + 2);
        idx.add(base); idx.add(base + 2); idx.add(base + 3);
    }

    private void uploadToGPU(List<Float> vertices, List<Float> normals, List<Integer> indices) {
        FloatBuffer vertexBuffer = null;
        FloatBuffer normalBuffer = null;
        IntBuffer indexBuffer = null;

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            vertexBuffer = MemoryUtil.memAllocFloat(vertices.size());
            for (float f : vertices) vertexBuffer.put(f);
            vertexBuffer.flip();

            vboVertices = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            normalBuffer = MemoryUtil.memAllocFloat(normals.size());
            for (float f : normals) normalBuffer.put(f);
            normalBuffer.flip();

            vboNormals = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboNormals);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);

            indexBuffer = MemoryUtil.memAllocInt(indices.size());
            for (int i : indices) indexBuffer.put(i);
            indexBuffer.flip();

            eboIndices = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboIndices);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

            indexCount = indices.size();

            glBindVertexArray(0);
        } finally {
            if (vertexBuffer != null) MemoryUtil.memFree(vertexBuffer);
            if (normalBuffer != null) MemoryUtil.memFree(normalBuffer);
            if (indexBuffer != null) MemoryUtil.memFree(indexBuffer);
        }
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vboVertices);
        glDeleteBuffers(vboNormals);
        glDeleteBuffers(eboIndices);
        glDeleteVertexArrays(vaoId);
    }

    /**
     * Get the total width of the text for centering.
     */
    public static float getTextWidth(String text) {
        return text.length() * (CHAR_WIDTH + CHAR_SPACING) - CHAR_SPACING;
    }

    public static float getTextHeight() {
        return CHAR_HEIGHT;
    }
}
