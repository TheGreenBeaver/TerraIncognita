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

public class Cell extends ImageView {

    private CellType cellType;
    private final Coordinate coordinate;

    public Cell(Coordinate coordinate) {
        super(Util.FLOOR);
        this.coordinate = coordinate;
        cellType = CellType.EMPTY;

        setFitWidth(50);
        setFitHeight(50);
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

        Coordinate temp = MainEngine.isBlindMode() ? MainEngine.getLocalCoordinate().add(direction) : newCoordinate;
        Coordinate.CoordinateState state = temp.getCoordinateState();
        if (probableResult.getCellType() == CellType.PORTAL && state == Coordinate.CoordinateState.UNKNOWN) {
            Coordinate[] transitions = MainEngine.getPortalTransitions();
            int portalIndex = UIHandler.getNumOfPortal(probableResult.coordinate);
            Coordinate actualNewCoordinate = transitions[(portalIndex == transitions.length - 1) ? 0 : portalIndex + 1];
            Cell actualResult = MainEngine.getMaze()[actualNewCoordinate.getY()][actualNewCoordinate.getX()];
            MoveResult.setResult(actualResult);
            return MoveResult.PORTAL;
        } else {

            MoveResult.setResult(probableResult);

            if (!probableResult.getCellType().isReachable() || state == Coordinate.CoordinateState.KNOWN_BAD_PORTAL) {
                return MoveResult.UNREACHABLE_CELL;
            }

            return state == Coordinate.CoordinateState.UNKNOWN
                    ? MoveResult.SUCCESSFUL
                    : MoveResult.ALREADY_VISITED_CELL;
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
            setImage(Util.NUMBERED_PORTALS[UIHandler.getNumOfPortal(coordinate)]);
        }
    }

    public void highlight(boolean real) {
        if (cellType != CellType.PORTAL) {
            this.setImage(real ? cellType.getHImage() : cellType.getPhImage());
        } else {
            int n = UIHandler.getNumOfPortal(coordinate);
            this.setImage(real ? Util.H_NUMBERED_PORTALS[n] : Util.PH_NUMBERED_PORTALS[n]);
        }
    }
}
