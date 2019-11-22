package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;

public class Coordinate {

    public enum CoordinateState {
        UNKNOWN,
        KNOWN_UNREACHABLE,
        KNOWN_REACHABLE,
        KNOWN_BAD_PORTAL,
        KNOWN_MAZE_BORDER
    }

    private static CoordinateState[][] localCoordinateStates;

    private final int x;
    private final int y;
    private CoordinateState coordinateState;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
        this.coordinateState = MainEngine.getMaze()[y][x].getCellType() == CellType.ENTRANCE
                ? CoordinateState.KNOWN_REACHABLE
                : CoordinateState.UNKNOWN;
    }

    public Coordinate add(int toX, int toY) {
        return new Coordinate(x + toX, y + toY);
    }

    public Coordinate add(Direction direction) { return add(direction.getToX(), direction.getToY()); }

    public Coordinate subtract(Coordinate to) {
        return add(-to.getX(), -to.getY());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean fits() {
        boolean blindMode = MainEngine.isBlindMode();
        return !blindMode && x >= 0 && y >= 0 && x < MainEngine.getMazeWidth() && y < MainEngine.getMazeHeight()
                || blindMode && localCoordinateStates[y][x] != CoordinateState.KNOWN_MAZE_BORDER;
    }

    public int getRawNumber() {
        return MainEngine.getMazeWidth() * y + x;
    }

    public static Coordinate getByRawNumber(int rawNumber) {
        int y = rawNumber / MainEngine.getMazeWidth();
        int x = rawNumber % MainEngine.getMazeWidth();

        return new Coordinate(x, y);
    }

    public CoordinateState getCoordinateState() {
        return coordinateState;
    }

    public void setCoordinateState(CoordinateState coordinateState) {
        this.coordinateState = coordinateState;
    }

    public CoordinateState getLocalCoordinateState() {
        return localCoordinateStates[y][x];
    }

    public void setLocalCoordinateState(CoordinateState coordinateState, Direction last) {
        if (coordinateState == CoordinateState.KNOWN_MAZE_BORDER) {
            int multiplier = last.isPositive() ? 1 : -1;
            if (last.getHorizontal()) {
                for (int i = 0; i < MainEngine.getMazeHeight() * 2 + 2; i++) {
                    localCoordinateStates[i][x] = coordinateState;
                    int otherBorder = x + multiplier * (MainEngine.getMazeWidth() + 1);
                    localCoordinateStates[i][otherBorder] = coordinateState;
                }
            } else {
                for (int i = 0; i < MainEngine.getMazeWidth() * 2 + 2; i++) {
                    localCoordinateStates[y][i] = coordinateState;
                    int otherBorder = y + multiplier * (MainEngine.getMazeHeight() + 1);
                    localCoordinateStates[otherBorder][i] = coordinateState;
                }
            }
        }
        localCoordinateStates[y][x] = coordinateState;
    }

    public static void setNewField() {
        localCoordinateStates =
                new CoordinateState[2 * MainEngine.getMazeHeight() + 2][2 * MainEngine.getMazeWidth() + 2];
        clearLocalCoordinateStates();
    }

    public static void clearLocalCoordinateStates() {
        for (int i = 0; i < MainEngine.getMazeHeight() * 2 + 2; i++) {
            for (int j = 0; j < MainEngine.getMazeWidth() * 2 + 2; j++) {
                localCoordinateStates[i][j] = CoordinateState.UNKNOWN;
            }
        }
    }

    @Override
    public String toString() {
        return "X: " + x + "; Y: " + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }

        Coordinate other = (Coordinate) obj;
        return other.x == x && other.y == y;
    }
}
