package greenbeaver.terraincognita.model.cellConstruction;

public enum Direction {
    UP(0, -1, false),
    RIGHT(1, 0, true),
    DOWN(0, 1, false),
    LEFT(-1, 0, true);

    private final int toX;
    private final int toY;

    private final boolean horizontal;

    Direction(int toX, int toY, boolean horizontal) {
        this.toX = toX;
        this.toY = toY;
        this.horizontal = horizontal;
    }

    public static Direction getByConstructor(int byX, int byY) {
        for (Direction direction: values()) {
            if (direction.toX == byX && direction.toY == byY) {
                return direction;
            }
        }
        return null;
    }

    public int getToX() {
        return toX;
    }

    public int getToY() {
        return toY;
    }

    public Direction opposite() {
        switch (this) {
            case UP: {
                return DOWN;
            }

            case DOWN: {
                return UP;
            }

            case RIGHT: {
                return LEFT;
            }

            case LEFT: {
                return RIGHT;
            }

            default: throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public Direction firstPerpendicular() {
        if (horizontal) {
            return UP;
        }
        return LEFT;
    }

    public boolean getHorizontal() {
        return horizontal;
    }
}
