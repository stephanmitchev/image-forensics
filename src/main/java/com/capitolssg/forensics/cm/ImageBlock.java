package com.capitolssg.forensics.cm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by stephan on 9/19/16.
 */
public class ImageBlock implements Comparable<ImageBlock> {

    public static int sideX = 8;
    public static int sideY = 8;

    public int ox;
    public int oy;

    private ArrayList<Integer> pixels;
    public float sum;
    public float variance;
    public float mean;

    public void setPixels(List<Integer> pixels) {
        this.pixels = new ArrayList<>(pixels);

        for (float a : pixels) {
            sum += a;
        }
        mean = sum / pixels.size();

        for (float a : pixels)
            variance += (a - mean) * (a - mean) / pixels.size();
    }

    public ImageBlock( int idx, List<Integer> allPixels, int width, int height) {
        ox = idx % width;
        oy = idx / width;

        List<Integer> pixels = new ArrayList<>();
        if (ox < width - sideX && oy < height - sideY) {
            IntStream.range(0, sideX * sideY - 1)
                    .forEach(i -> {
                        int index = (oy + i / sideX) * width + ox + i % sideX;
                        int p = allPixels.get(index);
                        pixels.add(p);
                    });
        }

        this.setPixels(pixels);
    }

    double getVariance() {
        return variance;
    }

    double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public float difference(ImageBlock o) {
        return Math.abs(sum - o.sum);
    }

    public float distance(ImageBlock o) {
        double dist = Math.sqrt((ox - o.ox) * (ox - o.ox) + (oy - o.oy) * (oy - o.oy));
        return (float) dist;
    }

    @Override
    public int compareTo(ImageBlock o) {
        for (int i = 0; i < pixels.size(); i++) {
            if (!pixels.get(i).equals(o.pixels.get(i))) {
                return pixels.get(i).compareTo(o.pixels.get(i));
            }
        }
        return 0;
    }
}
