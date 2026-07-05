package su.nezushin.nminimap.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ImageCanvasUtil {


    /**
     * Prepare image to be marker. Add 4 pixels with specific at the corners
     *
     * @param originalImage
     * @param colors
     * @param outFile
     * @throws IOException
     */
    public static void processPng(BufferedImage originalImage, List<Integer> colors, File outFile, int[] markerSize) throws IOException {

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage resultImage = new BufferedImage(width + 2, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resultImage.createGraphics();

        g2d.drawImage(originalImage, 1, 0, null);
        g2d.dispose();

        //markers in corners
        resultImage.setRGB(0, 0, new Color(((float) colors.get(0)) / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(0, height - 1, new Color(((float) colors.get(1)) / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, height - 1, new Color(((float) colors.get(2)) / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, 0, new Color(((float) colors.get(3)) / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f).getRGB());

        int newWidth = markerSize == null ? width + 2 : markerSize[0];
        int newHeight = markerSize == null ? height : markerSize[1];

        //size info for shader
        resultImage.setRGB(0, 1, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, 1, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(0, height - 2, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, 0.0f, 1.0f / 100f).getRGB());
        resultImage.setRGB(width + 1, height - 2, new Color(((float) newWidth) / 255.0f, ((float) (newHeight)) / 255.0f, 0.0f, 1.0f / 100f).getRGB());


        ImageIO.write(resultImage, "png", outFile);
    }


}
