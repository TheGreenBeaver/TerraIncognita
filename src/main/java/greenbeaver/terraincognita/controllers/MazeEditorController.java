package greenbeaver.terraincognita.controllers;

import greenbeaver.terraincognita.model.*;
import greenbeaver.terraincognita.model.cellConstruction.Cell;
import greenbeaver.terraincognita.model.cellConstruction.CellType;
import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MazeEditorController implements Initializable {

    private class ToggleSwitch extends Parent {

        private BooleanProperty switchedOn = new SimpleBooleanProperty(false);

        private TranslateTransition translateAnimation = new TranslateTransition(Duration.seconds(0.15));

        ToggleSwitch(double height) {
            Rectangle background = new Rectangle(height * 2, height);
            background.setArcWidth(height);
            background.setArcHeight(height);
            background.setFill(Paint.valueOf("#192522"));
            background.setStroke(Paint.valueOf("#2b5045"));

            Circle trigger = new Circle(height / 2);
            trigger.setCenterX(height / 2);
            trigger.setCenterY(height / 2);
            trigger.setFill(Paint.valueOf("#2b5045"));

            translateAnimation.setNode(trigger);

            getChildren().addAll(background, trigger);

            switchedOn.addListener((obs, oldState, newState) -> {
                translateAnimation.setToX(newState ? height : 0);
                translateAnimation.play();
            });

            setOnMouseClicked(e -> switchedOn.set(!switchedOn.get()));
        }
    }

    private enum InputState {
        CORRECT,
        EMPTY,
        WRONG,
        DANGEROUS
    }

    private HashMap<TextField, InputState> inputs = new HashMap<>();
    private MazeGrid currentMaze;

    private final static int MAX_INPUT = 35;
    private final static int DANGEROUS_RANGE = 20;
    private final static String NON_NUMERICAL_INPUT_MESSAGE = "ERROR: You may only use numbers";
    private final static String ZERO_INPUT_MESSAGE = "ERROR: Enter a value that's above zero";
    private final static String MAX_INPUT_MESSAGE = "ERROR: Enter a value less than " + MAX_INPUT;
    private final static String DANGEROUS_INPUT_MESSAGE = "WARNING: Inputs more than "
            + DANGEROUS_RANGE
            + " might cause visualising or processing problems";
    private static final String NOT_ALL_INPUTS_MESSAGE = "ERROR: You must fill all input fields";
    private static final String WRONG_INPUT_MESSAGE = "ERROR: Enter correct values first";
    private static final String ONE_PORTAL_MESSAGE = "ERROR: There can't be just one Portal in the Maze," +
            " each Portal must have a next one";
    private static final String EQUAL_PORTALS_MESSAGE = "ERROR: Some Portals have equal numbers;" +
            " right-click on them to fix this";
    private static final String NO_ESSENTIALS_MESSAGE = "ERROR: You must have an Entrance, an Exit and a Treasure in the Maze";

    @FXML
    private ImageView filler;

    @FXML
    private AnchorPane mazeContainer;

    @FXML
    private ImageView heightScull;
    @FXML
    private TextField mazeHeightInput;

    @FXML
    private ImageView widthScull;
    @FXML
    private TextField mazeWidthInput;

    @FXML
    private ImageView submissionScull;

    @FXML
    private Button solveButton;

    @FXML
    private VBox resultView;

    @FXML
    private Label treasureState;
    @FXML
    private Label exitState;
    @FXML
    private Label cCellsPassed;
    @FXML
    private Label rCellsPassed;
    @FXML
    private Label cMoves;
    @FXML
    private Label rMoves;

    @FXML
    private HBox toggle;

    private Stage hint;
    private Label hintText;

    {
        hint = new Stage();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Hint.fxml"));
            hintText = (Label) root.getChildrenUnmodifiable().get(0);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            hint.setScene(scene);
            hint.initModality(Modality.NONE);
            hint.initStyle(StageStyle.TRANSPARENT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage alarm;

    {
        alarm = new Stage();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/DangerousInputAlarm.fxml"));
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            alarm.setScene(scene);
            alarm.initModality(Modality.WINDOW_MODAL);
            alarm.initStyle(StageStyle.TRANSPARENT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ToggleSwitch stepsOrMoves;

    private void clearResults() {
        treasureState.setText("Treasure Found: ");
        exitState.setText("Exit Reached: ");
        cCellsPassed.setText("Computational Cells Passed: ");
        rCellsPassed.setText("Real Cells Passed: ");
        cMoves.setText("Computational Moves: ");
        rMoves.setText("Real Moves: ");

        stepsOrMoves.setOnMouseClicked(ev -> stepsOrMoves.switchedOn.set(!stepsOrMoves.switchedOn.get()));

        if (resultView.getChildren().size() > 8) {
            resultView.getChildren().remove(8);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Rectangle2D screenRect = Screen.getPrimary().getVisualBounds();
        double h = screenRect.getHeight();
        if (h > 800) {
            filler.setViewport(new Rectangle2D(0, 0, 230, h - 800));
            filler.setVisible(true);
        }

        inputs.put(mazeWidthInput, InputState.EMPTY);
        inputs.put(mazeHeightInput, InputState.EMPTY);

        stepsOrMoves = new ToggleSwitch(40);
        toggle.getChildren().add(1, stepsOrMoves);
    }

    private void clearScull(ImageView scull) {
        scull.setOnMouseClicked(null);
        scull.setImage(new Image("/images/normalScull.png"));
    }

    private void universal(@Nullable TextField inputField,
                           InputState toReplaceFor,
                           String message,
                           ImageView scull) {

        if (inputField != null) {
            inputs.replace(inputField, toReplaceFor);
            clearScull(submissionScull);
        }

        switch (toReplaceFor) {
            case EMPTY:

            case CORRECT: {
                clearScull(scull);
                break;
            }

            case WRONG: {
                scull.setImage(new Image("/images/errorScull.png"));
                handleScull(scull, message);
                break;
            }

            case DANGEROUS: {
                scull.setImage(new Image("/images/warningScull.png"));
                handleScull(scull, message);
                break;
            }
        }
    }

    private void handleScull(ImageView scull, String message) {
        scull.setOnMouseClicked(e -> {
            if (hint.isShowing()) {
                hint.close();
            } else {
                double x = e.getScreenX() + 5;
                double y = e.getScreenY() + 5;
                hint.setX(x);
                hint.setY(y);
                hintText.setText(message);
                if (hint.getOwner() == null) {
                    hint.initOwner(scull.getScene().getWindow());
                }
                hint.show();
            }
        });
    }

    private void checkInput(TextField inputField, ImageView scull) {
        String inputText = inputField.getText();
        if (inputText.isEmpty()) {
            universal(inputField, InputState.EMPTY, "", scull);
            return;
        }

        if (!inputText.matches("\\d*")) {
            universal(inputField, InputState.WRONG, NON_NUMERICAL_INPUT_MESSAGE, scull);
            return;
        }

        try {
            int inputValue = Integer.parseInt(inputText);
            if (inputValue < DANGEROUS_RANGE && inputValue > 0) {
                universal(inputField, InputState.CORRECT, "", scull);
                return;
            }

            if (inputValue == 0) {
                universal(inputField, InputState.WRONG, ZERO_INPUT_MESSAGE, scull);
                return;
            }

            if (inputValue > MAX_INPUT) {
                universal(inputField, InputState.WRONG, MAX_INPUT_MESSAGE, scull);
                return;
            }

            universal(inputField, InputState.DANGEROUS, DANGEROUS_INPUT_MESSAGE, scull);
        } catch (NumberFormatException tooBig) {
            universal(inputField, InputState.WRONG, MAX_INPUT_MESSAGE, scull);
        }
    }

    @FXML
    void checkHeightInput() {
        checkInput(mazeHeightInput, heightScull);
    }

    @FXML
    void checkWidthInput() {
        checkInput(mazeWidthInput, widthScull);
    }

    private void saveAndShowMazeGrid() {
        clearResults();
        MainEngine.setMazeHeight(Integer.parseInt(mazeHeightInput.getText()));
        MainEngine.setMazeWidth(Integer.parseInt(mazeWidthInput.getText()));
        currentMaze = new MazeGrid();
        currentMaze.setOnMouseClicked(e -> {
            clearScull(submissionScull);
            clearResults();
        });
        ObservableList<Node> mazeContainerChildren = mazeContainer.getChildren();
        mazeContainerChildren.clear();
        mazeContainer.getChildren().add(currentMaze);
        UIHandler.clearUIHandler();
        solveButton.setVisible(true);
    }

    @FXML
    private void saveProperties() {
        InputState widthState = inputs.get(mazeWidthInput);
        InputState heightState = inputs.get(mazeHeightInput);

        if (heightState == InputState.CORRECT && widthState == InputState.CORRECT) {
            saveAndShowMazeGrid();
            return;
        }

        if (heightState == InputState.EMPTY || widthState == InputState.EMPTY) {
            universal(null, InputState.WRONG, NOT_ALL_INPUTS_MESSAGE, submissionScull);
            return;
        }

        if (heightState == InputState.WRONG || widthState == InputState.WRONG) {
            universal(null, InputState.WRONG, WRONG_INPUT_MESSAGE, submissionScull);
            return;
        }

        if (heightState == InputState.DANGEROUS || widthState == InputState.DANGEROUS) {

            if (alarm.getOwner() == null) {
                alarm.initOwner(mazeContainer.getScene().getWindow());
            }
            alarm.showAndWait();

            if (UIHandler.getContinueWithDangerousInput()) {
                saveAndShowMazeGrid();
            }
        }
    }

    @FXML
    void solve() {

        CellType.FieldState fieldState = CellType.fieldFilled();
        boolean portalsOK = UIHandler.portalNumsOK();

        if (fieldState == CellType.FieldState.GOOD && portalsOK) {
            MainEngine.setMaze(currentMaze.getMazeAsArray());
            MainEngine.setPortalTransitions(UIHandler.getPortalTransitions());
            HashMap<Pair<Boolean, Boolean>, ArrayList<Pair<Coordinate, Boolean>>> results = new HashMap<>();
            ArrayList<Pair<Coordinate, Boolean>> res = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                try {
                    MainEngine.solve(i);
                    results.put(new Pair<>(MainEngine.exitReached(), MainEngine.treasureFound()), MainEngine.getSteps());
                    if (MainEngine.exitReached() && MainEngine.treasureFound() && (res.isEmpty() || MainEngine.getSteps().size() < res.size())) {
                        res = MainEngine.getSteps();
                        treasureState.setText("Treasure Found: " + MainEngine.getRTreasure().toString());
                        exitState.setText("Exit Reached: TRUE");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (res.isEmpty()) {
                for (Map.Entry<Pair<Boolean, Boolean>, ArrayList<Pair<Coordinate, Boolean>>> entry : results.entrySet()) {
                    if (res.isEmpty() || entry.getValue().size() < res.size()) {
                        res = entry.getValue();
                        treasureState.setText("Treasure Found: " + (entry.getKey().getB() ? MainEngine.getRTreasure().toString() : "FALSE"));
                        exitState.setText("Exit Reached: " + (entry.getKey().getA() ? "TRUE" : "FALSE"));
                    }
                }
            }

            cCellsPassed.setText("Computational Cells Passed: " + res.size());

            ListView<Label> stepsList = new ListView<>();
            ListView<Label> movesList = new ListView<>();
            int r = 0;
            int rm = 0;
            boolean delay = false;
            boolean prevR = true;
            Coordinate prev = MainEngine.getEntrance();

            for (int i = 0; i < res.size(); i++) {

                Pair<Coordinate, Boolean> cb = res.get(i);
                Coordinate coordinate = cb.getA();
                boolean real = cb.getB();
                boolean isPortal = coordinate.fits() && coordinate.getCell().getCellType() == CellType.PORTAL;

                if (real) {
                    r++;
                }

                Coordinate diff = coordinate.subtract(prev);

                int checkSum = Math.abs(diff.getX()) + Math.abs(diff.getY());

                if (i > 0 && !delay && checkSum == 1) {

                    if (real) {
                        rm++;
                    }

                    Direction direction = Direction.getByConstructor(diff.getX(), diff.getY());

                    Label moveString = new Label(direction.toString());
                    moveString.getStyleClass().addAll("mayan_text", "various_text", "step");
                    Coordinate finalPrev = prev;
                    boolean finalPrevR = prevR;
                    moveString.setOnMouseEntered(e -> {
                        finalPrev.getCell().highlight(finalPrevR);
                        if (coordinate.fits()) {
                            coordinate.getCell().highlight(real);
                        }
                    });

                    moveString.setOnMouseExited(e -> {
                        mouseExit(finalPrev);

                        if (coordinate.fits()) {
                            mouseExit(coordinate);
                        }
                    });

                    movesList.getItems().add(moveString);
                }

                if (coordinate.fits() && coordinate.getCell().getCellType().isReachable()) {
                    prev = coordinate;
                    prevR = real;
                }

                delay = isPortal;

                Label stepString = new Label(coordinate.toString());
                stepString.getStyleClass().addAll("mayan_text", "various_text", "step");
                if (coordinate.fits()) {
                    stepString.setOnMouseEntered(e -> coordinate.getCell().highlight(real));
                    stepString.setOnMouseExited(e -> mouseExit(coordinate));
                }
                stepsList.getItems().add(stepString);
            }

            rCellsPassed.setText("Real Cells Passed: " + r);
            cMoves.setText("Computational Moves: " + movesList.getItems().size());
            rMoves.setText("Real Moves: " + rm);
            if (resultView.getChildren().size() > 8) {
                resultView.getChildren().remove(8);
            }

            stepsOrMoves.setOnMouseClicked(e -> {
                stepsOrMoves.switchedOn.set(!stepsOrMoves.switchedOn.get());
                resultView.getChildren().remove(8);
                resultView.getChildren().add(stepsOrMoves.switchedOn.get() ? movesList : stepsList);
            });

            resultView.getChildren().add(stepsOrMoves.switchedOn.get() ? movesList : stepsList);
        } else {
            if (!portalsOK) {
                universal(null, InputState.WRONG, EQUAL_PORTALS_MESSAGE, submissionScull);
                return;
            }

            switch (fieldState) {
                case ONE_PORTAL: {
                    universal(null, InputState.WRONG, ONE_PORTAL_MESSAGE, submissionScull);
                    return;
                }
                case UNUSED_ESSENTIALS: {
                    universal(null, InputState.WRONG, NO_ESSENTIALS_MESSAGE, submissionScull);
                }
            }
        }
    }

    private void mouseExit(Coordinate coordinate) {
        Cell cell = coordinate.getCell();
        Image def;
        CellType type = cell.getCellType();
        if (type != CellType.PORTAL) {
            def = cell.getCellType().getImage();
        } else {
            int num = UIHandler.getNumOfPortal(coordinate);
            def = Util.NUMBERED_PORTALS[num];
        }
        cell.setImage(def);
    }
}
