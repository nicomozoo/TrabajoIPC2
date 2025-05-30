package poiupv;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.NavDAOException;
import model.Navigation;
import model.User;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;

public class FXMLDocumentController implements Initializable {

    private final HashMap<String, Poi> hm = new HashMap<>();
    private ObservableList<Poi> data;
    private Group zoomGroup;
    private User currentUser;
    private Line linePainting;
    private Circle circlePainting;
    private double inicioXArc;
    private Tool currentTool = Tool.LINE;
    private Color currentColor = Color.BLACK;
    private int currentFontSize = 2;
    private ImageView transportador, ruler;

    @FXML private ScrollPane map_scrollpane;
    @FXML private Slider zoom_slider;
    @FXML private MenuButton map_pin;
    @FXML private MenuItem pin_info;
    @FXML private Label mousePosition;
    @FXML private ComboBox<Integer> fontSizeBox;
    @FXML private ColorPicker colorPicker;
    @FXML private Button botCerrarSesion;

    private enum Tool {
        LINE, CIRCLE, TEXT, POINT, RULER, PROTRACTOR
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        zoom_slider.setMin(0.2);
        zoom_slider.setMax(3.0);
        zoom_slider.setValue(0.2);
        zoom_slider.valueProperty().addListener((o, oldVal, newVal) -> zoom(newVal.doubleValue()));

        Group contentGroup = new Group();
        zoomGroup = new Group();
        Node originalContent = map_scrollpane.getContent();
        map_scrollpane.setContent(null);
        zoomGroup.getChildren().add(originalContent);
        contentGroup.getChildren().add(zoomGroup);
        map_scrollpane.setContent(contentGroup);

        zoomGroup.setOnMousePressed(this::onMousePressed);
        zoomGroup.setOnMouseDragged(this::onMouseDragged);
        
        transportador = new ImageView(new Image("/resources/transportador.png"));
        transportador.setFitWidth(800);
        transportador.setPreserveRatio(true);
        transportador.setOpacity(0.6);
        transportador.setVisible(false);

        zoomGroup.getChildren().add(transportador);     
        
        ruler = new ImageView(new Image("/resources/regla.png"));
        ruler.setFitWidth(1600);
        ruler.setPreserveRatio(true);
        ruler.setVisible(false);

        zoomGroup.getChildren().add(ruler);

        fontSizeBox.getItems().addAll(2, 4, 6, 8, 10, 12, 14, 18, 24, 36);
        fontSizeBox.setValue(currentFontSize);
        currentColor = colorPicker.getValue();

