package com.legostudio.model;

/**
 * Standard Lego brick colors with RGB values.
 */
public enum BrickColor {
    RED(0.8f, 0.1f, 0.1f),
    BLUE(0.0f, 0.3f, 0.8f),
    YELLOW(1.0f, 0.85f, 0.0f),
    GREEN(0.0f, 0.6f, 0.2f),
    WHITE(0.95f, 0.95f, 0.95f),
    BLACK(0.1f, 0.1f, 0.1f),
    ORANGE(1.0f, 0.5f, 0.0f),
    LIGHT_GRAY(0.7f, 0.7f, 0.7f),
    DARK_GRAY(0.4f, 0.4f, 0.4f),
    BROWN(0.4f, 0.2f, 0.1f),
    TAN(0.85f, 0.75f, 0.55f),
    PINK(1.0f, 0.6f, 0.7f),
    LIME(0.6f, 0.9f, 0.2f),
    AZURE(0.3f, 0.7f, 0.9f),
    DARK_BLUE(0.0f, 0.15f, 0.4f);

    private final float r, g, b;

    BrickColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }

    public float[] toArray() {
        return new float[] { r, g, b };
    }
}
