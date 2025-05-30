/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package poiupv;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.NavDAOException;
import model.Navigation;
import model.User;
import poiupv.Poi;

/**
 *
 * @author jsoler
 */
public class FXMLDocumentController implements Initializable {

    //=======================================
    // hashmap para guardar los puntos de interes POI
    private final HashMap<String, Poi> hm = new HashMap<>();
    private ObservableList<Poi> data;
    // ======================================
    // la variable zoomGroup se utiliza para dar soporte al zoom
    // el escalado se realiza sobre este nodo, al escalar el Group no mueve sus nodos
    private Group zoomGroup;
    private Group drawGroup;
    private User currentUser;
    private Line linePainting;
    private Arc arcPainting;
    private Circle circlePainting;
    private double inicioXArc;
    private double inicioYArc;
    private Line distanceLine;
    private Text distanceLabel;
    private Group distanceGroup;
    private double lastMouseX;
    private double lastMouseY;
    private ImageView mapa;
    private Group markerGroup;
    private double panStartX, panStartY;
    private double hScrollStart, vScrollStart;
    private ImageView transportadorView;
    private double mouseAnchorX, mouseAnchorY;



    private ListView<Poi> map_listview;
    @FXML
    private ScrollPane map_scrollpane;
    @FXML
    private Slider zoom_slider;
    @FXML
    private MenuButton map_pin;
    @FXML
    private MenuItem pin_info;
    @FXML
    private SplitPane splitPane;
    @FXML
    private Label mousePosition;
    @FXML
    private Button botCerrarSesion;
    @FXML
    private Button butClear;
    @FXML
    private ComboBox<Integer> fontSizeBox;
    private int currentFontSize = 2;
    @FXML
    private Button buttonArco;
    @FXML
    private Button buttonLine;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private Button buttonText;
    @FXML
    private Button buttonCoordinate;
    @FXML
    private Button buttonDistance;
    @FXML
    private Button buttonMover;
    @FXML
    private Button buttonAngulo;
    @FXML
    private Button buttonEraser;
    
    @FXML
    private void selectLineTool() {
        currentTool = Tool.LINE;
        updateProtractorState();
    }
    @FXML
    private void selectCircleTool() {
        currentTool = Tool.CIRCLE;
        updateProtractorState();
    }  

    @FXML
    void zoomIn(ActionEvent event) {
        //================================================
        // el incremento del zoom dependerá de los parametros del 
        // slider y del resultado esperado
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal += 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal + -0.1);
    }
    
