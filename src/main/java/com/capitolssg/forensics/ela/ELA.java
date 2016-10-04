package com.capitolssg.forensics.ela;

import com.capitolssg.forensics.util.ColorUtils;
import com.capitolssg.forensics.util.FileUtils;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ColorProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by stephan on 10/2/16.
 */
public class ELA {

    private String result;
    private String image;

    public ELA(byte[] bytes, float ampFactor, int quantizationLevels) {

        result = "error";

        // Check for empty arrays
        if (bytes == null || bytes.length == 0) {
            return;
        }

        // Decode the image bytes
        BufferedImage imgSrc = null;
        try {
            imgSrc = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return;
        }

        // Save the image with high quality
        ImagePlus ipSrc = new ImagePlus("src", imgSrc);
        String file = String.format("%s/%s.jpg", System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        FileSaver fs = new FileSaver(ipSrc);
        fs.setJpegQuality(80);
        fs.saveAsJpeg(file);

        // Reopen the image
        Opener opener = new Opener();
        ImagePlus ipDest = opener.openImage(file);
        new File(file).delete();


        ColorProcessor cpSrc = ipSrc.getProcessor().convertToColorProcessor();
        ColorProcessor cpDest = ipDest.getProcessor().convertToColorProcessor();
        ColorProcessor cpResult = new ColorProcessor(cpSrc.getWidth(), cpSrc.getHeight());

        for (int i = 0; i < cpSrc.getPixelCount(); i++) {
            int r = (byte) Math.abs(ampFactor * (ColorUtils.quantize(0, 255, quantizationLevels, 1f * ColorUtils.getRed(cpSrc.get(i))) - ColorUtils.quantize(0, 255, quantizationLevels, 1f * ColorUtils.getRed(cpDest.get(i)))));
            int g = (byte) Math.abs(ampFactor * (ColorUtils.quantize(0, 255, quantizationLevels, 1f * ColorUtils.getGreen(cpSrc.get(i))) - ColorUtils.quantize(0, 255, quantizationLevels, 1f * ColorUtils.getGreen(cpDest.get(i)))));
            int b = (byte) Math.abs(ampFactor * (ColorUtils.quantize(0, 255, quantizationLevels, 1f * ColorUtils.getBlue(cpSrc.get(i))) - ColorUtils.quantize(0, 255, quantizationLevels, 1f * ColorUtils.getBlue(cpDest.get(i)))));

            cpResult.set(i, ColorUtils.getColor(r, g, b));
        }

        ipDest = new ImagePlus("src", cpResult);
        file = String.format("%s/%s.jpg", System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString());
        fs = new FileSaver(ipDest);
        fs.setJpegQuality(80);
        fs.saveAsJpeg(file);

        try {
            image = FileUtils.encodeFileToBase64Binary(file);
            result = "success";
        } catch (IOException e) {

        }

        new File(file).delete();
    }


    public String getResult() {
        return result;
    }

    public String getImage() {
        return image;
    }
}
