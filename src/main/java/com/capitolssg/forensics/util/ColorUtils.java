package com.capitolssg.forensics.util;

/**
 * Created by stephan on 10/2/16.
 */
public class ColorUtils {

    public static int getRed(int color) {
        return (color & 0x00ff0000) >> 16;
    }

    public static int getGreen(int color) {
        return (color & 0x0000ff00) >> 8;
    }

    public static int getBlue(int color) {
        return (color & 0x00000ff);
    }

    public static int getColor(int r, int g, int b) {
        return 0xff000000 + (r << 16) + (g << 8) + b;
    }

    public static float quantize(int min, int max, int quantCount, float value) {
        float range = max - min;
        float quantSize = range / quantCount;

        float normalized = (value - min) / range;

        int quants = Math.round(normalized * quantCount);

        return quants * quantSize + min;

    }
}
