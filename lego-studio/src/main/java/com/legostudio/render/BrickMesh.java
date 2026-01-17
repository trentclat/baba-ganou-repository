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
 * Generates and caches mesh data for Lego bricks.
 * Uses VAOs/VBOs for efficient rendering.
 */
public class BrickMesh {
    private static final int CYLINDER_SEGMENTS = 12;

    private int vaoId;
    private int vboVertices;
    private int vboNormals;
    private int eboIndices;
    private int indexCount;

    public BrickMesh(int width, int length, int height) {
        generateMesh(width, length, height);
    }

    private void generateMesh(int width, int length, int height) {
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float w = width;
        float l = length;
        float h = height * 0.4f; // Convert plates to world units

        // Generate main brick body (box)
        generateBox(vertices, normals, indices, 0, 0, 0, w, h, l);

        // Generate studs on top
        float studRadius = 0.3f;
        float studHeight = 0.17f;
        for (int sx = 0; sx < width; sx++) {
            for (int sz = 0; sz < length; sz++) {
                float cx = sx + 0.5f;
                float cz = sz + 0.5f;
                generateCylinder(vertices, normals, indices, cx, h, cz, studRadius, studHeight);
            }
        }

        // Upload to GPU
        uploadToGPU(vertices, normals, indices);
    }

    private void generateBox(List<Float> v, List<Float> n, List<Integer> idx,
                             float x, float y, float z, float w, float h, float l) {
        int baseIndex = v.size() / 3;

        // Front face (z+)
        addQuad(v, n, idx, baseIndex,
                x, y, z + l,
                x + w, y, z + l,
                x + w, y + h, z + l,
                x, y + h, z + l,
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
                x + w, y, z + l,
                x + w, y, z,
                x + w, y + h, z,
                x + w, y + h, z + l,
                1, 0, 0);
        baseIndex += 4;

        // Left face (x-)
        addQuad(v, n, idx, baseIndex,
                x, y, z,
                x, y, z + l,
                x, y + h, z + l,
                x, y + h, z,
                -1, 0, 0);
        baseIndex += 4;

        // Top face (y+)
        addQuad(v, n, idx, baseIndex,
                x, y + h, z + l,
                x + w, y + h, z + l,
                x + w, y + h, z,
                x, y + h, z,
                0, 1, 0);
        baseIndex += 4;

        // Bottom face (y-)
        addQuad(v, n, idx, baseIndex,
                x, y, z,
                x + w, y, z,
                x + w, y, z + l,
                x, y, z + l,
                0, -1, 0);
    }

    private void addQuad(List<Float> v, List<Float> n, List<Integer> idx, int base,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float x4, float y4, float z4,
                         float nx, float ny, float nz) {
        // Add vertices
        v.add(x1); v.add(y1); v.add(z1);
        v.add(x2); v.add(y2); v.add(z2);
        v.add(x3); v.add(y3); v.add(z3);
        v.add(x4); v.add(y4); v.add(z4);

        // Add normals
        for (int i = 0; i < 4; i++) {
            n.add(nx); n.add(ny); n.add(nz);
        }

        // Add indices (two triangles)
        idx.add(base); idx.add(base + 1); idx.add(base + 2);
        idx.add(base); idx.add(base + 2); idx.add(base + 3);
    }

    private void generateCylinder(List<Float> v, List<Float> n, List<Integer> idx,
                                   float cx, float baseY, float cz, float radius, float height) {
        int baseIndex = v.size() / 3;
        float topY = baseY + height;

        // Top center vertex
        v.add(cx); v.add(topY); v.add(cz);
        n.add(0f); n.add(1f); n.add(0f);
        int topCenterIdx = baseIndex++;

        // Generate top cap vertices and side vertices
        for (int i = 0; i <= CYLINDER_SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / CYLINDER_SEGMENTS);
            float x = cx + radius * (float) Math.cos(angle);
            float z = cz + radius * (float) Math.sin(angle);
            float nx = (float) Math.cos(angle);
            float nz = (float) Math.sin(angle);

            // Top cap vertex
            v.add(x); v.add(topY); v.add(z);
            n.add(0f); n.add(1f); n.add(0f);

            // Top side vertex
            v.add(x); v.add(topY); v.add(z);
            n.add(nx); n.add(0f); n.add(nz);

            // Bottom side vertex
            v.add(x); v.add(baseY); v.add(z);
            n.add(nx); n.add(0f); n.add(nz);
        }

        // Top cap triangles
        for (int i = 0; i < CYLINDER_SEGMENTS; i++) {
            int curr = baseIndex + i * 3;
            int next = baseIndex + ((i + 1) % (CYLINDER_SEGMENTS + 1)) * 3;
            idx.add(topCenterIdx);
            idx.add(curr);
            idx.add(next);
        }

        // Side triangles
        for (int i = 0; i < CYLINDER_SEGMENTS; i++) {
            int curr = baseIndex + i * 3;
            int next = baseIndex + (i + 1) * 3;

            // Top-left triangle
            idx.add(curr + 1);
            idx.add(next + 1);
            idx.add(next + 2);

            // Bottom-right triangle
            idx.add(curr + 1);
            idx.add(next + 2);
            idx.add(curr + 2);
        }
    }

    private void uploadToGPU(List<Float> vertices, List<Float> normals, List<Integer> indices) {
        FloatBuffer vertexBuffer = null;
        FloatBuffer normalBuffer = null;
        IntBuffer indexBuffer = null;

        try {
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Vertex positions
            vertexBuffer = MemoryUtil.memAllocFloat(vertices.size());
            for (float f : vertices) vertexBuffer.put(f);
            vertexBuffer.flip();

            vboVertices = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);

            // Normals
            normalBuffer = MemoryUtil.memAllocFloat(normals.size());
            for (float f : normals) normalBuffer.put(f);
            normalBuffer.flip();

            vboNormals = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboNormals);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(1);

            // Indices
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
}
