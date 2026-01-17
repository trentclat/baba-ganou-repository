package com.legostudio.render;

import com.legostudio.model.Brick;
import com.legostudio.model.BrickColor;
import com.legostudio.model.BrickWorld;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Main renderer for the Lego world.
 */
public class Renderer {
    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;

            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;

            out vec3 fragNormal;
            out vec3 fragPos;

            void main() {
                fragPos = vec3(model * vec4(aPos, 1.0));
                fragNormal = mat3(transpose(inverse(model))) * aNormal;
                gl_Position = projection * view * vec4(fragPos, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec3 fragNormal;
            in vec3 fragPos;

            uniform vec3 brickColor;
            uniform vec3 lightDir;
            uniform float ambient;

            out vec4 FragColor;

            void main() {
                vec3 norm = normalize(fragNormal);
                vec3 light = normalize(-lightDir);
                float diff = max(dot(norm, light), 0.0);
                vec3 result = (ambient + diff * (1.0 - ambient)) * brickColor;
                FragColor = vec4(result, 1.0);
            }
            """;

    private static final String GRID_VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPos;

            uniform mat4 model;
            uniform mat4 view;
            uniform mat4 projection;

            void main() {
                gl_Position = projection * view * model * vec4(aPos, 1.0);
            }
            """;

    private static final String GRID_FRAGMENT_SHADER = """
            #version 330 core
            uniform vec3 brickColor;
            out vec4 FragColor;

            void main() {
                FragColor = vec4(brickColor, 1.0);
            }
            """;

    private Shader brickShader;
    private Shader gridShader;
    private MeshCache meshCache;
    private GridMesh gridMesh;
    private TextMesh3D titleMesh;
    private int gridSizeCache;

    private final Vector3f lightDirection = new Vector3f(-0.5f, -1.0f, -0.3f).normalize();
    private final float ambientStrength = 0.3f;

    private final Matrix4f modelMatrix = new Matrix4f();

    private static final String TITLE_TEXT = "YERK STUDIOS";

    public void init(int gridSize) {
        this.gridSizeCache = gridSize;

        // Enable depth testing and back-face culling
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Create shaders
        brickShader = new Shader(VERTEX_SHADER, FRAGMENT_SHADER);
        gridShader = new Shader(GRID_VERTEX_SHADER, GRID_FRAGMENT_SHADER);

        // Create mesh cache, grid, and title
        meshCache = new MeshCache();
        gridMesh = new GridMesh(gridSize);
        titleMesh = new TextMesh3D(TITLE_TEXT);
    }

    public void render(BrickWorld world, Camera camera, Brick ghostBrick, boolean ghostValid) {
        glClearColor(0.2f, 0.25f, 0.3f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Render grid
        renderGrid(camera, world.getGridSize());

        // Render all placed bricks
        brickShader.bind();
        brickShader.setView(camera.getViewMatrix());
        brickShader.setProjection(camera.getProjectionMatrix());
        brickShader.setLightDir(lightDirection);
        brickShader.setAmbient(ambientStrength);

        for (Brick brick : world.getBricks()) {
            renderBrick(brick);
        }

        // Render ghost brick (preview of placement)
        if (ghostBrick != null) {
            renderGhostBrick(ghostBrick, ghostValid);
        }

        // Render 3D title
        renderTitle(camera);

        brickShader.unbind();
    }

    private void renderTitle(Camera camera) {
        // Position title floating above the grid, centered
        float textWidth = TextMesh3D.getTextWidth(TITLE_TEXT);
        float scale = 3.0f;
        float x = (gridSizeCache - textWidth * scale) / 2.0f;
        float y = 15.0f; // Float above the grid
        float z = -5.0f; // Slightly in front

        brickShader.setColor(1.0f, 0.85f, 0.0f); // Yellow/gold color

        modelMatrix.identity()
                .translate(x, y, z)
                .scale(scale);

        brickShader.setModel(modelMatrix);
        titleMesh.render();
    }

    private void renderGrid(Camera camera, int gridSize) {
        gridShader.bind();

        modelMatrix.identity();
        gridShader.setModel(modelMatrix);
        gridShader.setView(camera.getViewMatrix());
        gridShader.setProjection(camera.getProjectionMatrix());
        gridShader.setColor(0.4f, 0.4f, 0.4f);

        gridMesh.render();
        gridShader.unbind();
    }

    private void renderBrick(Brick brick) {
        BrickColor color = brick.getColor();
        brickShader.setColor(color.getR(), color.getG(), color.getB());

        // Brick position is grid cell (0-based), render at that position
        float x = brick.getPosition().x;
        float y = brick.getPosition().y * 0.4f; // Convert plates to world units
        float z = brick.getPosition().z;

        modelMatrix.identity().translate(x, y, z);

        if (brick.getRotation() != 0) {
            float halfW = brick.getActualWidth() / 2.0f;
            float halfL = brick.getActualLength() / 2.0f;
            modelMatrix.translate(halfW, 0, halfL)
                    .rotateY((float) Math.toRadians(brick.getRotation()))
                    .translate(-halfW, 0, -halfL);
        }

        brickShader.setModel(modelMatrix);

        BrickMesh mesh = meshCache.getMesh(brick.getType());
        mesh.render();
    }

    private void renderGhostBrick(Brick brick, boolean valid) {
        // Use semi-transparent rendering for ghost
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (valid) {
            brickShader.setColor(0.5f, 1.0f, 0.5f); // Green for valid
        } else {
            brickShader.setColor(1.0f, 0.3f, 0.3f); // Red for invalid
        }

        float x = brick.getPosition().x;
        float y = brick.getPosition().y * 0.4f;
        float z = brick.getPosition().z;

        modelMatrix.identity().translate(x, y, z);

        if (brick.getRotation() != 0) {
            float halfW = brick.getActualWidth() / 2.0f;
            float halfL = brick.getActualLength() / 2.0f;
            modelMatrix.translate(halfW, 0, halfL)
                    .rotateY((float) Math.toRadians(brick.getRotation()))
                    .translate(-halfW, 0, -halfL);
        }

        brickShader.setModel(modelMatrix);

        BrickMesh mesh = meshCache.getMesh(brick.getType());
        mesh.render();

        glDisable(GL_BLEND);
    }

    public void cleanup() {
        if (brickShader != null) brickShader.cleanup();
        if (gridShader != null) gridShader.cleanup();
        if (meshCache != null) meshCache.cleanup();
        if (gridMesh != null) gridMesh.cleanup();
        if (titleMesh != null) titleMesh.cleanup();
    }
}
