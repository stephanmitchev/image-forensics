package com.capitolssg.forensics.cm;

/**
 * Created by stephan on 9/30/16.
 */
public class SymmetricKey {

    private int x;
    private int y;
    private float weight;
private float edgeDistance;

    public float getWeight() {
        return edgeDistance;
    }

    public SymmetricKey(int x, int y, float weight, int edgeDistance) {
        this.x = x;
        this.y = y;
        this.weight = weight;
        this.edgeDistance = 1/ new Float(Math.log(edgeDistance));
    }

    public String toString() {
        return x + "-" + y + ":" + weight;
    }

    public boolean equals(Object o) {
        return o.getClass().equals(this.getClass()) &&
                o != null &&
                this != null &&
                (
                        (((SymmetricKey)o).x == x && ((SymmetricKey)o).y == y) ||
                        (((SymmetricKey)o).x == y && ((SymmetricKey)o).y == x)
                );
    }

    public int hashCode() {
        return new Integer(x).hashCode() + new Integer(y).hashCode();
    }

}
