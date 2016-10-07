package com.capitolssg.forensics.cm;

import com.capitolssg.forensics.util.ColorUtils;
import com.capitolssg.forensics.util.FileUtils;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ColorProcessor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by stephan on 10/3/16.
 */
public class CopyMove {

    private String result;
    private String image;


    public CopyMove(byte[] bytes, float maxDifference, float minShift, float minStdDev, int quantizationLevels, int heatRadius, int suspectedCopies) {

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

        // Apply quantization on the colors
        List<Integer> diff = quantizeImage(cpO, quantizationLevels);

        // Create blocks and gather statistics
        List<ImageBlock> imageBlocks = createImageBlocks(diff, cpO.getWidth(), cpO.getHeight());

        // Sort by shift
        imageBlocks.sort((p1, p2) -> p1.compareTo(p2));

        // Discover sequential blocks based on the distance function that match the parameters
        // The symmetric key stores the StdDev as a weight parameter for later sorting
        Map<SymmetricKey, List<ImageBlock>> symmetricShiftVectors = discoverCopies(imageBlocks, minStdDev, maxDifference, minShift);

        // Find out the count of blocks for each unique distance
        Map<Integer, SymmetricKey> countSymmetricKeyVectors = symmetricShiftVectors.keySet()
                .parallelStream()
                .collect(Collectors.toMap(i -> symmetricShiftVectors.get(i).size(), i -> i, (v1, v2) -> v1));

        // Perform weighted sort using the StdDev as a weight
        List<Integer> keys = countSymmetricKeyVectors.keySet()
                .parallelStream()
                .sorted((p1, p2) -> -(new Float(p1 * countSymmetricKeyVectors.get(p1).getWeight())).compareTo(p2 * countSymmetricKeyVectors.get(p2).getWeight()))
                .collect(Collectors.toList());

        // Copy Original image and darken it
        ColorProcessor cpResult = new ColorProcessor(cpO.getWidth(), cpO.getHeight());
        IntStream.range(0, cpO.getPixelCount())
                .mapToObj(p -> Arrays.asList(p, ColorUtils.getColor(ColorUtils.getRed(cpO.get(p)) / 2, ColorUtils.getGreen(cpO.get(p)) / 2, ColorUtils.getBlue(cpO.get(p)) / 2)))
                .parallel()
                .forEach(a -> cpResult.set(a.get(0), a.get(1)));


        // Draw the findings
        for (int i = 0; i < suspectedCopies && i < keys.size(); i++) {

            List<ImageBlock> blocks = symmetricShiftVectors.get(countSymmetricKeyVectors.get(keys.get(i)));

            for (ImageBlock b : blocks) {
                for (int y = Math.max(0, b.oy - heatRadius); y < Math.min(cpResult.getHeight(), b.oy + ImageBlock.sideY + heatRadius); y++) {
                    for (int x = Math.max(0, b.ox - heatRadius); x < Math.min(cpResult.getWidth(), b.ox + ImageBlock.sideX + heatRadius); x++) {

                        int color = cpResult.get(x, y);
                        float dist = (float) Math.sqrt((x - b.ox - ImageBlock.sideX / 2) * (x - b.ox - ImageBlock.sideX / 2) + (y - b.oy - ImageBlock.sideY / 2) * (y - b.oy - ImageBlock.sideY / 2));
                        dist = Math.max(0, Math.min(1, 1 - dist / (ImageBlock.sideX + heatRadius)));

                        Color heatColor;
                        switch (i) {
                            case 0:
                                heatColor = Color.magenta;
                                break;
                            case 1:
                                heatColor = Color.green;
                                break;
                            case 2:
                                heatColor = Color.cyan;
                                break;
                            case 3:
                                heatColor = Color.yellow;
                                break;
                            case 4:
                                heatColor = Color.blue;
                                break;
                            case 5:
                                heatColor = Color.white;
                                break;
                            case 6:
                                heatColor = Color.pink;
                                break;
                            default:
                                heatColor = Color.red;
                                break;
                        }
                        cpResult.set(x, y, ColorUtils.getColor(
                                Math.min(255, (int) (ColorUtils.getRed(color) + heatColor.getRed() / 25 * dist)),
                                Math.min(255, (int) (ColorUtils.getGreen(color) + heatColor.getGreen() / 25 * dist)),
                                Math.min(255, (int) (ColorUtils.getBlue(color) + heatColor.getBlue() / 25 * dist)))
                        );
                    }
                }
            }
        }

        // Save to encode and return
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

        // Delete the temporary file
        new File(file).delete();

    }

    private Map<SymmetricKey,List<ImageBlock>> discoverCopies(
            List<ImageBlock> imageBlocks,
            float minStdDev,
            float maxDifference,
            float minShift) {

        Map<SymmetricKey, List<ImageBlock>> result = new HashMap<>();

        int a = 0;
        while (a < imageBlocks.size()) {

            if (imageBlocks.get(a).getStdDev() > minStdDev) {
                ImageBlock ibA = imageBlocks.get(a);

                int b = a + 1;

                while (b < imageBlocks.size()) {

                    if (ibA.difference(imageBlocks.get(b)) < maxDifference) {
                        ImageBlock ibB = imageBlocks.get(b);
                        if (ibA.distance(ibB) >= minShift) {

                            SymmetricKey sk = new SymmetricKey(Math.abs(ibA.ox - ibB.ox), Math.abs(ibA.oy - ibB.oy), (float)ibA.getStdDev());

                            if (!result.containsKey(sk)) {
                                result.put(sk, new ArrayList<>());
                            }

                            result.get(sk).add(ibA);
                            result.get(sk).add(ibB);
                        }
                    } else {
                        break;
                    }

                    b++;
                }
            }

            a++;
        }

        return result;
    }


    private List<ImageBlock> createImageBlocks(List<Integer> diff, int width, int height) {

        List<ImageBlock> result = new ArrayList<>();
        IntStream.range(0, diff.size())
                //.parallel()
                .filter(i -> (i % width < width - ImageBlock.sideX) && (i / width < height - ImageBlock.sideY))
                .forEach(i -> {
                    result.add(new ImageBlock(i, diff, width, height));
                });

        return result;
    }

    private List<Integer> quantizeImage(ColorProcessor cpO, int quantizationLevels) {

        List<Integer> results = IntStream
                .of((int[]) cpO.getPixels())
                .boxed()
                .parallel()
                .map(p -> (int) ColorUtils.quantize(0, 255, quantizationLevels, 0.4f * ColorUtils.getRed(p) + 0.3f * ColorUtils.getGreen(p) + 0.3f * ColorUtils.getBlue(p)))
                .collect(Collectors.toList());

        return results;
    }


    public String getResult() {
        return result;
    }

    public String getImage() {
        return image;
    }
}
