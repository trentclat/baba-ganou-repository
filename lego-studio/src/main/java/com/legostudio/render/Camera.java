package com.legostudio.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Orbital camera for viewing the Lego world.
 * Supports rotation, zoom, panning, and ray casting for mouse picking.
 */
public class Camera {
    private final Vector3f target = new Vector3f(0, 0, 0);
    private float distance = 20.0f;
    private float yaw = 45.0f;   // Horizontal rotation in degrees
    private float pitch = 30.0f; // Vertical rotation in degrees

    private float minDistance = 5.0f;
    private float maxDistance = 100.0f;
    private float minPitch = 5.0f;
    private float maxPitch = 89.0f;

    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f inverseViewProj = new Matrix4f();
    private final Vector3f position = new Vector3f();

    private float aspectRatio = 16.0f / 9.0f;
    private float fov = 45.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 500.0f;

    public Camera() {
        updateViewMatrix();
        updateProjectionMatrix();
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        yaw += deltaYaw;
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch + deltaPitch));
        updateViewMatrix();
    }

    public void zoom(float delta) {
        distance = Math.max(minDistance, Math.min(maxDistance, distance - delta));
        updateViewMatrix();
    }

    public void pan(float deltaX, float deltaZ) {
        // Calculate right and forward vectors on XZ plane
        float yawRad = (float) Math.toRadians(yaw);
        float rightX = (float) Math.cos(yawRad);
        float rightZ = (float) Math.sin(yawRad);
        float forwardX = -rightZ;
        float forwardZ = rightX;

        target.x += rightX * deltaX + forwardX * deltaZ;
        target.z += rightZ * deltaX + forwardZ * deltaZ;
        updateViewMatrix();
    }

    public void setTarget(float x, float y, float z) {
        target.set(x, y, z);
        updateViewMatrix();
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
        updateProjectionMatrix();
    }

    private void updateViewMatrix() {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Calculate camera position based on spherical coordinates
        float horizontalDist = distance * (float) Math.cos(pitchRad);
        position.x = target.x + horizontalDist * (float) Math.sin(yawRad);
        position.y = target.y + distance * (float) Math.sin(pitchRad);
        position.z = target.z + horizontalDist * (float) Math.cos(yawRad);

        viewMatrix.identity().lookAt(position, target, new Vector3f(0, 1, 0));
    }

    private void updateProjectionMatrix() {
        projectionMatrix.identity().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                nearPlane,
                farPlane
        );
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getTarget() {
        return target;
    }

    public float getDistance() {
        return distance;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    /**
     * Cast a ray from screen coordinates and find intersection with a horizontal plane.
     * @param mouseX Mouse X in screen coordinates (0 to width)
     * @param mouseY Mouse Y in screen coordinates (0 to height)
     * @param screenWidth Window width
     * @param screenHeight Window height
     * @param planeY Y coordinate of the horizontal plane to intersect
     * @return World coordinates of intersection, or null if no intersection
     */
    public Vector3f screenToWorldOnPlane(float mouseX, float mouseY, int screenWidth, int screenHeight, float planeY) {
        // Convert screen coords to normalized device coords (-1 to 1)
        float ndcX = (2.0f * mouseX) / screenWidth - 1.0f;
        float ndcY = 1.0f - (2.0f * mouseY) / screenHeight;

        // Create inverse view-projection matrix
        projectionMatrix.mul(viewMatrix, inverseViewProj);
        inverseViewProj.invert();

        // Unproject near and far points
        Vector4f nearPoint = new Vector4f(ndcX, ndcY, -1.0f, 1.0f);
        Vector4f farPoint = new Vector4f(ndcX, ndcY, 1.0f, 1.0f);

        inverseViewProj.transform(nearPoint);
        inverseViewProj.transform(farPoint);

        // Convert from homogeneous coordinates
        Vector3f rayStart = new Vector3f(nearPoint.x / nearPoint.w, nearPoint.y / nearPoint.w, nearPoint.z / nearPoint.w);
        Vector3f rayEnd = new Vector3f(farPoint.x / farPoint.w, farPoint.y / farPoint.w, farPoint.z / farPoint.w);

        // Ray direction
        Vector3f rayDir = new Vector3f(rayEnd).sub(rayStart).normalize();

        // Intersect with horizontal plane at planeY
        // Ray: P = rayStart + t * rayDir
        // Plane: y = planeY
        // Solve: rayStart.y + t * rayDir.y = planeY
        if (Math.abs(rayDir.y) < 0.0001f) {
            return null; // Ray is parallel to plane
        }

        float t = (planeY - rayStart.y) / rayDir.y;
        if (t < 0) {
            return null; // Intersection is behind camera
        }

        return new Vector3f(
                rayStart.x + t * rayDir.x,
                planeY,
                rayStart.z + t * rayDir.z
        );
    }
}
