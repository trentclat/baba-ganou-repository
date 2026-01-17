package com.legostudio;

import com.legostudio.input.InputHandler;
import com.legostudio.model.*;
import com.legostudio.render.Camera;
import com.legostudio.render.Renderer;
import com.legostudio.render.UIRenderer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Main application class for the 3D Lego Studio.
 *
 * Controls:
 * - Right mouse drag: Rotate camera
 * - Middle mouse drag: Pan camera
 * - Scroll wheel: Zoom in/out
 * - Left click: Place brick
 * - R: Rotate brick
 * - X/Delete/Backspace: Delete brick at cursor
 * - Q/[: Previous brick type
 * - E/]: Next brick type
 * - ,: Previous color
 * - .: Next color
 * - W/PageUp: Raise placement height
 * - S/PageDown: Lower placement height
 * - C: Clear all bricks
 * - Escape: Exit
 */
public class LegoStudio implements InputHandler.BrickPlacementListener {
    private static final int INITIAL_WIDTH = 1280;
    private static final int INITIAL_HEIGHT = 720;
    private static final int GRID_SIZE = 32;

    private long window;
    private Renderer renderer;
    private UIRenderer uiRenderer;
    private Camera camera;
    private InputHandler inputHandler;
    private BrickWorld world;

    // Help panel state
    private boolean showHelpPanel = false;

    // Window size for ray casting (screen coordinates, not pixels)
    private int windowWidth = INITIAL_WIDTH;
    private int windowHeight = INITIAL_HEIGHT;
    // Framebuffer size for viewport (pixels, may differ on Retina displays)
    private int framebufferWidth = INITIAL_WIDTH;
    private int framebufferHeight = INITIAL_HEIGHT;

    // Current brick placement state
    private BrickType[] brickTypes = BrickType.values();
    private BrickColor[] brickColors = BrickColor.values();
    private int currentTypeIndex = 4;  // Start with 2x2 brick
    private int currentColorIndex = 0; // Start with red
    private int currentHeight = 0;     // Placement height in plates
    private int currentRotation = 0;

    // Ghost brick for preview
    private Brick ghostBrick;
    private boolean ghostValid = false;

    public static void main(String[] args) {
        new LegoStudio().run();
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Required for macOS
        glfwWindowHint(GLFW_SAMPLES, 4); // 4x antialiasing

        // Create window
        window = glfwCreateWindow(INITIAL_WIDTH, INITIAL_HEIGHT, "Lego Studio 3D", NULL, NULL);
        if (window == NULL) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window. Make sure your system supports OpenGL 3.3+");
        }

        // Center window on screen
        var vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(window,
                    (vidmode.width() - INITIAL_WIDTH) / 2,
                    (vidmode.height() - INITIAL_HEIGHT) / 2);
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // Enable vsync
        glfwShowWindow(window);

        // Initialize OpenGL
        GL.createCapabilities();

        // Initialize components
        camera = new Camera();
        camera.setTarget(GRID_SIZE / 2.0f, 2, GRID_SIZE / 2.0f); // Center on grid
        camera.setAspectRatio((float) INITIAL_WIDTH / INITIAL_HEIGHT);

        renderer = new Renderer();
        renderer.init(GRID_SIZE);

        uiRenderer = new UIRenderer();
        uiRenderer.init();

        world = new BrickWorld(GRID_SIZE);

        // Add some starter bricks
        addStarterBricks();

