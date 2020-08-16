package pl.ksitarski.imageclassifier.imageclassifier;


import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Contains functions related to image conversion.
 */
public class ImageConverter {


    /**
     * Converts image to double array that is understandable by classifier.
     * @param image image to be converted
     * @param targetSize target width and height of the image
     * @param colors whether colors should be used
     */
    public static double[] imageToData(BufferedImage image, int targetSize, boolean colors) {
        BufferedImage scaled = getScaledImage(image, targetSize);
        if (colors) {
            double[] data = new double[targetSize * targetSize * 3];
            int[] opImage0 = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
            for (int i = 0; i < targetSize * targetSize * 3; i+=3) {
                int argb = opImage0[i/3];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                double r1 = r/255.0;
                double g1 = g/255.0;
                double b1 = b/255.0;
                data[i] = r1;
                data[i+1] = g1;
                data[i+2] = b1;
            }
            return data;
        } else {
            double[] data = new double[targetSize * targetSize];
            int[] opImage0 = ((DataBufferInt) scaled.getRaster().getDataBuffer()).getData();
            for (int i = 0; i < targetSize * targetSize; i++) {
                int argb = opImage0[i];
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                double grey = (r + g + b) / 3.0 / 255.0;
                data[i] = grey;
            }
            return data;
        }
    }

    /**
     * Scales down image to given size (width and height)
     */
    private static BufferedImage getScaledImage(BufferedImage source, final int SIZE) {
        if (source == null) return null;
        BufferedImage newImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);

        final double SIZE_AS_DOUBLE = SIZE * 1.0;

        Graphics2D graphics2D = (Graphics2D) newImage.getGraphics();

        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        AffineTransform affineTransform = new AffineTransform();
        affineTransform.scale(SIZE_AS_DOUBLE / source.getWidth(), SIZE_AS_DOUBLE / source.getHeight());
        graphics2D.drawImage(source, affineTransform, null);
        graphics2D.dispose();

        return newImage;
    }
}
