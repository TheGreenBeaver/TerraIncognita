package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;

import java.util.ArrayList;

class RadialCheck {
    private ArrayList<ArrayList<Coordinate>> sides;
    private Coordinate initial;
    private DestinationWithDirection temp;

    // checks if the probable Coordinate can be reached from the coordinate that's to the (direction) from it
    private DestinationWithDirection calculateCorner(Direction direction, Coordinate probable) {
        Coordinate check = probable.add(direction);
        if (reachableAndKnown(check)) {
            return new DestinationWithDirection(check, direction.opposite());
        }

        return null;
    }

    // computes the corners of a new ring
    // level is the amount of rows between the current ring and initial coordinate
    // if startOfLine is true, computed corners will be checked for actually being the nearest unknown coordinates
    // if this is true, they are returned, else they are stored in the first element of a new ring's current side ArrayList
    // if startOfLine is false, the corners are just put in the last element of a new ring's (current - 1) side ArrayList
    private DestinationWithDirection addCorners(int level, boolean startOfLine) {
        int[][] corners = {{-level, -level}, {level, -level}, {level, level}, {-level, level}};

        Direction first;
        Direction second; // two possible neighbours for the corner that could be the bridges to it
        for (int i = 0; i < 4; i++) {
            Coordinate probable = initial.add(corners[i][0], corners[i][1]);
            if (probable.fits()) {
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
                if (startOfLine) {
                    if (MainEngine.coordinateUnknown(probable)) {
                        DestinationWithDirection f = calculateCorner(first, probable);
                        if (f != null) {
                            return f;
                        }

                        DestinationWithDirection s = calculateCorner(second, probable);
                        if (s != null) {
                            return s;
                        }
                    }
                    sides.get(i).add(probable);
                } else {
                    int also = (i == 0) ? 3 : (i - 1);
                    sides.get(also).add(probable);
                }
            }
        }

        return null;
    }

    RadialCheck(Coordinate initial) {
        this.initial = initial;
        sides = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            sides.add(new ArrayList<>());
        }
    }

    // fills the "cross" around the initial element, immediately returns if an unknown cell is found
    private DestinationWithDirection initialFill() {
        for (int i = 0; i < 4; i++) {
            Direction direction = Direction.values()[i];
            Coordinate probable = initial.add(direction);
            if (probable.fits()) {
                if (MainEngine.coordinateUnknown(probable)) {
                    return new DestinationWithDirection(initial, direction);
                }

                sides.get(i).add(probable);
            }
        }
        addCorners(1, false);

        return null;
    }

    private boolean reachableAndKnown(Coordinate coordinate) {
        return coordinate.fits() && MainEngine.coordinateReachable(coordinate) && !MainEngine.coordinateUnknown(coordinate);
    }

    private DestinationWithDirection updateLines(int level) {
        for (int i = 0; i < 4; i++) {
            Direction direction = Direction.values()[i]; // Up -> Right -> Down -> Left
            ArrayList<Coordinate> current = sides.get(i);
            ArrayList<Coordinate> currentCopy = new ArrayList<>(current); // save the (level - 1) state of the side
            current.clear();
            temp = addCorners(level, true); // might return corner if there's no good coordinate in the ring itself
            for (Coordinate from : currentCopy) {
                Coordinate probable = from.add(direction);

                if (probable.fits()) {
                    if (MainEngine.coordinateUnknown(probable)) {
                        if (reachableAndKnown(from)) {
                            return new DestinationWithDirection(from, direction);
                        }

                        Direction p = direction.firstPerpendicular(); // probable coordinate might not be reachable going straight radially from the center, but if it has a "bridge" neighbour in the same ring, it would still be better than corner
                        if (reachableAndKnown(probable.add(p))) {
                            temp = new DestinationWithDirection(probable.add(p), p.opposite());
                        } else if (reachableAndKnown(probable.add(p.opposite()))){
                            temp = new DestinationWithDirection(probable.add(p.opposite()), p);
                        }
                    }

                    current.add(probable);
                }
            }
        }

        addCorners(level, false);
        return temp;
    }

    DestinationWithDirection find() {
        DestinationWithDirection firstTry = addCorners(1, true); // elements at the start of side ArrayLists (which represent corners) are filled first so that there's no shifting of n elements

        DestinationWithDirection secondTry = initialFill();
        if (secondTry != null) {
            return secondTry;
        }

        if (firstTry != null) {
            return firstTry;
        }

        int distanceToBottom = MainEngine.getMazeHeight() - initial.getY();
        int distanceToRight = MainEngine.getMazeWidth() - initial.getX();

        int maxHDistance = Math.max(distanceToRight, initial.getX());
        int maxVDistance = Math.max(distanceToBottom, initial.getY());

        int cutoff = Math.max(maxHDistance, maxVDistance);

        int level = 2;
        DestinationWithDirection result = null;
        while (result == null) {
            if (level > cutoff) {
                return null;
            }
            result = updateLines(level++);
        }

        return result;
    }
}
