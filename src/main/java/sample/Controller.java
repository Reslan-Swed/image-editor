package sample;

import com.jfoenix.controls.*;
import com.jfoenix.controls.JFXSnackbar.SnackbarEvent;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.MaterialIconView;
import image.io.Effects;
import image.io.ImageManager;
import image.io.PixelColor;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private enum Effect {
        GRAY_SCALE("Gray Scale"), HSL("HSL"), CYMK("CYMK"), CYM("CYM"), YUV("YUV");

        private String value;

        Effect(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private enum Tool {
        PEN, BRUSH, EYE_DROPPER, ERASER, COLOR_FILL, TEXT,
    }

    @FXML
    private HBox statusBar;

    @FXML
    private AnchorPane backgroundAnchorPane;

    @FXML
    private ImageView imageView;

    @FXML
    private JFXToggleNode penToggle;

    @FXML
    private JFXToggleNode textToggle;

    @FXML
    private ToggleGroup toolsToggleGroup;

    @FXML
    private JFXToggleNode brushToggle;

    @FXML
    private JFXToggleNode eyeDropperToggle;

    @FXML
    private JFXToggleNode eraserToggle;

    @FXML
    private JFXToggleNode colorFillToggle;

    @FXML
    private JFXButton saveButton;

    @FXML
    private JFXButton openButton;

    @FXML
    private JFXButton createBlankButton;

    @FXML
    private JFXComboBox<Effect> effectsComboBox;

    @FXML
    private JFXSlider strokeSizeSlider;

    @FXML
    private JFXColorPicker colorPicker;

    @FXML
    private Label statusLabel;

    @FXML
    private Hyperlink statusLink;

    private ImageManager imageManager;

    private int currentColor;

    private int currentStrokeSize;

    private Effect currentEffect;

    private Tool currentTool;

    private Optional<MouseEvent> oldEvent;

    private static final boolean DEBUG = false;

    private JFXSnackbar snackbar;

    private double orgSceneX, orgSceneY;
    private double orgTranslateX, orgTranslateY;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        snackbar = new JFXSnackbar(statusBar);
        snackbar.setPrefWidth(400);

//        cropStackPane.getChildren().clear();
        currentStrokeSize = (int) strokeSizeSlider.getValue();
        currentColor = PixelColor.fromARGB(colorPicker.getValue().getOpacity(),
            colorPicker.getValue().getRed(),
            colorPicker.getValue().getGreen(),
            colorPicker.getValue().getBlue());


        currentEffect = null;

        imageManager = new ImageManager(imageView);
        imageManager.createBlank(500, 400);

        imageView.setOnDragOver(this::mouseDragOver);
        imageView.setOnDragDropped(this::mouseDragDropped);
        imageView.setOnDragExited(event -> imageView.setStyle("-fx-border-color: #bbbbbb;"));
        oldEvent = Optional.empty();

        strokeSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            imageManager.setBrushSize(newValue.intValue());
        });

        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                double alpha = newValue.getOpacity();
                double red = newValue.getRed();
                double green = newValue.getGreen();
                double blue = newValue.getBlue();

                currentColor = PixelColor.fromARGB(alpha, red, green, blue);
                if (DEBUG) System.out.println(currentColor);
            }
        });

        strokeSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                currentStrokeSize = newValue.intValue();
                if (DEBUG) System.out.println(currentStrokeSize);
            }
        });

        effectsComboBox.setConverter(new StringConverter<Effect>() {
            @Override
            public String toString(Effect object) {
                return object == null ? "" : object.toString();
            }

            @Override
            public Effect fromString(String string) {
                return Effect.valueOf(string);
            }
        });
        effectsComboBox.getSelectionModel().selectFirst();
        for (Effect e : Effect.values()) {
            effectsComboBox.getItems().add(e);
        }
        effectsComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                currentEffect = newValue;
                if (DEBUG) System.out.println(currentEffect);
            }
        });

        toolsToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == penToggle) {
                currentTool = Tool.PEN;
            } else if (newValue == brushToggle) {
                currentTool = Tool.BRUSH;
            } else if (newValue == colorFillToggle) {
                currentTool = Tool.COLOR_FILL;
            } else if (newValue == eyeDropperToggle) {
                currentTool = Tool.EYE_DROPPER;
            } else if (newValue == eraserToggle) {
                currentTool = Tool.ERASER;
            } else if (newValue == textToggle) {
                currentTool = Tool.TEXT;
            } else {
                currentTool = null;
            }
        });

    }

    private void pinNode(Node node) {
        node.setOnMousePressed(t -> {
            orgSceneX = t.getSceneX();
            orgSceneY = t.getSceneY();
            orgTranslateX = ((Node) (t.getSource())).getTranslateX();
            orgTranslateY = ((Node) (t.getSource())).getTranslateY();
        });
        node.setOnMouseDragged(t -> {
            double offsetX = t.getSceneX() - orgSceneX;
            double offsetY = t.getSceneY() - orgSceneY;
            double newTranslateX = orgTranslateX + offsetX;
            double newTranslateY = orgTranslateY + offsetY;
            ((Node) (t.getSource())).setTranslateX(newTranslateX);
            ((Node) (t.getSource())).setTranslateY(newTranslateY);
        });
        node.setCursor(Cursor.MOVE);
        backgroundAnchorPane.getChildren().add(node);
    }

    @FXML
    void onShrinkHandler(ActionEvent event) {
        imageManager.resizeBilinear(0.5);
    }

    @FXML
    void onScaleHandler(ActionEvent event) {
        imageManager.pixelReplication(2);
    }

    @FXML
    void onCombineHandler(ActionEvent event) {
        HBox toast = new HBox();
        toast.getStyleClass().add("snack-bar");
        Image image = null;
        File file = null;
        try {
            file = openFileChooser();
            if (file == null) {
                System.out.println("return");
                return;
            }
            image = new Image(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            statusLabel.setText("Failed to Open File");
            MaterialIconView icon = new MaterialIconView(MaterialIcon.ERROR_OUTLINE);
            icon.getStyleClass().add("error");
            statusLabel.setGraphic(icon);
            statusLink.setText(file.getAbsolutePath());
            File finalFile = file;
            statusLink.setOnAction(ev -> {
                try {
                    Desktop.getDesktop().open(finalFile);
                    System.out.println("Can not open file location " + finalFile.getAbsolutePath());
                } catch (IOException ex) {

                }
            });
        }
        Canvas canvas = new Canvas(image.getWidth(), image.getHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0);
        Slider slider = new Slider(0, 1, 1);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(0.2);
        slider.setMinorTickCount(1);
        canvas.opacityProperty().bind(slider.valueProperty());

        JFXButton check = new JFXButton();
        check.setGraphic(new MaterialDesignIconView(MaterialDesignIcon.CHECK));
        JFXButton cancel = new JFXButton();
        cancel.setGraphic(new MaterialDesignIconView(MaterialDesignIcon.CLOSE));
        check.setOnAction(e -> {
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.WHITE);
            parameters.setViewport(new Rectangle2D(0, 0, imageManager.getWidth(), imageManager.getHeight()));
            WritableImage textImage = canvas.snapshot(parameters, null);
//                    System.out.println("layout x : " + text.getLayoutX() + " layout y : " + text.getLayoutY());
            imageManager.combine(textImage, (c1, c2) -> {
                if (c2 == PixelColor.WHITE) {
                    return c1;
                }
                int r = PixelColor.getRed(c1) + PixelColor.getRed(c2);
                int g = PixelColor.getGreen(c1) + PixelColor.getGreen(c2);
                int b = PixelColor.getBlue(c1) + PixelColor.getBlue(c2);
                int a = PixelColor.getAlpha(c1) + PixelColor.getAlpha(c2);
                return PixelColor.fromARGB(a / 2, r / 2, g / 2, b / 2);
            });
            backgroundAnchorPane.getChildren().remove(canvas);
            snackbar.close();
        });
        cancel.setOnAction(e -> {
            backgroundAnchorPane.getChildren().remove(canvas);
            snackbar.close();
        });
        toast.getChildren().addAll(slider, check, cancel);
        pinNode(canvas);
        snackbar.fireEvent(new SnackbarEvent(toast, Duration.INDEFINITE, null));
    }

    @FXML
    void onKeyPressedHandler(KeyEvent event) {

    }

    @FXML
    void onMousePressedHandler(MouseEvent event) {
        if (DEBUG)
            System.out.println("onImageViewMouseClickedHandler: event x: " + event.getX() + " - y :" + event.getY());
        if (currentTool == null) {
            return;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (currentTool) {
            case PEN:
                imageManager.setBrush(ImageManager.Brush.DOT);
                imageManager.setPixelColor(x, y, (pX, pY, color) -> currentColor);
                break;
            case ERASER:
                imageManager.setPixelColor(x, y, (pX, pY, color) -> imageManager.getPixelDefaultColor(pX, pY));
                break;
            case BRUSH:
                imageManager.setBrush(ImageManager.Brush.MAIN_DIAMETER);
                imageManager.setPixelColor(x, y, (pX, pY, color) -> currentColor);
                break;
            case EYE_DROPPER:
                Integer currColor = imageManager.getPixelCurrentColor(x, y);
                if (currColor == null) {
                    event.consume();
                    return;
                }
                colorPicker.setValue(Color.color(PixelColor.getRed(currColor) / 255d,
                    PixelColor.getGreen(currColor) / 255d,
                    PixelColor.getBlue(currColor) / 255d,
                    PixelColor.getAlpha(currColor) / 255d));
                break;
            case COLOR_FILL:
                imageManager.flood(x, y, currentColor);
                break;
            case TEXT:
                HBox toast = new HBox();
                toast.getStyleClass().add("snack-bar");
                Text text = new Text();
                text.fillProperty().bind(colorPicker.valueProperty());
                JFXTextField textField = new JFXTextField();
                textField.setPromptText("Enter Your Text");
                textField.setLabelFloat(true);
                text.textProperty().bind(textField.textProperty());
                Slider slider = new Slider(10, 30, 10);
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(true);
                slider.setMajorTickUnit(10);
                slider.setMinorTickCount(10);
                slider.valueProperty().addListener((observable, oldValue, newValue) -> text.setFont(new Font(newValue.doubleValue())));
                JFXButton check = new JFXButton();
                check.setGraphic(new MaterialDesignIconView(MaterialDesignIcon.CHECK));
                JFXButton cancel = new JFXButton();
                cancel.setGraphic(new MaterialDesignIconView(MaterialDesignIcon.CLOSE));
                check.setOnAction(e -> {
                    SnapshotParameters parameters = new SnapshotParameters();
                    parameters.setFill(Color.WHITE);
                    parameters.setViewport(new Rectangle2D(0, 0, imageManager.getWidth(), imageManager.getHeight()));
                    WritableImage textImage = text.snapshot(parameters, null);
//                    System.out.println("layout x : " + text.getLayoutX() + " layout y : " + text.getLayoutY());
                    imageManager.combine(textImage, (c1, c2) -> {
                        if (c2 == PixelColor.WHITE) {
                            return c1;
                        }
                        return c2;
                    });
                    backgroundAnchorPane.getChildren().remove(text);
                    snackbar.close();
                    textToggle.setSelected(false);
                });
                cancel.setOnAction(e -> {
                    backgroundAnchorPane.getChildren().remove(text);
                    snackbar.close();
                    textToggle.setSelected(false);
                });
                toast.getChildren().addAll(textField, slider, check, cancel);
                text.setLayoutX(x);
                text.setLayoutY(y);
                pinNode(text);
                snackbar.fireEvent(new SnackbarEvent(toast, Duration.INDEFINITE, null));
                break;
            default:
                event.consume();
        }

    }

    @FXML
    void onMouseDraggedHandler(MouseEvent event) {
        if (currentTool == null) {
            return;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (currentTool) {
            case PEN:
                if (oldEvent.isPresent()) {
                    int oldX = (int) oldEvent.get().getX();
                    int oldY = (int) oldEvent.get().getY();
                    imageManager.setBrush(ImageManager.Brush.DOT);
                    imageManager.drawStrokeLine(oldX, oldY, x, y, (pxX, pxY, color) -> currentColor);
                }
                break;
            case BRUSH: {
                if (oldEvent.isPresent()) {
                    int oldX = (int) oldEvent.get().getX();
                    int oldY = (int) oldEvent.get().getY();
                    imageManager.setBrush(ImageManager.Brush.MAIN_DIAMETER);
                    imageManager.drawStrokeLine(oldX, oldY, x, y, (pxX, pxY, color) -> currentColor);
                }
            }
            break;
            case ERASER:
                if (oldEvent.isPresent()) {
                    int oldX = (int) oldEvent.get().getX();
                    int oldY = (int) oldEvent.get().getY();
                    imageManager.drawStrokeLine(oldX, oldY, x, y, (pxX, pxY, color) -> imageManager.getPixelDefaultColor(pxX, pxY));
                }
                break;
            default:
                event.consume();
        }
        oldEvent = Optional.of(event);
        if (DEBUG)
            System.out.println("onMouseDraggedHandler: event x: " + oldEvent.get().getX() + " - y :" + oldEvent.get().getY());
    }

    @FXML
    void onMouseExitedHandler(MouseEvent event) {
        oldEvent = Optional.empty();
    }

    @FXML
    void onMouseReleasedHandler(MouseEvent event) {
        oldEvent = Optional.empty();
    }

    @FXML
    void onNewHandler(ActionEvent event) {
        imageManager.createBlank(500, 400);
    }

    @FXML
    void onOpenHandler(ActionEvent event) {
        File file = openFileChooser();
        if (file == null) {
            return;
        }
        new LoadingTask(file).execute();
    }

    @FXML
    void onRedoHandler(ActionEvent event) {
        imageManager.reDo();
    }

    @FXML
    void onRotateLeftHandler(ActionEvent event) {
        imageManager.rotateLeft();
    }

    @FXML
    void onRotateRightHandler(ActionEvent event) {
        imageManager.rotateRight();
    }

    @FXML
    void onSaveHandler(ActionEvent event) {
        File file = saveFileChooser();
        if (file == null) {
            return;
        }
        new StoringTask(file).execute();
    }

    @FXML
    void onUndoHandler(ActionEvent event) {
        imageManager.unDo();
    }

    @FXML
    void onApplyEffectHandler(ActionEvent event) {
        if (currentEffect == null) {
            return;
        }

        switch (currentEffect) {
            case CYM:
                imageManager.applyEffect(Effects::convertCMY);
                break;
            case CYMK:
                imageManager.applyEffect(Effects::convertCYMK);
                break;
            case YUV:
                imageManager.applyEffect(Effects::convertYUV);
                break;
            case GRAY_SCALE:
                imageManager.applyEffect(Effects::convertToGrayScale);
                break;
            case HSL:
                imageManager.applyEffect(Effects::convertHSL);
                break;

        }
    }

    private void mouseDragDropped(final DragEvent e) {
        final Dragboard db = e.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            success = true;
            // Only get the first file from the list
            final File file = db.getFiles().get(0);
            new LoadingTask(file).execute();
//            Platform.runLater(() -> {
//                statusLabel.setGraphic(null);
//                statusLabel.setText(file.getAbsolutePath());
//                imageManager.loadImage(file.getAbsoluteFile());
//            });
        }
        e.setDropCompleted(success);
        e.consume();
    }

    private void mouseDragOver(final DragEvent e) {
        final Dragboard db = e.getDragboard();

        final boolean isAccepted = db.getFiles().get(0).getName().toLowerCase().endsWith(".png")
            || db.getFiles().get(0).getName().toLowerCase().endsWith(".jpeg")
            || db.getFiles().get(0).getName().toLowerCase().endsWith(".jpg");

        if (db.hasFiles()) {
            if (isAccepted) {
                imageView.setStyle("-fx-opacity: 0.5;");
                e.acceptTransferModes(TransferMode.MOVE);
            }
        } else {
            e.consume();
        }
    }

    private File saveFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG", "*.png"));
        return chooser.showSaveDialog(null);
    }

    private File openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG", "*.png"),
            new FileChooser.ExtensionFilter("JPEG", "*.jpeg"),
            new FileChooser.ExtensionFilter("JPG", "*.jpg"));
        return chooser.showOpenDialog(null);
    }

    private class LoadingTask extends AsyncTask<Void, Void, Boolean> {
        private File file;

        public LoadingTask(File file) {
            this.file = file;
        }

        @Override
        public void onPreExecute() {
            JFXSpinner statusSpinner = new JFXSpinner();
            statusSpinner.setPrefSize(20, 20);
            statusLabel.setGraphic(statusSpinner);
            statusLabel.setText("Opening File " + file.getAbsolutePath());
            statusLink.setText("");
            statusLink.setOnAction(Event::consume);
            openButton.setDisable(true);
            saveButton.setDisable(true);
            createBlankButton.setDisable(true);
        }

        @Override
        public Boolean doInBackground(Void... params) {
            WritableImage image;
            try {
                image = SwingFXUtils.toFXImage(ImageIO.read(file), null);
                imageManager.loadImage(image);
            } catch (IOException e) {
                System.err.println("Error loading image : " + file.getAbsolutePath());
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean params) {
            openButton.setDisable(false);
            saveButton.setDisable(false);
            createBlankButton.setDisable(false);
            if (params) {
                statusLabel.setGraphic(null);
                statusLabel.setText("Ready");
            } else {
                statusLabel.setText("Failed to Open File");
                MaterialIconView icon = new MaterialIconView(MaterialIcon.ERROR_OUTLINE);
                icon.getStyleClass().add("error");
                statusLabel.setGraphic(icon);
            }
            statusLink.setText(file.getAbsolutePath());
            statusLink.setOnAction(e -> {
                try {
                    Desktop.getDesktop().open(file);
                } catch (IOException ex) {
                    System.out.println("Can not open file location " + file.getAbsolutePath());
                }
            });
        }

        @Override
        public void progressCallback(Void... params) {

        }
    }

    private class StoringTask extends AsyncTask<Void, Void, Boolean> {
        private File file;

        public StoringTask(File file) {
            this.file = file;
        }

        @Override
        public void onPreExecute() {
            JFXSpinner statusSpinner = new JFXSpinner();
            statusSpinner.setPrefSize(20, 20);
            statusLabel.setGraphic(statusSpinner);
            statusLabel.setText("Saving File " + file.getAbsolutePath());
            statusLink.setText("");
            statusLink.setOnAction(Event::consume);
            saveButton.setDisable(true);
        }

        @Override
        public Boolean doInBackground(Void... params) {
//                int dot = file.getName().lastIndexOf(".");
//                String extension = file.getName().substring(dot + 1);
            return imageManager.storeImage(file, "png");
        }

        @Override
        public void onPostExecute(Boolean params) {
            saveButton.setDisable(false);
            if (params) {
                statusLabel.setText("File Saved Successfully");
                MaterialDesignIconView icon = new MaterialDesignIconView(MaterialDesignIcon.CHECK);
                icon.getStyleClass().add("check");
                statusLabel.setGraphic(icon);
                statusLink.setText(file.getAbsolutePath());
                statusLink.setOnAction(e -> {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (IOException ex) {
                        System.out.println("Can not open file location " + file.getAbsolutePath());
                    }
                });
            } else {
                statusLabel.setText("Failed to Save File " + file.getAbsolutePath());
                MaterialIconView icon = new MaterialIconView(MaterialIcon.ERROR_OUTLINE);
                icon.getStyleClass().add("error");
                statusLabel.setGraphic(icon);
            }
        }

        @Override
        public void progressCallback(Void... params) {

        }
    }

}
