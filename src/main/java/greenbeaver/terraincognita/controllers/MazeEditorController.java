package greenbeaver.terraincognita.controllers;

import greenbeaver.terraincognita.model.MainEngine;
import greenbeaver.terraincognita.model.MazeGrid;
import greenbeaver.terraincognita.model.UIHandler;
import greenbeaver.terraincognita.model.cellConstruction.CellType;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

import static greenbeaver.terraincognita.model.Util.LINE_SEPARATOR;

public class MazeEditorController implements Initializable {

    private enum InputState {
        CORRECT,
        EMPTY,
        WRONG,
        DANGEROUS
    }
    private HashMap<TextField, InputState> inputs = new HashMap<>();
    private double xOffset = 0;
    private double yOffset = 0;
    private MazeGrid currentMaze;

    private final static int MAX_INPUT = 35;
    private final static int DANGEROUS_RANGE = 20;
    private final static String NON_NUMERICAL_INPUT_MESSAGE = "You may only" + LINE_SEPARATOR + "use numbers";
    private final static String ZERO_INPUT_MESSAGE = "Enter a value" + LINE_SEPARATOR + "that's above zero";
    private final static String MAX_INPUT_MESSAGE = "Enter a value" + LINE_SEPARATOR + "less than " + MAX_INPUT;
    private final static String DANGEROUS_INPUT_MESSAGE = "Inputs more than "
            + DANGEROUS_RANGE
            + LINE_SEPARATOR
            + " might cause"
            + LINE_SEPARATOR
            + "visualising or"
            + LINE_SEPARATOR
            + "processing problems";
    private static final String NOT_ALL_INPUTS_MESSAGE = "You must fill" + LINE_SEPARATOR + "all input fields!";
    private static final String WRONG_INPUT_MESSAGE = "Enter correct" + LINE_SEPARATOR + "values first!";
    // TODO: handle line separation with css!!!

    @FXML
    private AnchorPane mazeContainer;
    @FXML
    private Label submissionHint;
    @FXML
    private Label mazeHeightHint;
    @FXML
    private Label mazeWidthHint;
    @FXML
    private TextField mazeHeightInput;
    @FXML
    private TextField mazeWidthInput;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        inputs.put(mazeWidthInput, InputState.EMPTY);
        inputs.put(mazeHeightInput, InputState.EMPTY);
    }

    private void universal(@Nullable TextField inputField,
                           Label hint,
                           @Nullable InputState toReplaceFor,
                           String message,
                           @Nullable String color) {
        if (inputField != null) {
            inputs.replace(inputField, toReplaceFor);
        }
        hint.setText(message);
        if (color != null) {
            hint.setTextFill(Paint.valueOf(color)); // TODO: css!!!
        }
    }

    private void checkInput(TextField inputField, Label hint) {
        if (!submissionHint.getText().isEmpty()) {
            submissionHint.setText("");
        }

        String inputText = inputField.getText();
        if (inputText.isEmpty()) {
            universal(inputField, hint, InputState.EMPTY, "", null);
            return;
        }

        if (!inputText.matches("\\d*")) {
            universal(inputField, hint, InputState.WRONG, NON_NUMERICAL_INPUT_MESSAGE, "#fb0321");
            return;
        }

        try {
            int inputValue = Integer.parseInt(inputText);
            if (inputValue < DANGEROUS_RANGE && inputValue > 0) {
                universal(inputField, hint, InputState.CORRECT, "", null);
                return;
            }

            if (inputValue == 0) {
                universal(inputField, hint, InputState.WRONG, ZERO_INPUT_MESSAGE, "#fb0321");
                return;
            }

            if (inputValue > MAX_INPUT) {
                universal(inputField, hint, InputState.WRONG, MAX_INPUT_MESSAGE, "#fb0321");
                return;
            }

            universal(inputField, hint, InputState.DANGEROUS, DANGEROUS_INPUT_MESSAGE, "#fedd00");
        } catch (NumberFormatException tooBig) {
            universal(inputField, hint, InputState.WRONG, MAX_INPUT_MESSAGE, "#fb0321");
        }
    }

    @FXML
    void checkHeightInput() {
        checkInput(mazeHeightInput, mazeHeightHint);
    }

    @FXML
    void checkWidthInput() {
        checkInput(mazeWidthInput, mazeWidthHint);
    }

    private void saveAndShowMazeGrid() {
        submissionHint.setText("");
        MainEngine.setMazeHeight(Integer.parseInt(mazeHeightInput.getText()));
        MainEngine.setMazeWidth(Integer.parseInt(mazeWidthInput.getText()));
        currentMaze = new MazeGrid(30);
        ObservableList<Node> mazeContainerChildren = mazeContainer.getChildren();
        mazeContainerChildren.clear();
        mazeContainer.getChildren().add(currentMaze);
        UIHandler.clearUIHandler();
    }

    @FXML
    private void saveProperties() throws IOException {
        InputState widthState = inputs.get(mazeWidthInput);
        InputState heightState = inputs.get(mazeHeightInput);

        if (heightState == InputState.CORRECT && widthState == InputState.CORRECT) {
            saveAndShowMazeGrid();
            return;
        }

        if (heightState == InputState.EMPTY || widthState == InputState.EMPTY) {
            universal(null, submissionHint, null, NOT_ALL_INPUTS_MESSAGE, "#fb0321");
            return;
        }

        if (heightState == InputState.WRONG || widthState == InputState.WRONG) {
            universal(null, submissionHint, null, WRONG_INPUT_MESSAGE, "#fb0321");
            return;
        }

        if (heightState == InputState.DANGEROUS || widthState == InputState.DANGEROUS) {

            Stage alarm = new Stage();
            alarm.initStyle(StageStyle.TRANSPARENT);

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/DangerousInputAlarm.fxml"));

            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            root.setOnMouseDragged(event -> {
                alarm.setX(event.getScreenX() - xOffset);
                alarm.setY(event.getScreenY() - yOffset);
            });

            alarm.setScene(new Scene(root));
            alarm.initModality(Modality.WINDOW_MODAL);
            alarm.initOwner(submissionHint.getScene().getWindow());
            alarm.showAndWait();

            if (UIHandler.getContinueWithDangerousInput()) {
                saveAndShowMazeGrid();
            }
        }
    }

    @FXML
    void solve() {
        if (CellType.fieldFilled() && UIHandler.portalNumsOK()) {
            MainEngine.setMaze(currentMaze.getMazeAsArray());
            MainEngine.setPortalTransitions(UIHandler.getPortalTransitions());
            MainEngine.solve();
        } else {
            Stage alarm = new Stage(); // TODO: make a normal alarm window!!!
            Label bad = new Label("You must have at least one entrance, escape and treasure in your maze," +
                    LINE_SEPARATOR +
                    "and each portal should have a next portal so there can't be just one of them");
            VBox box = new VBox();
            box.getChildren().add(bad);
            Scene scene = new Scene(box);
            alarm.setScene(scene);
            alarm.initModality(Modality.WINDOW_MODAL);
            alarm.initOwner(submissionHint.getScene().getWindow());
            alarm.showAndWait();
        }
    }
}
