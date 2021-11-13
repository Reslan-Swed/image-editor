package image.io;

public class PixelColor {

    /*
    The bit position of the 4 components is summarized in the table below
                     ALPHA     RED     GREEN   BLUE
    BIT POSITION    31 - 24  23 - 16  15 - 8   7 - 0
     */

    public static final int MAX = 255;
    public static final int MIN = 0;
    public static final int TRANSPARENT = 0x00000000;
    public static final int WHITE = 0xffffffff;

    public static int getRed(int color) {
        return (color >> 16) & 0xff;
    }

    public static int getGreen(int color) {
        return (color >> 8) & 0xff;
    }

    public static int getBlue(int color) {
        return color & 0xff;
    }

    public static int getAlpha(int color) {
        return (color >> 24) & 0xff;
    }

    public static int fromARGB(int alpha, int red, int green, int blue) {
        return ((alpha << 24) & 0xff000000) | ((red << 16) & 0xff0000) | ((green << 8) & 0xff00) | blue;
    }

    public static int fromARGB(double alpha, double red, double green, double blue) {
        int a = (int) (alpha * MAX);
        int r = (int) (red * MAX);
        int g = (int) (green * MAX);
        int b = (int) (blue * MAX);
        return fromARGB(a, r, g, b);
    }

}
