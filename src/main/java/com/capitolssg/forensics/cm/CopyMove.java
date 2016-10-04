package com.capitolssg.forensics.cm;

import com.capitolssg.forensics.util.ColorUtils;
import com.capitolssg.forensics.util.FileUtils;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ColorProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by stephan on 10/3/16.
 */
public class CopyMove {

    private String result;
    private String image;

    public CopyMove(byte[] bytes, float maxDifference, float minShift, float minStdDev, int quantizationLevels, int heatRadius) {

        result = "error";

        // Check for empty arrays
        if (bytes == null || bytes.length == 0) {
            return;
        }

        // Decode the image bytes
        BufferedImage imgSrc;
        try {
            imgSrc = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return;
        }


        ColorProcessor cpO = new ImagePlus("src", imgSrc).getProcessor().convertToColorProcessor();

        ColorProcessor cpResult = new ColorProcessor(cpO.getWidth(), cpO.getHeight());

        List<ImageBlock> matrix = new ArrayList<>(cpO.getPixelCount());


        int[] diff = new int[cpO.getPixelCount()];

        for (int i = 0; i < cpO.getPixelCount(); i++) {
            float gray = ColorUtils.quantize(
                    0,
                    255,
                    quantizationLevels,
                    0.4f * ColorUtils.getRed(cpO.get(i)) +
                            0.3f * ColorUtils.getGreen(cpO.get(i)) +
                            0.3f * ColorUtils.getBlue(cpO.get(i)));

            diff[i] = (int) gray;

        }

        for (int y = 0; y < cpO.getHeight() - ImageBlock.sideY; y++) {
            for (int x = 0; x < cpO.getWidth() - ImageBlock.sideX; x++) {
                ImageBlock ib = new ImageBlock();
                ib.ox = x;
                ib.oy = y;

                List<Integer> px = new ArrayList<>();
                for (int j = 0; j < ImageBlock.sideX * ImageBlock.sideY; j++) {
                    px.add(0);
                }

                for (int v = 0; v < ImageBlock.sideY; v++) {
                    for (int u = 0; u < ImageBlock.sideX; u++) {
                        int idx = (y + v) * cpO.getWidth() + x + u;
                        px.set(v * ImageBlock.sideX + u, diff[idx]);
                    }
                }

                ib.setPixels(px);
                //ib.pixels = ib.getDCT();
                matrix.add(ib);
            }
        }

        matrix.sort((p1, p2) -> p1.compareTo(p2));

        for (int i = 0; i < cpO.getPixelCount(); i++) {
            int color = cpO.get(i);
            cpResult.set(i, ColorUtils.getColor(ColorUtils.getRed(color) / 2, ColorUtils.getGreen(color) / 2, ColorUtils.getBlue(color) / 2));
        }

        Map<SymmetricKey, List<ImageBlock>> symmetricShiftVectors = new HashMap<>();


        int a = 0;
        while (a < matrix.size()) {
            if (a % 1000 == 0)
                System.out.println("a: " + a);

            if (matrix.get(a).getStdDev() > minStdDev) {
                ImageBlock ibA = matrix.get(a);

                int b = a + 1;


                while (b < matrix.size()) {

                    if (ibA.difference(matrix.get(b)) < maxDifference) {
                        ImageBlock ibB = matrix.get(b);
                        if (ibA.distance(ibB) >= minShift) {

                            SymmetricKey sk = new SymmetricKey(Math.abs(ibA.ox - ibB.ox), Math.abs(ibA.oy - ibB.oy), (float)ibA.getStdDev());

                            if (!symmetricShiftVectors.containsKey(sk)) {
                                symmetricShiftVectors.put(sk, new ArrayList<>());
                            }

                            symmetricShiftVectors.get(sk).add(ibA);
                            symmetricShiftVectors.get(sk).add(ibB);

                        }
                    } else {
                        break;
                    }

                    b++;

                }

            }

            a++;

        }

        Map<Integer, SymmetricKey> countSymmetricKeyVectors = new HashMap<>();

        for (SymmetricKey sk : symmetricShiftVectors.keySet()) {
            int idx = symmetricShiftVectors.get(sk).size();
            countSymmetricKeyVectors.put(idx, sk);
        }

        List<Integer> keys = new ArrayList<>(countSymmetricKeyVectors.keySet());
        Collections.sort(keys, (p1, p2) -> -(new Float(p1 * countSymmetricKeyVectors.get(p1).getWeight())).compareTo(p2 * countSymmetricKeyVectors.get(p2).getWeight()));

        for (int i = 0; i < 10 && i < keys.size(); i++) {

            List<ImageBlock> blocks = symmetricShiftVectors.get(countSymmetricKeyVectors.get(keys.get(i)));
            System.out.println("Shift: " + countSymmetricKeyVectors.get(keys.get(i)) + "; Count: " + keys.get(i) + "; StdDev: " + blocks.get(0).getStdDev());

            for (ImageBlock b : blocks) {
                for (int y = Math.max(0, b.oy - heatRadius); y < Math.min(cpResult.getHeight(), b.oy + ImageBlock.sideY + heatRadius); y++) {
                    for (int x = Math.max(0, b.ox - heatRadius); x < Math.min(cpResult.getWidth(), b.ox + ImageBlock.sideX + heatRadius); x++) {

                        int color = cpResult.get(x, y);
                        float dist = (float)Math.sqrt((x - b.ox - ImageBlock.sideX/2) * (x - b.ox - ImageBlock.sideX/2) +  (y - b.oy - ImageBlock.sideY/2) * (y - b.oy - ImageBlock.sideY/2));
                        dist = Math.max(0, Math.min(1, 1 - dist / (ImageBlock.sideX + heatRadius)));

                        cpResult.set(x, y, ColorUtils.getColor(
                                Math.min(255, (int) (ColorUtils.getRed(color) + 10 * dist)),
                                Math.min(255, (int) (ColorUtils.getGreen(color) + 3 * dist)),
                                Math.min(255, (int) (ColorUtils.getBlue(color) + 10 * dist)))
                        );
                    }
                }
            }
        }


        String file = String.format("%s/%s.jpg", System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        FileSaver fs = new FileSaver(new ImagePlus("result", cpResult));
        FileSaver.setJpegQuality(80);
        fs.saveAsJpeg(file);

        try {
            image = FileUtils.encodeFileToBase64Binary(file);
            result = "success";
        } catch (IOException e) {
            Logger.getAnonymousLogger().warning("Cannot write result temp file");
        }

        if (!new File(file).delete()) {
            Logger.getAnonymousLogger().warning("Cannot delete result temp file");
        }

    }



    public String getResult() {
        return result;
    }

    public String getImage() {
        return image;
    }
}
