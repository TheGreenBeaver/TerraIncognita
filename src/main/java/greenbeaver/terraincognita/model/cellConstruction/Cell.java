package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;
import greenbeaver.terraincognita.model.Util;
import javafx.scene.image.ImageView;

public class Cell extends ImageView {

    private final static double MAX_FIT_SIZE = 55;
    private final static double MIN_FIT_SIZE = 40;
    private CellType cellType;
    private final Coordinate coordinate;

    public Cell(Coordinate coordinate, double fitSize) {
        super(Util.FLOOR);
        this.coordinate = coordinate;
        cellType = CellType.EMPTY;

        double actualFitSize = (fitSize < MIN_FIT_SIZE) ? MIN_FIT_SIZE : (Math.min(fitSize, MAX_FIT_SIZE));

        setFitWidth(actualFitSize);
        setFitHeight(actualFitSize);
        setOnMouseClicked(e -> onClick());
    }

    public MoveResult move(Direction direction) {
        Coordinate newCoordinate = coordinate.add(direction);

        if (!newCoordinate.fits()) {
            return MoveResult.MAZE_BORDER;
        }

        Cell probableResult = MainEngine.getMaze()[newCoordinate.getY()][newCoordinate.getX()];

        MoveResult.setResult(probableResult);

        if (!probableResult.getCellType().isReachable()) {
            return MoveResult.UNREACHABLE_CELL;
        }

        return (!MainEngine.coordinateUnknown(newCoordinate) && !MainEngine.isForcedReach())
                ? MoveResult.ALREADY_VISITED_CELL
                :  MoveResult.SUCCESSFUL;
    }

    public CellType getCellType() {
        return cellType;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    private void onClick() {
        cellType = cellType.switchType();
        if (cellType == CellType.ENTRANCE) {
            MainEngine.setEntrance(coordinate);
        }
        super.setImage(cellType.getImage());
    }
}
