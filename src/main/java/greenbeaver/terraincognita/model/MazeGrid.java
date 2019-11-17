package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Cell;
import greenbeaver.terraincognita.model.cellConstruction.CellType;
import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import javafx.scene.layout.GridPane;

public class MazeGrid extends GridPane {

    private Cell[][] mazeAsArray;

    public MazeGrid(double cellSize) {
        setGridLinesVisible(true);
        CellType.flush();
        mazeAsArray = new Cell[MainEngine.getMazeHeight()][MainEngine.getMazeWidth()];

        for (int i = 0; i < MainEngine.getMazeWidth(); i++) {
            for (int j = 0; j < MainEngine.getMazeHeight(); j++) {
                Cell cell = new Cell(new Coordinate(i, j), cellSize);
                add(cell, i, j);
                mazeAsArray[j][i] = cell;
            }
        }
    }

    public Cell[][] getMazeAsArray() {
        return mazeAsArray;
    }
}
