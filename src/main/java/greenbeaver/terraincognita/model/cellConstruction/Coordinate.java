package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;

import java.util.ArrayList;

public class Coordinate {
    private final int x;
    private final int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
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
        return x >= 0 && y >= 0 && x < MainEngine.getMazeWidth() && y < MainEngine.getMazeHeight();
    }

    public int getRawNumber() {
        return MainEngine.getMazeWidth() * y + x;
    }

    public static Coordinate getByRawNumber(int rawNumber) {
        int y = rawNumber / MainEngine.getMazeWidth();
        int x = rawNumber % MainEngine.getMazeWidth();

        return new Coordinate(x, y);
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
