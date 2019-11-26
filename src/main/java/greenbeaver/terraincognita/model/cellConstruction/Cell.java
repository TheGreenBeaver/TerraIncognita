package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;
import greenbeaver.terraincognita.model.UIHandler;
import greenbeaver.terraincognita.model.Util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Cell extends ImageView {

    private final static double MAX_FIT_SIZE = 55;
    private final static double MIN_FIT_SIZE = 20;
    private CellType cellType;
    private final Coordinate coordinate;

    public Cell(Coordinate coordinate, double fitSize) {
        super(Util.FLOOR);
        this.coordinate = coordinate;
        cellType = CellType.EMPTY;

        double actualFitSize = (fitSize < MIN_FIT_SIZE) ? MIN_FIT_SIZE : (Math.min(fitSize, MAX_FIT_SIZE));

        setFitWidth(actualFitSize);
        setFitHeight(actualFitSize);
        setOnMouseClicked(event -> {
            try {
                onClick(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public MoveResult move(Direction direction) {
        Coordinate newCoordinate = coordinate.add(direction);

        if (!newCoordinate.fits()) {
            return MoveResult.MAZE_BORDER;
        }

        Cell probableResult = MainEngine.getMaze()[newCoordinate.getY()][newCoordinate.getX()];
        if (probableResult.getCellType() == CellType.PORTAL) {
            Coordinate[] transitions = MainEngine.getPortalTransitions();
            int portalIndex = UIHandler.getNumOfPortal(probableResult.coordinate);
            Coordinate actualNewCoordinate = transitions[(portalIndex == transitions.length - 1) ? 0 : portalIndex + 1];
            Cell actualResult = MainEngine.getMaze()[actualNewCoordinate.getY()][actualNewCoordinate.getX()];
            MoveResult.setResult(actualResult);
            return MoveResult.PORTAL;
        } else {
            MoveResult.setResult(probableResult);

            if (!probableResult.getCellType().isReachable()) {
                return MoveResult.UNREACHABLE_CELL;
            }

            Coordinate temp = MainEngine.isBlindMode() ? MainEngine.getLocalCoordinate().add(direction) : newCoordinate;
            return temp.getCoordinateState() == Coordinate.CoordinateState.KNOWN_REACHABLE
                    ? MoveResult.ALREADY_VISITED_CELL
                    : MoveResult.SUCCESSFUL;
        }
    }

    public CellType getCellType() {
        return cellType;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    private void onClick(MouseEvent event) throws IOException {
        if (event.getButton() == MouseButton.PRIMARY) {
            if (cellType == CellType.PORTAL) {
                UIHandler.removePortal(coordinate);
            }
            cellType = cellType.switchType();

            switch (cellType) {
                case PORTAL: {
                    UIHandler.createPortal(coordinate);
                    break;
                }

                case ENTRANCE: {
                    MainEngine.setEntrance(coordinate);
                    break;
                }
            }

            super.setImage(cellType.getImage());
        } else if (event.getButton() == MouseButton.SECONDARY && cellType == CellType.PORTAL) {
            UIHandler.setCurrentPortal(coordinate);
            Stage numSettings = new Stage();
            numSettings.initStyle(StageStyle.TRANSPARENT);
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/PortalSettings.fxml"));
            numSettings.setScene(new Scene(root));
            numSettings.initModality(Modality.WINDOW_MODAL);
            numSettings.initOwner(((Node) event.getSource()).getScene().getWindow());
            numSettings.setX(event.getScreenX());
            numSettings.setY(event.getScreenY());
            numSettings.showAndWait();
        }
    }
}
