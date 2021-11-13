package image.io;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

public class ImageManager {

    private final ImageView imageViewReference;
    private WritableImage originalImage, currentImage;
    private final Stack<WritableImage> forwardStack, backwardStack;
    private int width, height;
    private static final int MAX_UNDO = 10;
    private int brushSize;
    private Brush brush;

    public enum Brush {
        DOT, PLUS, MAIN_DIAMETER, SECONDARY_DIAMETER,
    }


    public ImageManager(ImageView imageViewReference) {
        backwardStack = new Stack<>();
        backwardStack.setSize(MAX_UNDO);
        forwardStack = new Stack<>();
        forwardStack.setSize(MAX_UNDO);
        this.imageViewReference = imageViewReference;
        this.brushSize = 1;
        this.brush = Brush.MAIN_DIAMETER;
    }

    public void loadImage(WritableImage image) {
        backwardStack.clear();
        forwardStack.clear();
        originalImage = image;
        setImageView(originalImage);
    }

    public boolean storeImage(File fileName, String formatName) {
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(currentImage, null), formatName, fileName);
        } catch (IOException e) {
            System.err.println("Error storing image : " + fileName.getAbsolutePath());
            return false;
        }
        return true;
    }

    public void applyEffect(PixelTriFunction pixelTriFunction) {
        WritableImage imageCopy = new WritableImage(width, height);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = pixelReader.getArgb(x, y);
                pixelWriter.setArgb(x, y, pixelTriFunction.apply(x, y, pixelColor));
            }
        }
        setImageView(imageCopy);
    }

    public void applyEffect(PixelUnaryFunction pixelUnaryFunction) {
        WritableImage imageCopy = new WritableImage(width, height);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = pixelReader.getArgb(x, y);
                pixelWriter.setArgb(x, y, pixelUnaryFunction.apply(pixelColor));
            }
        }
        setImageView(imageCopy);
    }

    public void setPixelColor(int x, int y, PixelTriFunction pixelTriFunction) {
        if (!inBounds(x, y)) {
            return;
        }
        applyEffect(color -> color);
        paintPixel(x, y, pixelTriFunction);
    }

    private void paintPixel(int x, int y, PixelTriFunction pixelTriFunction) {
        if (brushSize == 1) {
            currentImage.getPixelWriter().setArgb(x, y, pixelTriFunction.apply(x, y, currentImage.getPixelReader().getArgb(x, y)));
            return;
        }
        int factor = (brushSize - 1) / 2;
        switch (brush) {
            case DOT:
                for (int i = y - factor; i <= y + factor; i++) {
                    for (int j = x - factor; j <= x + factor; j++) {
                        int x0 = Math.max(Math.min(j, width - 1), 0);
                        int y0 = Math.max(Math.min(i, height - 1), 0);
                        currentImage.getPixelWriter().setArgb(x0, y0, pixelTriFunction.apply(x0, y0, currentImage.getPixelReader().getArgb(x0, y0)));
                    }
                }
                break;
            case PLUS:
                for (int i = y - factor; i <= y + factor; i++) {
                    int y0 = Math.max(Math.min(i, height - 1), 0);
                    currentImage.getPixelWriter().setArgb(x, y0, currentImage.getPixelReader().getArgb(x, y0));
                }
                for (int j = x - factor; j <= x + factor; j++) {
                    int x0 = Math.max(Math.min(j, width - 1), 0);
                    currentImage.getPixelWriter().setArgb(x0, y, currentImage.getPixelReader().getArgb(x0, y));
                }
                break;
            case MAIN_DIAMETER:
                for (int i = 0; i < brushSize; i++) {
                    int x0 = Math.max(Math.min(x - factor + i, width - 1), 0);
                    int y0 = Math.max(Math.min(y - factor + i, height - 1), 0);
                    currentImage.getPixelWriter().setArgb(x0, y0, pixelTriFunction.apply(x0, y0, currentImage.getPixelReader().getArgb(x0, y0)));
                }
                break;
            case SECONDARY_DIAMETER:
                for (int i = 0; i < brushSize; i++) {
                    int x0 = Math.max(Math.min(x + factor - i, width - 1), 0);
                    int y0 = Math.max(Math.min(y - factor + i, height - 1), 0);
                    currentImage.getPixelWriter().setArgb(x0, y0, pixelTriFunction.apply(x0, y0, currentImage.getPixelReader().getArgb(x0, y0)));
                }
                break;

        }
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public void createBlank(int width, int height) {
        WritableImage blankImage = new WritableImage(width, height);
        PixelWriter pixelWriter = blankImage.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelWriter.setArgb(x, y, PixelColor.WHITE);
            }
        }
        backwardStack.clear();
        forwardStack.clear();
        originalImage = blankImage;
        setImageView(originalImage);
    }

    private void setImageView(WritableImage image) {
        forwardStack.clear();
        if (forwardStack.size() + backwardStack.size() >= MAX_UNDO) {
            backwardStack.remove(0);
        }
        currentImage = image;
        width = (int) currentImage.getWidth();
        height = (int) currentImage.getHeight();
        backwardStack.push(currentImage);
        imageViewReference.setImage(currentImage);
        imageViewReference.setFitHeight(height);
        imageViewReference.setFitWidth(width);
        imageViewReference.setPreserveRatio(true);
    }

    public void pixelReplication(int factor) {
        int scaledWidth = factor * width;
        int scaledHeight = factor * height;
        WritableImage imageCopy = new WritableImage(scaledWidth, scaledHeight);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        for (int i = 0; i < scaledHeight; i++) {
            for (int j = 0; j < scaledWidth; j++) {
                int y = Math.min(Math.round(i / factor), height - 1);
                int x = Math.min(Math.round(j / factor), width - 1);
                pixelWriter.setArgb(j, i, pixelReader.getArgb(x, y));
            }
        }
        setImageView(imageCopy);
    }

    //resizeBilinear
    public void resizeBilinear(double factor) {
        int w2 = (int) (width * factor);
        int h2 = (int) (height * factor);
        WritableImage imageCopy = new WritableImage(w2, h2);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        int a, b, c, d, x, y;
        float x_ratio = ((float) (width - 1)) / w2;
        float y_ratio = ((float) (height - 1)) / h2;
        float x_diff, y_diff, blue, red, green;
        for (int i = 0; i < h2; i++) {
            for (int j = 0; j < w2; j++) {
                x = (int) (x_ratio * j);
                y = (int) (y_ratio * i);
                x_diff = (x_ratio * j) - x;
                y_diff = (y_ratio * i) - y;
                a = pixelReader.getArgb(x, y);
                b = pixelReader.getArgb(x + 1, y);
                c = pixelReader.getArgb(x, y + 1);
                d = pixelReader.getArgb(x + 1, y + 1);

                // blue element
                // Yb = Ab(1-w)(1-h) + Bb(w)(1-h) + Cb(h)(1-w) + Db(wh)
                blue = PixelColor.getBlue(a) * (1 - x_diff) * (1 - y_diff) + PixelColor.getBlue(b) * (x_diff) * (1 - y_diff) +
                    PixelColor.getBlue(c) * (y_diff) * (1 - x_diff) + PixelColor.getBlue(d) * (x_diff * y_diff);

                // green element
                // Yg = Ag(1-w)(1-h) + Bg(w)(1-h) + Cg(h)(1-w) + Dg(wh)
                green = PixelColor.getGreen(a) * (1 - x_diff) * (1 - y_diff) + PixelColor.getGreen(b) * (x_diff) * (1 - y_diff) +
                    PixelColor.getGreen(c) * (y_diff) * (1 - x_diff) + PixelColor.getGreen(d) * (x_diff * y_diff);

                // red element
                // Yr = Ar(1-w)(1-h) + Br(w)(1-h) + Cr(h)(1-w) + Dr(wh)
                red = PixelColor.getRed(a) * (1 - x_diff) * (1 - y_diff) + PixelColor.getRed(b) * (x_diff) * (1 - y_diff) +
                    PixelColor.getRed(c) * (y_diff) * (1 - x_diff) + PixelColor.getRed(d) * (x_diff * y_diff);

                pixelWriter.setArgb(j, i, PixelColor.fromARGB(0xff, (int) red, (int) green, (int) blue));
            }
        }
        setImageView(imageCopy);
    }

    public void unDo() {
        if (backwardStack.size() == 1) {
            return;
        }
        forwardStack.push(backwardStack.pop());
        currentImage = backwardStack.peek();
        width = (int) currentImage.getWidth();
        height = (int) currentImage.getHeight();
        imageViewReference.setImage(currentImage);
        imageViewReference.setFitHeight(height);
        imageViewReference.setFitWidth(width);
        imageViewReference.setPreserveRatio(true);
    }

    public void reDo() {
        if (forwardStack.isEmpty()) {
            return;
        }
        backwardStack.push(forwardStack.pop());
        currentImage = backwardStack.peek();
        width = (int) currentImage.getWidth();
        height = (int) currentImage.getHeight();
        imageViewReference.setImage(currentImage);
        imageViewReference.setFitHeight(height);
        imageViewReference.setFitWidth(width);
        imageViewReference.setPreserveRatio(true);
    }

    public void drawStrokeLine(int x0, int y0, int x1, int y1, PixelTriFunction pixelTriFunction) {

        x0 = Math.min(x0, width - 1);
        x1 = Math.min(x1, width - 1);
        y0 = Math.min(y0, height - 1);
        y1 = Math.min(y1, height - 1);

        x0 = Math.max(x0, 0);
        x1 = Math.max(x1, 0);
        y0 = Math.max(y0, 0);
        y1 = Math.max(y1, 0);

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int err = dx - dy;
        int e2;

//        PixelReader pixelReader = currentImage.getPixelReader();
//        PixelWriter pixelWriter = currentImage.getPixelWriter();

        while (true) {
            paintPixel(x0, y0, pixelTriFunction);

            if (x0 == x1 && y0 == y1)
                break;

            e2 = 2 * err;
            if (e2 > -dy) {
                err = err - dy;
                x0 += sx;
            }

            if (e2 < dx) {
                err = err + dx;
                y0 += sy;
            }
        }
    }

    public Integer getPixelDefaultColor(int x, int y) {
        if (!inBounds(x, y)) {
            return null;
        }
        return originalImage.getPixelReader().getArgb(x, y);
    }

    public Integer getPixelCurrentColor(int x, int y) {
        if (!inBounds(x, y)) {
            return null;
        }
        return currentImage.getPixelReader().getArgb(x, y);

    }

    public void flood(int x, int y, int fillColor) {
        if (inBounds(x, y)) {
            int old = getPixelCurrentColor(x, y);
            if (old == fillColor) return;
            applyEffect(color -> color);
            int oldSize = brushSize;
            brushSize = 1;
            floodHelper(old, x, y, fillColor);
            brushSize = oldSize;
        }
    }

    private void floodHelper(int oldColor, int pX, int pY, int fillColor) {
        if (oldColor != getPixelCurrentColor(pX, pY)) return;
        int x = pX;
        int y = pY;
        int sx;
        while (x > 0 && oldColor == getPixelCurrentColor(x - 1, y)) {
            x--;
        }
        //if (old!=img.getRGB(x, y)) return;
        drawStrokeLine(x, y, pX, y, (x1, y1, color) -> fillColor);

        sx = x;
        x = pX;
        while (x < width - 1 && oldColor == getPixelCurrentColor(x + 1, y)) {
            //if (old!=img.getRGB(x+1, y)) return;
            if (x < width - 1) x++;
            else break;
        }
        drawStrokeLine(x, y, pX, y, (x1, y1, color) -> fillColor);

        // on range sx - x scan if y-1 or y+1 = old.
        // if so, horiz fill
        while (sx <= x) {
            if (y > 0 && oldColor == getPixelCurrentColor(sx, y - 1)) {
                floodHelper(oldColor, sx, y - 1, fillColor);
            }
            if (y < height - 1 && oldColor == getPixelCurrentColor(sx, y + 1)) {
                floodHelper(oldColor, sx, y + 1, fillColor);
            }
            sx++;
        }

    }

    public void combine(WritableImage image, ColorMixer colorMixer) {
        WritableImage imageCopy = new WritableImage(width, height);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = pixelReader.getArgb(x, y);
//                if (x >= pX && x < pX + image.getWidth() && y >= pY && y < pY + image.getHeight()) {
                pixelWriter.setArgb(x, y, colorMixer.apply(pixelColor, image.getPixelReader().getArgb(x, y)));
//                    System.out.println("pixel x : " + x + " - y : " + y);
//                } else {
//                    pixelWriter.setArgb(x, y, pixelColor);
//                }
            }
        }
        setImageView(imageCopy);
    }

    public void rotateRight() {
        WritableImage imageCopy = new WritableImage(height, width);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = pixelReader.getArgb(x, y);
                pixelWriter.setArgb(height - 1 - y, x, pixelColor);
            }
        }
        setImageView(imageCopy);
    }

    public void rotateLeft() {
        WritableImage imageCopy = new WritableImage(height, width);
        PixelReader pixelReader = currentImage.getPixelReader();
        PixelWriter pixelWriter = imageCopy.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelColor = pixelReader.getArgb(x, y);
                pixelWriter.setArgb(y, width - 1 - x, pixelColor);
            }
        }
        setImageView(imageCopy);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = brushSize;
    }

    public void setBrush(Brush brush) {
        this.brush = brush;
    }
}
