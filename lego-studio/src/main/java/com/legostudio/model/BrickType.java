package com.legostudio.model;

/**
 * Represents different types of Lego bricks with their dimensions.
 * Dimensions are in Lego units (1 unit = 8mm in real Lego).
 */
public enum BrickType {
    BRICK_1X1(1, 1, 3),
    BRICK_1X2(1, 2, 3),
    BRICK_1X3(1, 3, 3),
    BRICK_1X4(1, 4, 3),
    BRICK_2X2(2, 2, 3),
    BRICK_2X3(2, 3, 3),
    BRICK_2X4(2, 4, 3),
    PLATE_1X1(1, 1, 1),
    PLATE_1X2(1, 2, 1),
    PLATE_2X2(2, 2, 1),
    PLATE_2X4(2, 4, 1);

    private final int width;  // X dimension in studs
    private final int length; // Z dimension in studs
    private final int height; // Y dimension in plates (3 plates = 1 brick height)

    BrickType(int width, int length, int height) {
        this.width = width;
        this.length = length;
        this.height = height;
    }

    public int getWidth() { return width; }
    public int getLength() { return length; }
    public int getHeight() { return height; }

    // Real-world scale factors for rendering
    public static final float STUD_SIZE = 1.0f;       // 1 unit per stud
    public static final float PLATE_HEIGHT = 0.4f;    // Height of one plate
    public static final float STUD_RADIUS = 0.3f;     // Radius of stud on top
    public static final float STUD_HEIGHT = 0.2f;     // Height of stud
}
