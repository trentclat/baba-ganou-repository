package com.legostudio.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

/**
 * OpenGL shader program wrapper with efficient uniform handling.
 */
public class Shader {
    private final int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    // Cached uniform locations for performance
    private int modelLoc = -1;
    private int viewLoc = -1;
    private int projectionLoc = -1;
    private int colorLoc = -1;
    private int lightDirLoc = -1;
    private int ambientLoc = -1;

    public Shader(String vertexSource, String fragmentSource) {
        programId = glCreateProgram();
        if (programId == 0) {
            throw new RuntimeException("Failed to create shader program");
        }

        vertexShaderId = createShader(vertexSource, GL_VERTEX_SHADER);
        fragmentShaderId = createShader(fragmentSource, GL_FRAGMENT_SHADER);

        link();
        cacheUniformLocations();
    }

    private int createShader(String source, int type) {
        int shaderId = glCreateShader(type);
        if (shaderId == 0) {
            throw new RuntimeException("Failed to create shader");
        }

        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile error: " + glGetShaderInfoLog(shaderId, 1024));
        }

        glAttachShader(programId, shaderId);
        return shaderId;
    }

    private void link() {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Shader link error: " + glGetProgramInfoLog(programId, 1024));
        }

        // Detach shaders after linking
        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
            glDeleteShader(vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
            glDeleteShader(fragmentShaderId);
        }

        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Shader validation warning: " + glGetProgramInfoLog(programId, 1024));
        }
    }

    private void cacheUniformLocations() {
        modelLoc = glGetUniformLocation(programId, "model");
        viewLoc = glGetUniformLocation(programId, "view");
        projectionLoc = glGetUniformLocation(programId, "projection");
        colorLoc = glGetUniformLocation(programId, "brickColor");
        lightDirLoc = glGetUniformLocation(programId, "lightDir");
        ambientLoc = glGetUniformLocation(programId, "ambient");
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setModel(Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            glUniformMatrix4fv(modelLoc, false, fb);
        }
    }

    public void setView(Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            glUniformMatrix4fv(viewLoc, false, fb);
        }
    }

    public void setProjection(Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            glUniformMatrix4fv(projectionLoc, false, fb);
        }
    }

    public void setColor(float r, float g, float b) {
        glUniform3f(colorLoc, r, g, b);
    }

    public void setLightDir(Vector3f dir) {
        glUniform3f(lightDirLoc, dir.x, dir.y, dir.z);
    }

    public void setAmbient(float ambient) {
        glUniform1f(ambientLoc, ambient);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
}
