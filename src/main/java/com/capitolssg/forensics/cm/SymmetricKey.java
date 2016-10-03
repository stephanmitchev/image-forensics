package com.capitolssg.forensics.cm;

/**
 * Created by stephan on 9/30/16.
 */
public class SymmetricKey {

    private int x;
    private int y;

    public SymmetricKey(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }


    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public String toString() {
        return x + "-" + y;
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
