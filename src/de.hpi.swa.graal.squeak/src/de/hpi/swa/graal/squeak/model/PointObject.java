package de.hpi.swa.graal.squeak.model;

public class PointObject {

    long x;
    long y;
    long z;
    long w;

    public PointObject() {

    }

    public PointObject(final long x, final long y) {
        this.x = x;
        this.y = y;
    }

    public PointObject(final long x, final long y, final long z, final long w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void setX(final long x) {
        this.x = x;
    }

    public void setY(final long y) {
        this.y = y;
    }

    public void setZ(final long z) {
        this.z = z;
    }

    public void setW(final long w) {
        this.w = w;
    }
}