@FXML
private void selectAnguloTool(ActionEvent event) {
    currentTool = Tool.ANGULO;

    // Если транспортира нет (еще не создан или был удален) — создаем заново
    if (transportadorView == null) {
        transportadorView = new ImageView(
            new Image(getClass().getResource("/resources/transportador.png").toExternalForm())
        );
        transportadorView.setOpacity(0.5);
        transportadorView.setFitWidth(200);
        transportadorView.setPreserveRatio(true);
        transportadorView.setLayoutX(100);
        transportadorView.setLayoutY(100);

        makeDraggable(transportadorView);
        enableRemovalOnClick(transportadorView);

        zoomGroup.getChildren().add(transportadorView);
    }
    
    updateProtractorState();
}

    private void makeDraggable(Node node) {
    node.setOnMousePressed(event -> {
        mouseAnchorX = event.getSceneX() - node.getLayoutX();
        mouseAnchorY = event.getSceneY() - node.getLayoutY();
    });
    node.setOnMouseDragged(event -> {
        node.setLayoutX(event.getSceneX() - mouseAnchorX);
        node.setLayoutY(event.getSceneY() - mouseAnchorY);
    });
}

   private void updateProtractorState() {
    if (transportadorView == null) return;

    if (currentTool == Tool.ANGULO) {
        transportadorView.setMouseTransparent(false);
        transportadorView.toFront();
    } else if (currentTool == Tool.ERASER) {
        // в режиме резинки он тоже должен ловить клики
        transportadorView.setMouseTransparent(false);
        transportadorView.toFront();
    } else {
        // во всех остальных режимах — пропускать все клики сквозь себя
        transportadorView.setMouseTransparent(true);
    }
}
    
    
    // esta funcion es invocada al cambiar el value del slider zoom_slider
    private void zoom(double scaleValue) {
        //===================================================
        //guardamos los valores del scroll antes del escalado
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();
        //===================================================
        // escalamos el zoomGroup en X e Y con el valor de entrada
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);
        //===================================================
        // recuperamos el valor del scroll antes del escalado
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    void listClicked(MouseEvent event) {
        Poi itemSelected = map_listview.getSelectionModel().getSelectedItem();

        // Animación del scroll hasta la mousePosistion del item seleccionado
        double mapWidth = zoomGroup.getBoundsInLocal().getWidth();
        double mapHeight = zoomGroup.getBoundsInLocal().getHeight();
        double scrollH = itemSelected.getPosition().getX() / mapWidth;
        double scrollV = itemSelected.getPosition().getY() / mapHeight;
        final Timeline timeline = new Timeline();
        final KeyValue kv1 = new KeyValue(map_scrollpane.hvalueProperty(), scrollH);
        final KeyValue kv2 = new KeyValue(map_scrollpane.vvalueProperty(), scrollV);
        final KeyFrame kf = new KeyFrame(Duration.millis(500), kv1, kv2);
        timeline.getKeyFrames().add(kf);
        timeline.play();

        // movemos el objto map_pin hasta la mousePosistion del POI
//        double pinW = map_pin.getBoundsInLocal().getWidth();
//        double pinH = map_pin.getBoundsInLocal().getHeight();
        map_pin.setLayoutX(itemSelected.getPosition().getX());
        map_pin.setLayoutY(itemSelected.getPosition().getY());
        pin_info.setText(itemSelected.getDescription());
        map_pin.setVisible(true);
    }

    @FXML
    private void handleColorChange(ActionEvent event) {
    }

    @FXML
    private void selectCoordinateTool(ActionEvent event) {
        currentTool = Tool.COORDINATE;
        updateProtractorState();
    }

    private Point2D distanceStartPoint;
    
    @FXML
    private void selectDistanceTool() {
        currentTool = Tool.DISTANCE;
        distanceStartPoint = null;
        updateProtractorState();
    }

    @FXML
    private void selectPanTool(ActionEvent event) {
        currentTool = Tool.PAN;
        updateProtractorState();
    }

    @FXML
    private void selectEraserTool(ActionEvent event) {
        currentTool = Tool.ERASER;
        updateProtractorState();
    }
    
    private enum Tool {
    LINE, CIRCLE, TEXT,COORDINATE,DISTANCE, PAN, ANGULO, ERASER
    }
    
    private void handleColorChange() {
        currentColor = colorPicker.getValue();
    }
    
    private Tool currentTool = Tool.PAN;
    private Color currentColor = Color.BLACK;

    private void initData() {
        
    }
    
    @FXML
    private void selectTextTool() {
        currentTool = Tool.TEXT;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
        initData();
        //==========================================================
        // inicializamos el slider y enlazamos con el zoom
        zoom_slider.setMin(0.5);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(1.0);
        zoom_slider.valueProperty().addListener((o, oldVal, newVal) -> zoom((Double) newVal));
        mapa = new ImageView(new Image(getClass().getResource("/resources/carta_nautica.jpg").toExternalForm()));
        mapa.setScaleX(0.5);
        mapa.setScaleY(0.5);
        //=========================================================================
        //Envuelva el contenido de scrollpane en un grupo para que 
        //ScrollPane vuelva a calcular las barras de desplazamiento tras el escalad
        Group contentGroup = new Group();
        zoomGroup = new Group();
        contentGroup.getChildren().add(zoomGroup);
        zoomGroup.getChildren().add(map_scrollpane.getContent());
        map_scrollpane.setContent(contentGroup);
        
        //Inicializamos manejadores
        zoomGroup.setOnMousePressed(this::onMousePressed);
        zoomGroup.setOnMouseDragged(this::onMouseDragged);
        
        fontSizeBox.getItems().addAll(2, 4, 6, 8, 10, 12, 14, 18, 24, 36);
        fontSizeBox.setValue(currentFontSize);

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
                node instanceof Line ||
                node instanceof Arc ||
                node instanceof Text ||
                node instanceof Group);
    }
    
