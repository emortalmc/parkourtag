package dev.emortal.minestom.parkourtag.config;

import net.minestom.server.coordinate.Pos;

public class PositionJson {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public Pos asPos() {
        return new Pos(x, y, z, yaw, pitch);
    }
}
