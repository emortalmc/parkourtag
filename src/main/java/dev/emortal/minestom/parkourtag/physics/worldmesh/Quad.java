package dev.emortal.minestom.parkourtag.physics.worldmesh;

import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;

public record Quad(Vector3f point1, Vector3f point2, Vector3f point3, Vector3f point4) {
    public Triangle[] triangles() {
        Triangle[] triangles = new Triangle[2];
        triangles[0] = new Triangle(point1, point2, point3);
        triangles[1] = new Triangle(point3, point4, point1);
        return triangles;
    }

    public Vector3f min() {
        return new Vector3f(min(point1.x, point2.x, point3.x, point4.x), min(point1.y, point2.y, point3.y, point4.y), min(point1.z, point2.z, point3.z, point4.z));
    }

    private float min(float a, float b, float c, float d) {
        return Math.min(a, Math.min(b, Math.min(c, d)));
    }

    public Vector3f max() {
        return new Vector3f(max(point1.x, point2.x, point3.x, point4.x), max(point1.y, point2.y, point3.y, point4.y), max(point1.z, point2.z, point3.z, point4.z));
    }

    private float max(float a, float b, float c, float d) {
        return Math.max(a, Math.max(b, Math.max(c, d)));
    }

}