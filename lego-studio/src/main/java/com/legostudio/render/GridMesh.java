package com.legostudio.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders the baseplate grid for brick placement reference.
 */
public class GridMesh {
    private int vaoId;
    private int vboId;
    private int vertexCount;

    public GridMesh(int size) {
        generateGrid(size);
    }

    private void generateGrid(int size) {
        // Generate grid lines from 0 to size
        int lineCount = (size + 1) * 2;
        float[] vertices = new float[lineCount * 2 * 3];

        int idx = 0;

        // Horizontal lines (along X)
        for (int i = 0; i <= size; i++) {
            float z = i;
            vertices[idx++] = 0;
            vertices[idx++] = 0;
            vertices[idx++] = z;
            vertices[idx++] = size;
            vertices[idx++] = 0;
            vertices[idx++] = z;
        }

        // Vertical lines (along Z)
        for (int i = 0; i <= size; i++) {
            float x = i;
            vertices[idx++] = x;
            vertices[idx++] = 0;
            vertices[idx++] = 0;
            vertices[idx++] = x;
            vertices[idx++] = 0;
            vertices[idx++] = size;
        }

        vertexCount = lineCount * 2;

        // Upload to GPU
        FloatBuffer buffer = null;
        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            buffer = MemoryUtil.memAllocFloat(vertices.length);
            buffer.put(vertices).flip();

            vboId = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            glBindVertexArray(0);
        } finally {
            if (buffer != null) MemoryUtil.memFree(buffer);
        }
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}
