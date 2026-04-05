package su.nezushin.nminimap.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageCanvasUtil {


    /**
     * Prepare image to be marker. Add 4 pixels with specific at the corners
     *
     * @param originalImage
     * @param color
     * @param outFile
     * @throws IOException
     */
    public static void processPng(BufferedImage originalImage, Color color, File outFile) throws IOException {

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage resultImage = new BufferedImage(width + 2, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resultImage.createGraphics();

        g2d.drawImage(originalImage, 1, 0, null);
        g2d.dispose();

        //#010100
        resultImage.setRGB(0, 0, color.getRGB());
        resultImage.setRGB(width + 1, 0, color.getRGB());
        resultImage.setRGB(0, height - 1, color.getRGB());
        resultImage.setRGB(width + 1, height - 1, color.getRGB());


        ImageIO.write(resultImage, "png", outFile);
    }


}
