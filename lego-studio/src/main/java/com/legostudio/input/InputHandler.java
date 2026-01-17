package com.legostudio.input;

import com.legostudio.render.Camera;
import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles all user input for camera control and brick manipulation.
 */
public class InputHandler {
    private final long window;
    private final Camera camera;

    private double lastMouseX, lastMouseY;
    private boolean rightMouseDown = false;
    private boolean middleMouseDown = false;

    private final float rotateSpeed = 0.3f;
    private final float zoomSpeed = 2.0f;
    private final float panSpeed = 0.05f;

    // Callbacks (kept as fields to prevent GC)
    private GLFWCursorPosCallback cursorCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWFramebufferSizeCallback framebufferCallback;

    // Listeners for brick interaction
    private BrickPlacementListener placementListener;
    private Runnable resizeListener;

    public interface BrickPlacementListener {
        void onPlace();
        void onDelete();
        void onRotate();
        void onBrickTypeNext();
        void onBrickTypePrev();
        void onColorNext();
        void onColorPrev();
        void onHeightUp();
        void onHeightDown();
        void onClear();
        void onToggleHelp();
    }

    public InputHandler(long window, Camera camera) {
        this.window = window;
        this.camera = camera;

        // Initialize mouse position
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);
        lastMouseX = xpos[0];
        lastMouseY = ypos[0];

        setupCallbacks();
    }

    public void setPlacementListener(BrickPlacementListener listener) {
        this.placementListener = listener;
    }

    public void setResizeListener(Runnable listener) {
        this.resizeListener = listener;
    }

    private void setupCallbacks() {
        // Cursor position for camera rotation/pan
        cursorCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double xpos, double ypos) {
                double deltaX = xpos - lastMouseX;
                double deltaY = ypos - lastMouseY;

                if (rightMouseDown) {
                    camera.rotate((float) -deltaX * rotateSpeed, (float) -deltaY * rotateSpeed);
                } else if (middleMouseDown) {
                    camera.pan((float) deltaX * panSpeed, (float) deltaY * panSpeed);
                }

                lastMouseX = xpos;
                lastMouseY = ypos;
            }
        };
        glfwSetCursorPosCallback(window, cursorCallback);

        // Mouse buttons
        mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    rightMouseDown = action == GLFW_PRESS;
                } else if (button == GLFW_MOUSE_BUTTON_MIDDLE) {
                    middleMouseDown = action == GLFW_PRESS;
                } else if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                    if (placementListener != null) {
                        placementListener.onPlace();
                    }
                }
            }
        };
        glfwSetMouseButtonCallback(window, mouseButtonCallback);

        // Scroll for zoom
        scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {
                camera.zoom((float) yoffset * zoomSpeed);
            }
        };
        glfwSetScrollCallback(window, scrollCallback);

        // Keyboard input
        keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
                if (placementListener == null) return;

                switch (key) {
                    case GLFW_KEY_R -> placementListener.onRotate();
                    case GLFW_KEY_DELETE, GLFW_KEY_BACKSPACE, GLFW_KEY_X -> placementListener.onDelete();
                    case GLFW_KEY_RIGHT_BRACKET, GLFW_KEY_E -> placementListener.onBrickTypeNext();
                    case GLFW_KEY_LEFT_BRACKET, GLFW_KEY_Q -> placementListener.onBrickTypePrev();
                    case GLFW_KEY_PERIOD -> placementListener.onColorNext();
                    case GLFW_KEY_COMMA -> placementListener.onColorPrev();
                    case GLFW_KEY_PAGE_UP, GLFW_KEY_W -> placementListener.onHeightUp();
                    case GLFW_KEY_PAGE_DOWN, GLFW_KEY_S -> placementListener.onHeightDown();
                    case GLFW_KEY_C -> placementListener.onClear();
                    case GLFW_KEY_I -> placementListener.onToggleHelp();
                    case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(window, true);
                }
            }
        };
        glfwSetKeyCallback(window, keyCallback);

        // Window resize
        framebufferCallback = new GLFWFramebufferSizeCallback() {
            @Override
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0) {
                    camera.setAspectRatio((float) width / height);
                    if (resizeListener != null) {
                        resizeListener.run();
                    }
                }
            }
        };
        glfwSetFramebufferSizeCallback(window, framebufferCallback);
    }

    /**
     * Get the current mouse X position.
     * If mouse is outside the window, returns the last known position inside.
     */
    public double getMouseX() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(window, xpos, ypos);

        // Check if cursor is inside window
        int[] winWidth = new int[1];
        int[] winHeight = new int[1];
        glfwGetWindowSize(window, winWidth, winHeight);

        if (xpos[0] >= 0 && xpos[0] <= winWidth[0] && ypos[0] >= 0 && ypos[0] <= winHeight[0]) {
            lastMouseX = xpos[0];
            lastMouseY = ypos[0];
        }

        return lastMouseX;
    }

    /**
     * Get the current mouse Y position.
     * If mouse is outside the window, returns the last known position inside.
     */
    public double getMouseY() {
        // getMouseX already updates both coordinates, just return cached value
        return lastMouseY;
    }

    public void cleanup() {
        if (cursorCallback != null) cursorCallback.free();
        if (mouseButtonCallback != null) mouseButtonCallback.free();
        if (scrollCallback != null) scrollCallback.free();
        if (keyCallback != null) keyCallback.free();
        if (framebufferCallback != null) framebufferCallback.free();
    }
}
