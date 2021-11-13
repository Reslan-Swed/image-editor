package image.io;

public class Effects {
    /**
     * Weighted method or luminosity method
     * <p>
     * New grayscale image = ( (0.3 * R) + (0.59 * G) + (0.11 * B) ).
     * <p>
     * According to this equation , Red has contribute 30% , Green has contributed 59% which is greater in all three colors and Blue has contributed 11%.
     *
     * @param color
     * @return new color
     */
    public static int convertToGrayScale(int color) {
        int a = PixelColor.getAlpha(color);
        int r = PixelColor.getRed(color);
        int g = PixelColor.getGreen(color);
        int b = PixelColor.getBlue(color);
        int avg = (r + g + b) / 3;
        return PixelColor.fromARGB(a, avg, avg, avg);
    }

    public static int convertHSL(int color) {
        double a = PixelColor.getAlpha(color) / 255;
        double r = PixelColor.getRed(color) / 255;
        double g = PixelColor.getGreen(color) / 255;
        int b = PixelColor.getBlue(color);
        double minimum = 1;
        if (r < g && r < b) {
            minimum = r;
        } else if (g < r && g < b) {
            minimum = g;
        } else {
            minimum = b;
        }
        double S = 1 - (3 / (r + g + b)) * minimum;
        double I = 1 / 3 * (r + g + b);
        double up = 0.5 * (r - g + r - b);
        double down = (r - g) * (r - g) + (r - b) * Math.sqrt(g - b);
        double theta = up / down;
        double H;
        if (b <= g) {
            H = theta;
        } else {
            H = 360 - theta;
        }
        return PixelColor.fromARGB(a, (int) H, (int) S, (int) I);
    }

    public static int convertCMY(int color) {
        int a = PixelColor.getAlpha(color);
        int r = PixelColor.getRed(color);
        int g = PixelColor.getGreen(color);
        int b = PixelColor.getBlue(color);
        int C = 1 - r;
        int M = 1 - g;
        int Y = 1 - b;
        return PixelColor.fromARGB(a, C, M, Y);
    }

    public static int convertCYMK(int color) {
        int a = PixelColor.getAlpha(color);
        int r = PixelColor.getRed(color);
        int g = PixelColor.getGreen(color);
        int b = PixelColor.getBlue(color);
        int C = 1 - r;
        int M = 1 - g;
        int Y = 1 - b;
        int K;
        if (C < M && C < Y) {
            K = C;
        } else if (M < C && M < Y) {
            K = M;
        } else {
            K = Y;
        }
        C = C - K;
        M = M - K;
        Y = Y - K;
        return PixelColor.fromARGB(a, C, M, Y);
    }

    public static int convertYUV(int color) {
        int a = PixelColor.getAlpha(color);
        int r = PixelColor.getRed(color);
        int g = PixelColor.getGreen(color);
        int b = PixelColor.getBlue(color);
        double Y = 0.299 * r + 0.587 * g + 0.114 * b;
        double U = -0.14713 * r + (-0.28886 * g) + 0.436 * b;
        double V = 0.615 * r + (-0.51499 * g) + (-0.10001 * b);
        return PixelColor.fromARGB(a, (int) Y, (int) U, (int) V);
    }
}
