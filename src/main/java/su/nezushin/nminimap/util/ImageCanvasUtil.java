package su.nezushin.nminimap.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageCanvasUtil {

    /** Matches {@code FL_ROTATE} in {@code vertex_body.glsl}. */
    public static final int FL_ROTATE = 1;
    /** Matches {@code FL_INV_ROT} in {@code vertex_body.glsl}. */
    public static final int FL_INV_ROT = 2;

    public static int frameFlags(boolean rotateWithPlayer, boolean inverseRotation) {
        return (rotateWithPlayer ? FL_ROTATE : 0) | (inverseRotation ? FL_INV_ROT : 0);
    }

    /**
     * Marker sprites stay screen-upright on a round map unless {@code FL_ROTATE} is set.
     */
    public static int markerFlags(boolean keepUpright) {
        return keepUpright ? 0 : FL_ROTATE;
    }

    /**
     * Prepare image to be marker. Add 4 pixels with specific at the corners
     *
     * @param originalImage
     * @param colors
     * @param outFile
     * @throws IOException
     */
    public static void processPng(BufferedImage originalImage, List<Integer> colors, File outFile, int[] markerSize, int green, int flags) throws IOException {
        ImageIO.write(packMarker(originalImage, colors, markerSize, green, flags), "png", outFile);
    }

    /**
     * Pack a square frame the same way as a marker and stamp an extra metadata pixel with
     * the frame offset on screen and the map size the frame is drawn for.
     *
     * @param colors corner IDs counterclockwise (TL, BL, BR, TR)
     * @return false if the image is too large to pack or too small to hold the metadata pixel
     */
    public static boolean processSquareFramePng(BufferedImage originalImage, List<Integer> colors, File outFile,
                                                int offsetX, int offsetY, boolean rotateWithPlayer,
                                                boolean inverseRotation) throws IOException {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        //rows 0-2 and the two bottom ones are taken by metadata
        if (height < 5 || height > 256 || width + 2 > 256) {
            return false;
        }

        BufferedImage resultImage = packMarker(originalImage, colors, null, 2, frameFlags(rotateWithPlayer, inverseRotation));

        int offsetXValue = Math.max(-127, Math.min(127, offsetX)) + 127;
        int offsetYValue = Math.max(-127, Math.min(127, offsetY)) + 127;

        resultImage.setRGB(0, 2, new Color(((float) offsetXValue) / 255.0f, ((float) offsetYValue) / 255.0f,
                ((float) 127) / 255.0f, 1.0f / 100f).getRGB());

        ImageIO.write(resultImage, "png", outFile);
        return true;
    }

    private static BufferedImage packMarker(BufferedImage originalImage, List<Integer> colors, int[] markerSize, int green) {
        return packMarker(originalImage, colors, markerSize, green, 0);
    }

    private static BufferedImage packMarker(BufferedImage originalImage, List<Integer> colors, int[] markerSize, int green, int flags) {

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage resultImage = new BufferedImage(width + 2, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resultImage.createGraphics();

        g2d.drawImage(originalImage, 1, 0, null);
        g2d.dispose();

        //markers in corners
        resultImage.setRGB(0, 0, new Color(((float) colors.get(0)) / 255.0f, ((float) green) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(0, height - 1, new Color(((float) colors.get(1)) / 255.0f, ((float) green) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, height - 1, new Color(((float) colors.get(2)) / 255.0f, ((float) green) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, 0, new Color(((float) colors.get(3)) / 255.0f, ((float) green) / 255.0f, 0.0f, 1.0f / 100f).getRGB());

        int newWidth = markerSize == null ? width + 2 : markerSize[0];
        int newHeight = markerSize == null ? height : markerSize[1];
        int flagValue = Math.max(0, Math.min(255, flags));

        //size info for shader; B is flags (FL_ROTATE = 1, FL_INV_ROT = 2)
        resultImage.setRGB(0, 1, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, ((float) flagValue) / 255.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, 1, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, ((float) flagValue) / 255.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(0, height - 2, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, ((float) flagValue) / 255.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, height - 2, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, ((float) flagValue) / 255.0f, 1.0f / 100f).getRGB());

        return resultImage;
    }

    /**
     * Pack a long frame strip into a shader-ready font glyph: slice into 256px-wide rows,
     * stack them vertically, and stamp corner / size / metadata pixels.
     *
     * @param colors corner IDs counterclockwise (TL, BL, BR, TR)
     * @return false if the packed image would exceed 256px height
     */
    public static boolean processFramePng(BufferedImage originalImage, List<Integer> colors, File outFile,
                                          boolean rotateWithPlayer, boolean inverseRotation, int inset) throws IOException {
        int sourceWidth = originalImage.getWidth();
        int sourceHeight = originalImage.getHeight();
        int sliceCount = (sourceWidth + 255) / 256;
        int packedWidth = Math.min(256, sourceWidth);
        int packedHeight = sliceCount * sourceHeight + sliceCount;

        if (sourceHeight < 1 || packedWidth < 4 || packedHeight > 256) {
            return false;
        }

        BufferedImage resultImage = new BufferedImage(packedWidth, packedHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resultImage.createGraphics();
        //g2d.setColor(Color.BLACK);
        //g2d.fillRect(0, 0, packedWidth, packedHeight);

        for (int i = 0; i < sliceCount; i++) {
            int srcX = i * 256;
            int sliceWidth = Math.min(256, sourceWidth - srcX);
            int dstY = 1 + i * (sourceHeight);
            g2d.drawImage(originalImage,
                    0, dstY, sliceWidth, dstY + sourceHeight,
                    srcX, 0, srcX + sliceWidth, sourceHeight,
                    null);
        }
        g2d.dispose();

        int packedWidthMinusOne = packedWidth - 1;
        int packedHeightMinusOne = packedHeight - 1;
        int insetValue = Math.max(0, Math.min(255, inset));
        int flags = frameFlags(rotateWithPlayer, inverseRotation);

        resultImage.setRGB(0, 0, new Color(((float) colors.get(0)) / 255.0f, 3.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(0, packedHeightMinusOne, new Color(((float) colors.get(1)) / 255.0f, 3.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(packedWidthMinusOne, packedHeightMinusOne, new Color(((float) colors.get(2)) / 255.0f, 3.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(packedWidthMinusOne, 0, new Color(((float) colors.get(3)) / 255.0f, 3.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());

        resultImage.setRGB(1, 0, new Color(((float) packedWidthMinusOne) / 255.0f, ((float) packedHeightMinusOne) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(1, packedHeightMinusOne, new Color(((float) packedWidthMinusOne) / 255.0f, ((float) packedHeightMinusOne) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(packedWidth - 2, 0, new Color(((float) packedWidthMinusOne) / 255.0f, ((float) packedHeightMinusOne) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(packedWidth - 2, packedHeightMinusOne, new Color(((float) packedWidthMinusOne) / 255.0f, ((float) packedHeightMinusOne) / 255.0f, 0.0f, 1.0f / 100f).getRGB());

        resultImage.setRGB(2, 0, new Color(((float) flags) / 255.0f, ((float) ((sourceWidth >> 8) & 0xFF)) / 255.0f, ((float) (sourceWidth & 0xFF)) / 255.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(3, 0, new Color(0.0f, ((float) insetValue) / 255.0f, 0.0f, 1.0f / 100f).getRGB());

        ImageIO.write(resultImage, "png", outFile);
        return true;
    }


}
