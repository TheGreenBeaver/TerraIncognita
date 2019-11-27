package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;
import org.jetbrains.annotations.Nullable;

public class Coordinate {

    public enum CoordinateState {
        UNKNOWN,
        KNOWN_UNREACHABLE,
        KNOWN_REACHABLE,
        KNOWN_BAD_PORTAL,
        KNOWN_PORTAL,
        KNOWN_PORTAL_TO_EXIT,
        KNOWN_PORTAL_TO_UNBLIND,
        KNOWN_MAZE_BORDER,
        TEMPORARY_UNKNOWN
    }

    private static CoordinateState[][] coordinateStates;
    private static CoordinateState[][] localCoordinateStates;

    public static void clearLocalCoordinateStates() {
        for (int i = 0; i < MainEngine.getMazeHeight() * 2 + 1; i++) {
            for (int j = 0; j < MainEngine.getMazeWidth() * 2 + 1; j++) {
                localCoordinateStates[i][j] = CoordinateState.UNKNOWN;
            }
        }
    }

    public static void setCoordinateStates(CoordinateState[][] newCoordinateStates) {
        CoordinateState[][] settingNow = MainEngine.isBlindMode() ? localCoordinateStates : coordinateStates;
        for (int i = 0; i < settingNow.length; i++) {
            System.arraycopy(newCoordinateStates[i], 0, settingNow[i], 0, settingNow[0].length);
        }
    }

    public static CoordinateState[][] getCoordinateStates() {
        return MainEngine.isBlindMode() ? localCoordinateStates : coordinateStates;
    }

    public static void setNewField() {
        localCoordinateStates =
                new CoordinateState[2 * MainEngine.getMazeHeight() + 1][2 * MainEngine.getMazeWidth() + 1];
        clearLocalCoordinateStates();

        coordinateStates = new CoordinateState[MainEngine.getMazeHeight()][MainEngine.getMazeWidth()];
        for (int i = 0; i < MainEngine.getMazeHeight(); i++) {
            for (int j = 0; j < MainEngine.getMazeWidth(); j++) {
                coordinateStates[i][j] = MainEngine.getMaze()[i][j].getCellType() == CellType.ENTRANCE
                        ? CoordinateState.KNOWN_REACHABLE
                        : CoordinateState.UNKNOWN;
            }
        }
    }

    private final int x;
    private final int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Coordinate copy() {
        return new Coordinate(x, y);
    }

    public Coordinate add(int toX, int toY) {
        return new Coordinate(x + toX, y + toY);
    }

    public Coordinate add(Direction direction) { return add(direction.getToX(), direction.getToY()); }

    public Coordinate subtract(Coordinate coordinate) {
        return new Coordinate(x - coordinate.x, y - coordinate.y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean fits() {
        return x >= 0 && y >= 0 && x < MainEngine.getMazeWidth() && y < MainEngine.getMazeHeight();
    }

    public boolean fitsLocally() {
        return MainEngine.isBlindMode() && x >= 0 && y >= 0 && x < MainEngine.getMazeWidth() * 2 + 1 && y < MainEngine.getMazeHeight() * 2 + 1;
    }

    public int getRawNumber() {
        return (MainEngine.isBlindMode() ? MainEngine.getMazeWidth() * 2 + 1 : MainEngine.getMazeWidth()) * y + x;
    }

    public static Coordinate getByRawNumber(int rawNumber) {
        int div = MainEngine.isBlindMode() ? MainEngine.getMazeWidth() * 2 + 1 : MainEngine.getMazeWidth();
        int y = rawNumber / div;
        int x = rawNumber % div;

        return new Coordinate(x, y);
    }

    public CoordinateState getCoordinateState() {
        return MainEngine.isBlindMode() ? localCoordinateStates[y][x] : coordinateStates[y][x];
    }

    public void setCoordinateState(CoordinateState coordinateState, @Nullable Direction last) {
        if (MainEngine.isBlindMode()) {
            if (coordinateState == CoordinateState.KNOWN_MAZE_BORDER) {
                assert last != null;
                int multiplier = last.isPositive() ? 1 : -1;
                if (last.getHorizontal()) {
                    for (int i = 0; i < MainEngine.getMazeHeight() * 2 + 1; i++) {
                        localCoordinateStates[i][x] = coordinateState;
                        int otherBorder = x + multiplier * (MainEngine.getMazeWidth() + 1);
                        localCoordinateStates[i][otherBorder] = coordinateState;
                    }
                } else {
                    for (int i = 0; i < MainEngine.getMazeWidth() * 2 + 1; i++) {
                        localCoordinateStates[y][i] = coordinateState;
                        int otherBorder = y + multiplier * (MainEngine.getMazeHeight() + 1);
                        localCoordinateStates[otherBorder][i] = coordinateState;
                    }
                }
            } else {
                localCoordinateStates[y][x] = coordinateState;
            }
        } else {
            coordinateStates[y][x] = coordinateState;
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