        inputHandler = new InputHandler(window, camera);
        inputHandler.setPlacementListener(this);
        inputHandler.setResizeListener(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer fw = stack.mallocInt(1);
                IntBuffer fh = stack.mallocInt(1);
                glfwGetFramebufferSize(window, fw, fh);
                framebufferWidth = fw.get(0);
                framebufferHeight = fh.get(0);
                glViewport(0, 0, framebufferWidth, framebufferHeight);

                IntBuffer ww = stack.mallocInt(1);
                IntBuffer wh = stack.mallocInt(1);
                glfwGetWindowSize(window, ww, wh);
                windowWidth = ww.get(0);
                windowHeight = wh.get(0);
            }
        });

        // Initial viewport setup
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer fw = stack.mallocInt(1);
            IntBuffer fh = stack.mallocInt(1);
            glfwGetFramebufferSize(window, fw, fh);
            framebufferWidth = fw.get(0);
            framebufferHeight = fh.get(0);
            glViewport(0, 0, framebufferWidth, framebufferHeight);

            IntBuffer ww = stack.mallocInt(1);
            IntBuffer wh = stack.mallocInt(1);
            glfwGetWindowSize(window, ww, wh);
            windowWidth = ww.get(0);
            windowHeight = wh.get(0);
        }

        printControls();
    }

    private void addStarterBricks() {
        // Create a small starter structure near center of grid
        int cx = GRID_SIZE / 2 - 3;
        int cz = GRID_SIZE / 2 - 2;
        world.addBrick(new Brick(BrickType.BRICK_2X4, BrickColor.RED, cx, 0, cz));
        world.addBrick(new Brick(BrickType.BRICK_2X4, BrickColor.RED, cx + 4, 0, cz));
        world.addBrick(new Brick(BrickType.BRICK_2X4, BrickColor.BLUE, cx + 2, 3, cz));
        world.addBrick(new Brick(BrickType.BRICK_2X2, BrickColor.YELLOW, cx, 6, cz));
        world.addBrick(new Brick(BrickType.BRICK_2X2, BrickColor.YELLOW, cx + 4, 6, cz));
    }

    private void printControls() {
        System.out.println("""

            ╔══════════════════════════════════════════╗
            ║         LEGO STUDIO 3D CONTROLS          ║
            ╠══════════════════════════════════════════╣
            ║  CAMERA:                                 ║
            ║    Right mouse drag  - Rotate           ║
            ║    Middle mouse drag - Pan              ║
            ║    Scroll wheel      - Zoom             ║
            ╠══════════════════════════════════════════╣
            ║  BUILDING:                               ║
            ║    Left click        - Place brick      ║
            ║    R                 - Rotate brick     ║
            ║    X / Delete        - Delete brick     ║
            ║    Q / [             - Previous type    ║
            ║    E / ]             - Next type        ║
            ║    , (comma)         - Previous color   ║
            ║    . (period)        - Next color       ║
            ║    W / PageUp        - Raise height     ║
            ║    S / PageDown      - Lower height     ║
            ║    C                 - Clear all        ║
            ║    Escape            - Exit             ║
            ╚══════════════════════════════════════════╝
            """);
    }

    private void loop() {
        long lastTime = System.nanoTime();
        int frames = 0;
        double fpsTimer = 0;

        while (!glfwWindowShouldClose(window)) {
            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - lastTime) / 1_000_000_000.0;
            lastTime = currentTime;

            // FPS counter
            fpsTimer += deltaTime;
            frames++;
            if (fpsTimer >= 1.0) {
                glfwSetWindowTitle(window, String.format(
                        "Lego Studio 3D - %d FPS | Bricks: %d | Type: %s | Color: %s | Height: %d",
                        frames, world.getBrickCount(),
                        brickTypes[currentTypeIndex], brickColors[currentColorIndex], currentHeight));
                frames = 0;
                fpsTimer = 0;
            }

            // Update ghost brick position
            updateGhostBrick();

            // Render 3D scene
            renderer.render(world, camera, ghostBrick, ghostValid);

            // Render UI overlay
            renderUI();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void updateGhostBrick() {
        // Use ray casting to find where mouse intersects the placement plane
        float planeY = currentHeight * 0.4f; // Convert plates to world units
        var worldPos = camera.screenToWorldOnPlane(
                (float) inputHandler.getMouseX(),
                (float) inputHandler.getMouseY(),
                windowWidth,
                windowHeight,
                planeY
        );

        if (worldPos == null) {
            ghostBrick = null;
            ghostValid = false;
            return;
        }

        // Convert world position to grid coordinates
        int gridX = (int) Math.floor(worldPos.x);
        int gridZ = (int) Math.floor(worldPos.z);

        // Clamp to valid grid range
        gridX = Math.max(0, Math.min(GRID_SIZE - 1, gridX));
        gridZ = Math.max(0, Math.min(GRID_SIZE - 1, gridZ));

        ghostBrick = new Brick(brickTypes[currentTypeIndex], brickColors[currentColorIndex],
                gridX, currentHeight, gridZ);
        ghostBrick.setRotation(currentRotation);

        ghostValid = world.isValidPlacement(ghostBrick);
    }

    // InputHandler.BrickPlacementListener implementation

    @Override
    public void onPlace() {
        if (ghostBrick != null && ghostValid) {
            Brick newBrick = new Brick(ghostBrick.getType(), ghostBrick.getColor(),
                    ghostBrick.getPosition().x, ghostBrick.getPosition().y, ghostBrick.getPosition().z);
            newBrick.setRotation(ghostBrick.getRotation());
            if (world.addBrick(newBrick)) {
                System.out.println("Placed: " + newBrick);
            }
        }
    }

    @Override
    public void onDelete() {
        if (ghostBrick != null) {
            Brick removed = world.removeBrickAt(
                    ghostBrick.getPosition().x,
                    ghostBrick.getPosition().y,
                    ghostBrick.getPosition().z);
            if (removed != null) {
                System.out.println("Removed: " + removed);
            }
        }
    }

    @Override
    public void onRotate() {
        currentRotation = (currentRotation + 90) % 360;
        System.out.println("Rotation: " + currentRotation + "°");
    }

    @Override
    public void onBrickTypeNext() {
        currentTypeIndex = (currentTypeIndex + 1) % brickTypes.length;
        System.out.println("Brick type: " + brickTypes[currentTypeIndex]);
    }

    @Override
    public void onBrickTypePrev() {
        currentTypeIndex = (currentTypeIndex - 1 + brickTypes.length) % brickTypes.length;
        System.out.println("Brick type: " + brickTypes[currentTypeIndex]);
    }

    @Override
    public void onColorNext() {
        currentColorIndex = (currentColorIndex + 1) % brickColors.length;
        System.out.println("Color: " + brickColors[currentColorIndex]);
    }

    @Override
    public void onColorPrev() {
        currentColorIndex = (currentColorIndex - 1 + brickColors.length) % brickColors.length;
        System.out.println("Color: " + brickColors[currentColorIndex]);
    }

    @Override
    public void onHeightUp() {
        currentHeight++;
        System.out.println("Height: " + currentHeight + " plates");
    }

    @Override
    public void onHeightDown() {
        currentHeight = Math.max(0, currentHeight - 1);
        System.out.println("Height: " + currentHeight + " plates");
    }

    @Override
    public void onClear() {
        world.clear();
        System.out.println("Cleared all bricks");
    }

    @Override
    public void onToggleHelp() {
        showHelpPanel = !showHelpPanel;
    }

    private void renderUI() {
        // Use framebuffer size for UI rendering on Retina displays
        uiRenderer.setScreenSize(framebufferWidth, framebufferHeight);
        uiRenderer.beginRender();

        // Scale UI elements for Retina (use ratio of framebuffer to window)
        float dpiScale = (float) framebufferWidth / windowWidth;

        float scale = 2.0f * dpiScale;
        float padding = 10.0f * dpiScale;

        if (showHelpPanel) {
            // Draw semi-transparent background panel
            float panelWidth = 280 * dpiScale;
            float panelHeight = 220 * dpiScale;
            float panelX = padding;
            float panelY = padding;
            uiRenderer.drawRect(panelX, panelY, panelWidth, panelHeight, 0.0f, 0.0f, 0.0f, 0.7f);

            // Draw controls text
            float textX = panelX + 10 * dpiScale;
            float textY = panelY + 10 * dpiScale;
            float lineHeight = uiRenderer.getTextHeight(scale) + 4 * dpiScale;

            uiRenderer.drawText("CONTROLS", textX, textY, scale, 1.0f, 0.85f, 0.0f, 1.0f);
            textY += lineHeight + 5 * dpiScale;

            String[] controls = {
                    "Left click - Place",
                    "R - Rotate",
                    "X - Delete",
                    "Q/E - Brick type",
                    "Comma/Period - Color",
                    "W/S - Height",
                    "Right drag - Camera",
                    "Scroll - Zoom",
                    "C - Clear all",
                    "I - Close help"
            };

            for (String line : controls) {
                uiRenderer.drawText(line, textX, textY, scale, 1.0f, 1.0f, 1.0f, 0.9f);
                textY += lineHeight;
            }
        } else {
            // Draw hint text in bottom-left corner
            String hint = "Press I for controls";
            float textY = framebufferHeight - uiRenderer.getTextHeight(scale) - padding;
            uiRenderer.drawText(hint, padding, textY, scale, 1.0f, 1.0f, 1.0f, 0.5f);
        }

        uiRenderer.endRender();
    }

    private void cleanup() {
        if (inputHandler != null) inputHandler.cleanup();
        if (renderer != null) renderer.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();

        if (window != NULL) {
            glfwDestroyWindow(window);
        }
        glfwTerminate();

        var callback = glfwSetErrorCallback(null);
        if (callback != null) callback.free();
    }
}
