package polytech.content.analyzer.hash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.*;
import polytech.content.analyzer.stat.StatUtils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;

/*
 * pHash-like image hash.
 * Based On: <br/>
 * http://www.hackerfactor.com/blog/index.php?/archives/432-Looks-Like-It.html <br/>
 * https://www.memonic.com/user/aengus/folder/coding/id/1qVeq <br/>
 * http://phash.org/ <br/>
 */
@Component
public class PHashCalculator implements HashCalculator {

    private static final String CALCULATE_HASH = "Hash calculated successfully";
    private static final String CALCULATE_HASH_NO_SUCH_FILE = "СalculateHash: IO Exception, no such file or directory {}";

    private static final int SIZE = 32;
    private static final int SMALLER_SIZE = 8;

    private static final Logger logger = LoggerFactory.getLogger(PHashCalculator.class);

    private static final ColorConvertOp colorConvert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);

    public PHashCalculator() {
        initCoefficients();
    }

    /**
     * Calculate phash as 64-bit long
     *
     * @param path - Path to image file
     * @return phash
     * - phash in form of long number, or null if path is incorrect
     */
    @Override
    public Long calculateHash(Path path) {
        return calculateHash(path, 0);
    }

    /**
     * Calculate phash as 64-bit long
     *
     * @param path     - Path to image file
     * @param minWidth - If width is less than minWidth, the image will be scaled and blurred before processing
     * @return phash
     * - phash in form of long number, or null if path is incorrect
     */
    private Long calculateHash(Path path, int minWidth) {
        long startTime = StatUtils.getMeasureStartTime();
        InputStream is = null;
        try {
            is = new FileInputStream(path.toString());
            Long result = calculateHash(is, minWidth);
            StatUtils.success(startTime, CALCULATE_HASH, path, result);
            return result;
        } catch (java.io.IOException e) {
            logger.error(CALCULATE_HASH_NO_SUCH_FILE, path, e);
            StatUtils.failure(startTime, CALCULATE_HASH_NO_SUCH_FILE, path, e);
            return null;
        }
    }

    /**
     * Calculate phash as 64-bit long
     *
     * @param is - InputStream from image file
     * @return phash
     * - phash in form of long number, or null if path is incorrect
     */
    private Long calculateHash(InputStream is) throws IOException {
        return calculateHash(is, 0);
    }

    /**
     * Calculate phash as 64-bit long
     *
     * @param is       - InputStream from image file
     * @param minWidth - If width is less than minWidth, the image will be scaled and blurred before processing
     * @return phash
     * - phash in form of long number, or null if path is incorrect
     */
    private Long calculateHash(InputStream is, int minWidth) throws IOException {
        long startTime = StatUtils.getMeasureStartTime();
        BufferedImage img = ImageIO.read(is);
        Long result = calculateHash(img, minWidth);
        StatUtils.success(startTime, CALCULATE_HASH, result);
        return result;
    }

    /**
     * Calculate phash as 64-bit long
     *
     * @param img - BufferedImage to hash
     * @return phash
     * - phash in form of long number, or null if path is incorrect
     */
    private Long calculateHash(BufferedImage img) {
        return calculateHash(img, 0);
    }

    /**
     * Calculate phash as 64-bit long
     *
     * @param img      - BufferedImage to hash
     * @param minWidth - If width is less than minWidth, the image will be scaled and blurred before processing
     * @return phash
     * - phash in form of long number, or null if path is incorrect
     */
    private Long calculateHash(BufferedImage img, int minWidth) {
        long startTime = StatUtils.getMeasureStartTime();

        // optimizing technic for small images
        if (img.getWidth() < minWidth) {
            float factor = 1 + (float) minWidth / (float) img.getWidth();

            img = resize(img, (int) (img.getWidth() * factor), (int) (img.getHeight() * factor));

            img = blur(img);
        }

        /*
         * 1. Reduce size. Like Average Hash, pHash starts with a small image.
         * However, the image is larger than 8x8; 32x32 is a good size. This is
         * really done to simplify the DCT computation and not because it is
         * needed to reduce the high frequencies.
         */
        img = resize(img, SIZE, SIZE);

        /*
         * 2. Reduce color. The image is reduced to a grayscale just to further
         * simplify the number of computations.
         */
        img = grayscale(img);

        double[][] vals = new double[SIZE][SIZE];

        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                vals[x][y] = getBlue(img, x, y);
            }
        }

        /*
         * 3. Compute the DCT. The DCT separates the image into a collection of
         * frequencies and scalars. While JPEG uses an 8x8 DCT, this algorithm
         * uses a 32x32 DCT.
         */
        double[][] dctVals = applyDCT(vals); // Cosine transform

        /*
         * 4. Reduce the DCT. This is the magic step. While the DCT is 32x32,
         * just keep the top-left 8x8. Those represent the lowest frequencies in
         * the picture.
         *
         *
         * 5. Compute the average value. Like the Average Hash, compute the mean
         * DCT value (using only the 8x8 DCT low-frequency values and excluding
         * the first term since the DC coefficient can be significantly
         * different from the other values and will throw off the average).
         */
        double total = 0;

        for (int x = 0; x < SMALLER_SIZE; x++) {
            for (int y = 0; y < SMALLER_SIZE; y++) {
                total += dctVals[x][y];
            }
        }
        total -= dctVals[0][0];
        double avg = total / (double) ((SMALLER_SIZE * SMALLER_SIZE) - 1);

        /*
         * 6. Further reduce the DCT. This is the magic step. Set the 64 hash
         * bits to 0 or 1 depending on whether each of the 64 DCT values is
         * above or below the average value. The result doesn't tell us the
         * actual low frequencies; it just tells us the very-rough relative
         * scale of the frequencies to the mean. The result will not vary as
         * long as the overall structure of the image remains the same; this can
         * survive gamma and color histogram adjustments without a problem.
         */
        long hashBits = 0;

        for (int x = 0; x < SMALLER_SIZE; x++) {
            for (int y = 0; y < SMALLER_SIZE; y++) {
                hashBits = (dctVals[x][y] > avg ? (hashBits << 1) | 0x01 : (hashBits << 1) & 0xFFFFFFFFFFFFFFFEl);
            }
        }
        StatUtils.success(startTime, CALCULATE_HASH, hashBits);
        return hashBits;
    }

    private BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    private BufferedImage grayscale(BufferedImage img) {
        colorConvert.filter(img, img);
        return img;
    }

    private BufferedImage gaussian(BufferedImage image) {
        image = getGaussianBlurFilter(20, true).filter(image, null);
        image = getGaussianBlurFilter(20, false).filter(image, null);
        return image;
    }

    private ConvolveOp getGaussianBlurFilter(int radius, boolean horizontal) {
        if (radius < 1) {
            throw new IllegalArgumentException("Radius must be >= 1");
        }

        int size = radius * 2 + 1;
        float[] data = new float[size];

        // σ, or sigma, or standard deviation, influences
        // how significantly the center pixel’s neighboring pixels affect the computations result.
        float sigma = radius / 3.0f;
        float twoSigmaSquare = 2.0f * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(twoSigmaSquare * Math.PI);
        float total = 0.0f;

        for (int i = -radius; i <= radius; i++) {
            float distance = i * i;
            int index = i + radius;
            data[index] = (float) Math.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += data[index];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= total;
        }

        Kernel kernel = null;
        if (horizontal) {
            kernel = new Kernel(size, 1, data);
        } else {
            kernel = new Kernel(1, size, data);
        }
        return new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
    }

    private BufferedImage blur(BufferedImage img) {
        BufferedImage biDest = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);

        float data[] = {0.0625f, 0.125f, 0.0625f, 0.125f, 0.25f, 0.125f, 0.0625f, 0.125f, 0.0625f};
        Kernel kernel = new Kernel(3, 3, data);
        ConvolveOp convolve = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        convolve.filter(img, biDest);
        return biDest;
    }

    private static int getBlue(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y)) & 0xff;
    }

    // DCT function stolen from
    // http://stackoverflow.com/questions/4240490/problems-with-dct-and-idct-algorithm-in-java

    private double[] c;

    private void initCoefficients() {
        c = new double[SIZE];

        for (int i = 1; i < SIZE; i++) {
            c[i] = 1;
        }
        c[0] = 1 / Math.sqrt(2.0);
    }

    private double[][] applyDCT(double[][] f) {
        int N = SIZE;

        double[][] F = new double[N][N];
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += Math.cos(((2 * i + 1) / (2.0 * N)) * u * Math.PI) * Math.cos(((2 * j + 1) / (2.0 * N)) * v * Math.PI) * (f[i][j]);
                    }
                }
                sum *= ((c[u] * c[v]) / 4.0);
                F[u][v] = sum;
            }
        }
        return F;
    }

    private String img2PNGBase64(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", out);
        byte[] bytes = out.toByteArray();

        String base64bytes = Base64.getEncoder().encodeToString(bytes);
        String src = "data:image/png;base64," + base64bytes;
        return src;
    }
}