        map_scrollpane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double delta = event.getDeltaY() > 0 ? 0.05 : -0.05;
                zoom_slider.setValue(zoom_slider.getValue() + delta);
                event.consume();
            }
        });
        
        zoom(0.2);
    }
    
    private void añadirArrastre(ImageView iv, Group zg) {
        
    }

    private void zoom(double scaleValue) {
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    @FXML
    private void zoomIn(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() + 0.05);
    }

    @FXML
    private void zoomOut(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() - 0.05);
    }

    private void onMousePressed(MouseEvent e) {
        switch (currentTool) {
            case LINE -> {
                linePainting = new Line();
                linePainting.setStartX(e.getX());
                linePainting.setStartY(e.getY());
                linePainting.setEndX(e.getX());
                linePainting.setEndY(e.getY());
                linePainting.setStrokeWidth(currentFontSize);
                linePainting.setStroke(currentColor);
                zoomGroup.getChildren().add(linePainting);
            }
            case CIRCLE -> {
                circlePainting = new Circle(1);
                circlePainting.setStroke(currentColor);
                circlePainting.setFill(Color.TRANSPARENT);
                circlePainting.setCenterX(e.getX());
                circlePainting.setCenterY(e.getY());
                inicioXArc = e.getX();
                circlePainting.setStrokeWidth(currentFontSize);
                zoomGroup.getChildren().add(circlePainting);
            }
            case TEXT -> {
                Text textNode = new Text(e.getX(), e.getY(), "");
                textNode.setFont(new Font(currentFontSize));
                textNode.setFill(currentColor);
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Agregar nota");
                dialog.setHeaderText("Introduce el texto de la nota:");
                Optional<String> result = dialog.showAndWait();
                result.ifPresent(inputText -> {
                    textNode.setText(inputText);
                    zoomGroup.getChildren().add(textNode);
                });
            }
            case POINT -> {
                Circle punto = new Circle(e.getX(), e.getY(), 5);
                punto.setFill(currentColor);
                punto.setStroke(Color.BLACK);
                punto.setStrokeWidth(1);
                zoomGroup.getChildren().add(punto);
            }
        }
    }

    private void onMouseDragged(MouseEvent e) {
        switch (currentTool) {
            case LINE -> {
                if (linePainting != null) {
                    linePainting.setEndX(e.getX());
                    linePainting.setEndY(e.getY());
                }
            }
            case CIRCLE -> {
                if (circlePainting != null) {
                    double radius = Math.abs(e.getX() - inicioXArc);
                    circlePainting.setRadius(radius);
                }
            }
            case RULER -> {
                ruler.setX(zoomGroup.sceneToLocal(e.getSceneX(), e.getSceneY()).getX() - 75); // centrado
                ruler.setY(zoomGroup.sceneToLocal(e.getSceneX(), e.getSceneY()).getY() - 75);
            }            
            case PROTRACTOR -> {
                transportador.setX(zoomGroup.sceneToLocal(e.getSceneX(), e.getSceneY()).getX() - 75); // centrado
                transportador.setY(zoomGroup.sceneToLocal(e.getSceneX(), e.getSceneY()).getY() - 75);
            }
        }
        e.consume();
    }

    @FXML
    private void selectPointTool() {
        currentTool = Tool.POINT;
    }

    @FXML
    private void selectLineTool() {
        currentTool = Tool.LINE;
    }

    @FXML
    private void selectCircleTool() {
        currentTool = Tool.CIRCLE;
    }

    @FXML
    private void selectTextTool() {
        currentTool = Tool.TEXT;
    }
    
    @FXML
    private void selectMoveTool() {
        //TO-DO IMPLEMENTAR
    }    
    
    @FXML
    private void selectRulerTool() {
        currentTool = Tool.RULER;
        ruler.setVisible(!ruler.isVisible());
    }
    
    @FXML
    private void selectProtractorTool() {
        currentTool = Tool.PROTRACTOR;
        transportador.setVisible(!transportador.isVisible());
    }
    
    @FXML
    private void selectNoTool() {
        //TO-DO IMPLEMENTAR
    }

    @FXML
    private void handleColorChange(ActionEvent event) {
        currentColor = colorPicker.getValue();
    }

    @FXML
    private void handleFontSizeChange() {
        Integer selectedSize = fontSizeBox.getValue();
        if (selectedSize != null) {
            currentFontSize = selectedSize;
        }
    }

    @FXML
    private void handleClearAll() {
        zoomGroup.getChildren().removeIf(node ->
                node instanceof Line || node instanceof Circle || node instanceof Text);
    }

    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText("sceneX: " + (int) event.getSceneX() + ", sceneY: " + (int) event.getSceneY() + "\n"
                + "         X: " + (int) event.getX() + ",          Y: " + (int) event.getY());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void preguntasOnAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLPreguntasLista.fxml"));
            Parent root = loader.load();
            FXMLPreguntasListaController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            Stage newStage = new Stage();
            newStage.setScene(new Scene(root));
            newStage.setTitle("Preguntas");
            newStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBotCerrarSesion(ActionEvent event) throws NavDAOException, IOException {
        if (showAlert("Cerrar Sesión", "¿Estás seguro de que quieres cerrar sesión?")) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLInicio.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) zoom_slider.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Inicio de sesión");
            stage.show();
        }
    }

    @FXML
    private void handleBotVerSesiones(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLSesiones.fxml"));
            Parent root = loader.load();

            FXMLSesionesController controller = loader.getController();
            controller.setCurrentUser(currentUser); // Pasar el usuario actual

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Historial de sesiones");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBotEditarPerfil(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLPerfil.fxml"));
            Parent root = loader.load();

            FXMLPerfilController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Editar Perfil");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    @FXML
    private void addPoi(MouseEvent event) {
        if (event.isControlDown()) {
            Dialog<Poi> poiDialog = new Dialog<>();
            poiDialog.setTitle("Nuevo POI");
            poiDialog.setHeaderText("Introduce un nuevo punto de interés");

            Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));

            ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

            TextField nameField = new TextField();
            nameField.setPromptText("Nombre del POI");

            TextArea descArea = new TextArea();
            descArea.setPromptText("Descripción...");
            descArea.setWrapText(true);
            descArea.setPrefRowCount(4);

            VBox vbox = new VBox(10, new Label("Nombre:"), nameField, new Label("Descripción:"), descArea);
            poiDialog.getDialogPane().setContent(vbox);

            poiDialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButton) {
                    return new Poi(nameField.getText().trim(), descArea.getText().trim(), 0, 0);
                }
                return null;
            });

            Optional<Poi> result = poiDialog.showAndWait();
            if (result.isPresent()) {
                Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                Poi poi = result.get();
                poi.setPosition(localPoint);

                // Colocar el marcador visual
                map_pin.setLayoutX(localPoint.getX());
                map_pin.setLayoutY(localPoint.getY());
                map_pin.setVisible(true);
                pin_info.setText(poi.getDescription());

                // Guardar en hashmap
                hm.put(poi.getCode(), poi);
            }
        }
    }
}