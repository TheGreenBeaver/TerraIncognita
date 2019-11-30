package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;

import java.util.ArrayList;
import java.util.Arrays;

class RadialCheck {
    private final Coordinate initial;
    private final ArrayList<ArrayList<Coordinate>> sides;

    private final boolean[][] adjacency;
    private final boolean[] visited;
    private boolean marker;

    private Pair<Coordinate, Direction> priority1;
    private Pair<Coordinate, Direction> priority2;
    private Pair<Coordinate, Direction> priority3;

    private enum ValueFound {
        PRIORITY_1,
        PRIORITY_2,
        PRIORITY_3,
        NONE
    }

    RadialCheck(Coordinate initial) {
        this.initial = initial;
        sides = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            sides.add(new ArrayList<>());
        }
        priority1 = null;
        priority2 = null;
        priority3 = null;

        adjacency = MainEngine.getCurrentAdjacency();
        visited = new boolean[adjacency.length];
    }

    private boolean dfs(Coordinate start, Coordinate searching) {
        Arrays.fill(visited, false);
        marker = false;

        dfs(start.getRawNumber(), searching.getRawNumber());

        return marker;
    }

    private void dfs(int start, int searching) {

        if (marker) {
            return;
        }

        visited[start] = true;

        if (start == searching) {
            marker = true;
            return;
        }

        for (int i = 0; i < adjacency.length; i++) {
            if (adjacency[start][i] && !visited[i]) {
                dfs(i, searching);
            }
        }
    }

    // checks if the probable Coordinate can be reached from the coordinate that's to the (direction) from it
    private Pair<Coordinate, Direction> calculateCorner(Direction direction, Coordinate probable) {
        Coordinate check = probable.add(direction);
        if ((check.fitsLocally() || check.fits()) && check.getCoordinateState() == Coordinate.CoordinateState.KNOWN_REACHABLE) {
            if(dfs(initial, check)) {
                return new Pair<>(check, direction.opposite());
            }
        }

        return null;
    }

    private ValueFound addCorners(int level, boolean startOfLine) {
        int[][] corners = {{-level, -level}, {level, -level}, {level, level}, {-level, level}};

        for (int i = 0; i < 4; i++) {
            Coordinate probable = initial.add(corners[i][0], corners[i][1]);
            if (probable.fitsLocally() || probable.fits()) {
                Direction second;
                Direction first;
                switch (i) {
                    case 0: {
                        first = Direction.RIGHT;
                        second = Direction.DOWN;
                        break;
                    }

                    case 1: {
                        first = Direction.DOWN;
                        second = Direction.LEFT;
                        break;
                    }

                    case 2: {
                        first = Direction.LEFT;
                        second = Direction.UP;
                        break;
                    }
                    case 3: {
                        first = Direction.UP;
                        second = Direction.RIGHT;
                        break;
                    }
                    default:
                        throw new IllegalStateException("Unexpected value: " + i);
                }

                if (startOfLine) { // if true, this corner is added to the actual current side and checked for being an answer
                    if (probable.getCoordinateState() == Coordinate.CoordinateState.UNKNOWN) { // this block sets a low-priority answer if the currently examined corner is reachable from any side
                        Pair<Coordinate, Direction> frst = calculateCorner(first, probable);
                        if (frst != null) {
                            priority2 = frst;
                            return ValueFound.PRIORITY_2;
                        }

                        Pair<Coordinate, Direction> scnd = calculateCorner(second, probable);
                        if (scnd != null) {
                            priority2 = scnd;
                            return ValueFound.PRIORITY_2;
                        }

                        Pair<Coordinate, Direction> thrd = calculateCorner(first.opposite(), probable);
                        if (thrd != null) {
                            priority3 = thrd;
                            return ValueFound.PRIORITY_3;
                        }

                        Pair<Coordinate, Direction> frth = calculateCorner(second.opposite(), probable);
                        if (frth != null) {
                            priority3 = frth;
                            return ValueFound.PRIORITY_3;
                        }
                    }

                    sides.get(i).add(probable);
                } else { // This is a second time we enter this for the same corner and thus only need to add it to its other neighbouring side. By this time we are already sure that this corner isn't an answer
                    int also = (i == 0) ? 3 : (i - 1);
                    sides.get(also).add(probable);
                }
            }
        }
        return ValueFound.NONE;
    }

    // fills the "cross" around the initial element, immediately returns if an unknown cell is found
    private ValueFound initialFill() {
        for (int i = 0; i < 4; i++) {
            Direction direction = Direction.values()[i];
            Coordinate probable = initial.add(direction);
            if (probable.fitsLocally() || probable.fits()) {
                if (probable.getCoordinateState() == Coordinate.CoordinateState.UNKNOWN) {
                    priority1 = new Pair<>(initial, direction);
                    return ValueFound.PRIORITY_1;
                }

                sides.get(i).add(probable);
            }
        }
        addCorners(1, false);

        return ValueFound.NONE;
    }

    private void updateLines(int level) {
        for (int i = 0; i < 4; i++) {
            Direction direction = Direction.values()[i]; // Up -> Right -> Down -> Left
            ArrayList<Coordinate> current = sides.get(i);
            ArrayList<Coordinate> currentCopy = new ArrayList<>(current); // save the (level - 1) state of the side
            current.clear();
            addCorners(level, true);
            for (Coordinate from : currentCopy) {
                Coordinate probable = from.add(direction);

                if (probable.fitsLocally() || probable.fits()) {
                    if (probable.getCoordinateState() == Coordinate.CoordinateState.UNKNOWN) {

                        if (from.getCoordinateState() == Coordinate.CoordinateState.KNOWN_REACHABLE && dfs(initial, from)) {
                            priority1 = new Pair<>(from, direction);
                            return;
                        }

                        Direction p = direction.firstPerpendicular(); // probable coordinate might not be reachable going straight radially from the center, but if it has a "bridge" neighbour in the same ring, it would still be better than corner
                        if (suitable(probable.add(p))) {
                            priority2 = new Pair<>(probable.add(p), p.opposite());
                        } else if (suitable(probable.add(p.opposite()))) {
                            priority2 = new Pair<>(probable.add(p.opposite()), p);
                        } else if (suitable(probable.add(direction))) {
                            priority3 = new Pair<>(probable.add(direction), direction.opposite());
                        }
                    }

                    current.add(probable);
                }
            }
        }

        addCorners(level, false);
    }

    private boolean suitable(Coordinate toCheck) {
        return (toCheck.fitsLocally() || toCheck.fits()) &&
                toCheck.getCoordinateState() == Coordinate.CoordinateState.KNOWN_REACHABLE && dfs(initial, toCheck);
    }

    Pair<Coordinate, Direction> find() {
        ValueFound firstTry = addCorners(1, true);

        ValueFound secondTry = initialFill();

        if (secondTry == ValueFound.PRIORITY_1) {
            return priority1;
        }

        switch (firstTry) {
            case PRIORITY_2:
                return priority2;
            case PRIORITY_3:
                return priority3;
        }

        int h = MainEngine.isBlindMode() ? MainEngine.getMazeHeight() * 2 + 1 : MainEngine.getMazeHeight();
        int w = MainEngine.isBlindMode() ? MainEngine.getMazeWidth() * 2 + 1 : MainEngine.getMazeWidth();

        int distanceToBottom = h - initial.getY();
        int distanceToRight = w - initial.getX();

        int maxHDistance = Math.max(distanceToRight, initial.getX());
        int maxVDistance = Math.max(distanceToBottom, initial.getY());

        int cutoff = Math.max(maxHDistance, maxVDistance);

        int level = 2;
        while (priority1 == null && priority2 == null && priority3 == null) {
            if (level > cutoff) {
                return null;
            }
            updateLines(level++);
        }

        if (priority1 != null) {
            return priority1;
        }

        return priority2 != null ? priority2 : priority3;
    }
}