private void enableRemovalOnClick(Node node) {
    node.setOnMouseClicked(e -> {
        if (currentTool == Tool.ERASER) {
            zoomGroup.getChildren().remove(node);
            if (node == transportadorView) {
                transportadorView = null;
            }
        }
    });
}
    
    
    private void onMousePressed(MouseEvent e) {
    if (!e.isPrimaryButtonDown()){return;}
    switch (currentTool) {
        case PAN -> {
           if(currentTool!=Tool.ANGULO){ panStartX = e.getSceneX();
            panStartY = e.getSceneY();
            hScrollStart = map_scrollpane.getHvalue();
            vScrollStart = map_scrollpane.getVvalue();
            e.consume();}
        }
        case LINE -> {
            linePainting = new Line();
            linePainting.setStartX(e.getX());
            linePainting.setStartY(e.getY());
            linePainting.setEndX(e.getX());
            linePainting.setEndY(e.getY());
            linePainting.setStrokeWidth(currentFontSize);
            linePainting.setStroke(colorPicker.getValue());
            addContextMenuToLine(linePainting);
            enableRemovalOnClick(linePainting);
            zoomGroup.getChildren().add(linePainting);
        }
        case CIRCLE -> {
            arcPainting = new Arc();
            arcPainting.setStroke(colorPicker.getValue());
            arcPainting.setFill(Color.TRANSPARENT);
            arcPainting.setType(ArcType.OPEN);
            arcPainting.setStrokeWidth(currentFontSize);        
            arcPainting.setPickOnBounds(false);
            
            inicioXArc = e.getX();
            inicioYArc = e.getY();

            arcPainting.setCenterX(inicioXArc);
            arcPainting.setCenterY(inicioYArc);
            arcPainting.setRadiusX(1);
            arcPainting.setRadiusY(1);
            enableRemovalOnClick(arcPainting);
            zoomGroup.getChildren().add(arcPainting);
            addContextMenuToCircle(arcPainting);
        }
        case TEXT -> {
            Text textNode = new Text(e.getX(), e.getY(), "");
            int size = fontSizeBox.getValue();
            Color color = colorPicker.getValue();
            textNode.setFont(new Font(size));
            textNode.setFill(color);
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Agregar nota");
            dialog.setHeaderText("Introduce el texto de la nota:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(inputText -> {
            textNode.setText(inputText);
            enableRemovalOnClick(textNode);
            addContextMenuToText(textNode);
            zoomGroup.getChildren().add(textNode);
            });
        }
        case COORDINATE -> {
            double x = e.getX();
            double y = e.getY();
            Line diagonal1 = new Line(x - 5, y - 5, x + 5, y + 5);
            Line diagonal2 = new Line(x - 5, y + 5, x + 5, y - 5);

            diagonal1.setStroke(colorPicker.getValue());
            diagonal2.setStroke(colorPicker.getValue());
            diagonal1.setStrokeWidth(1.5);
            diagonal2.setStrokeWidth(1.5);

            Group cross = new Group(diagonal1, diagonal2);
            enableRemovalOnClick(cross);
            zoomGroup.getChildren().add(cross);
            addContextMenuToNode(cross);
        }
        
        case DISTANCE -> {
    double startX = e.getX();
    double startY = e.getY();

    distanceLine = new Line(startX, startY, startX, startY);
    distanceLine.setStroke(colorPicker.getValue());
    distanceLine.setStrokeWidth(currentFontSize);
    distanceLabel = new Text(startX, startY - 5, "");
    distanceLabel.setFill(Color.RED);
    distanceLabel.setFont(Font.font(12));

    distanceGroup = new Group(distanceLine, distanceLabel);
    addContextMenuToNode(distanceGroup);
    enableRemovalOnClick(distanceGroup);
    zoomGroup.getChildren().add(distanceGroup);
}


    }
}
    
    private void onMouseDragged(MouseEvent e) {
    switch (currentTool) {
        case PAN -> {
            if(currentTool!=Tool.ANGULO){double dx = e.getSceneX() - panStartX;
            double dy = e.getSceneY() - panStartY;

            double width = map_scrollpane.getContent().getBoundsInLocal().getWidth();
            double height = map_scrollpane.getContent().getBoundsInLocal().getHeight();

    double newH = hScrollStart - dx / width;
    double newV = vScrollStart - dy / height;


    newH = Math.max(0, Math.min(newH, 1));
    newV = Math.max(0, Math.min(newV, 1));

    map_scrollpane.setHvalue(newH);
    map_scrollpane.setVvalue(newV);

    e.consume();}
        }
        case LINE -> {
            if (linePainting != null) {
                linePainting.setEndX(e.getX());
                linePainting.setEndY(e.getY());
            }
        }
        case CIRCLE -> {
            if (arcPainting != null) {
                double finX = e.getX();
                double finY = e.getY();

                double centerX = (inicioXArc + finX) / 2;
                double centerY = (inicioYArc + finY) / 2;

                double dx = finX - inicioXArc;
                double dy = finY - inicioYArc;
                double radius = Math.sqrt(dx * dx + dy * dy) / 2;

                double angle = Math.toDegrees(Math.atan2(-dy, dx));
                angle = (angle + 360) % 360;

                arcPainting.setCenterX(centerX);
                arcPainting.setCenterY(centerY);
                arcPainting.setRadiusX(radius);
                arcPainting.setRadiusY(radius);
                arcPainting.setStartAngle(angle);
                arcPainting.setLength(180); // полукруг
            }
        }
        case DISTANCE -> {
            if (distanceLine != null && distanceLabel != null) {
        double endX = e.getX();
        double endY = e.getY();

        distanceLine.setEndX(endX);
        distanceLine.setEndY(endY);

        double dx = endX - distanceLine.getStartX();
        double dy = endY - distanceLine.getStartY();
        double length = Math.sqrt(dx * dx + dy * dy);

        distanceLabel.setText(String.format("%.1f px", length));
        distanceLabel.setX((distanceLine.getStartX() + endX) / 2);
        distanceLabel.setY((distanceLine.getStartY() + endY) / 2 - 5);
    }
        }
    }
     e.consume();
    }
    
    private void addContextMenuToLine(Line line) {
    line.setOnContextMenuRequested(e -> {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem eliminar = new MenuItem("Eliminar línea");

        eliminar.setOnAction(ev -> {
            zoomGroup.getChildren().remove(line);
        });

        contextMenu.getItems().add(eliminar);
        contextMenu.show(line, e.getScreenX(), e.getScreenY());
        e.consume();
    });
}
    
    private void addContextMenuToNode(Node node) {
    ContextMenu contextMenu = new ContextMenu();
    MenuItem deleteItem = new MenuItem("Eliminar marca");

    deleteItem.setOnAction(e -> zoomGroup.getChildren().remove(node));
    contextMenu.getItems().add(deleteItem);

    node.setOnContextMenuRequested(event -> {
        contextMenu.show(node, event.getScreenX(), event.getScreenY());
        event.consume();
    });
}

    
    private void addContextMenuToText(Text textNode) {
    ContextMenu contextMenu = new ContextMenu();
    MenuItem deleteItem = new MenuItem("Eliminar texto");

    deleteItem.setOnAction(e -> zoomGroup.getChildren().remove(textNode));
    contextMenu.getItems().add(deleteItem);

    textNode.setOnContextMenuRequested(e -> {
        contextMenu.show(textNode, e.getScreenX(), e.getScreenY());
        e.consume();
    });
}
    
    private void addContextMenuToCircle(Arc circle) {
    ContextMenu contextMenu = new ContextMenu();
    MenuItem deleteItem = new MenuItem("Eliminar circulo");

    deleteItem.setOnAction(e -> zoomGroup.getChildren().remove(circle));
    contextMenu.getItems().add(deleteItem);

    circle.setOnContextMenuRequested(e -> {
        contextMenu.show(circle, e.getScreenX(), e.getScreenY());
        e.consume();
    });
}



    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText("sceneX: " + (int) event.getSceneX() + ", sceneY: " + (int) event.getSceneY() + "\n"
                + "         X: " + (int) event.getX() + ",          Y: " + (int) event.getY());
    }

    private void closeApp(ActionEvent event) {
        ((Stage) zoom_slider.getScene().getWindow()).close();
    }

    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);
        // Acceder al Stage del Dialog y cambiar el icono
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("IPC - 2025");
        mensaje.showAndWait();
    }

    @FXML
    private void addPoi(MouseEvent event) {

        if (event.isControlDown()) {
            Dialog<Poi> poiDialog = new Dialog<>();
            poiDialog.setTitle("Nuevo POI");
            poiDialog.setHeaderText("Introduce un nuevo POI");
            // Acceder al Stage del Dialog y cambiar el icono
            Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));

            ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

            TextField nameField = new TextField();
            nameField.setPromptText("Nombre del POI");

            TextArea descArea = new TextArea();
            descArea.setPromptText("Descripción...");
            descArea.setWrapText(true);
            descArea.setPrefRowCount(5);

            VBox vbox = new VBox(10, new Label("Nombre:"), nameField, new Label("Descripción:"), descArea);
            poiDialog.getDialogPane().setContent(vbox);

            poiDialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButton) {
                    return new Poi(nameField.getText().trim(), descArea.getText().trim(), 0, 0);
                }
                return null;
            });
            Optional<Poi> result = poiDialog.showAndWait();

            if(result.isPresent()) {
                Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                Poi poi=result.get();
                poi.setPosition(localPoint);
                map_listview.getItems().add(poi);
            }
        }
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    @FXML
    private void handleBotCerrarSesion(ActionEvent event) throws NavDAOException, IOException {
    // Obtener el usuario actual
        if(showAlert("Cerrar Sesión", "¿Estás seguro de que quieres cerrar sesión?")){
            try {
                // Volver al login
                FXMLLoader loader = new FXMLLoader(getClass().getResource("FXMLInicio.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) zoom_slider.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Inicio de sesión");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            
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
    
    private boolean showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        
        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    
}
