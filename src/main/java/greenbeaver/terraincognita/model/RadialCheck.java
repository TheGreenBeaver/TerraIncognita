package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;

import java.util.ArrayList;

class RadialCheck {
    private final Coordinate initial;
    private final ArrayList<ArrayList<Coordinate>> sides;

    private DestinationWithDirection priority1;
    private DestinationWithDirection priority2;
    private DestinationWithDirection priority3;

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
    }

    private boolean reachableAndKnown(Coordinate coordinate) {
        return coordinate.fits() && MainEngine.coordinateReachable(coordinate) && !MainEngine.coordinateUnknown(coordinate);
    }

    // checks if the probable Coordinate can be reached from the coordinate that's to the (direction) from it
    private DestinationWithDirection calculateCorner(Direction direction, Coordinate probable) {
        Coordinate check = probable.add(direction);
        if (reachableAndKnown(check)) {
            return new DestinationWithDirection(check, direction.opposite());
        }

        return null;
    }

    private ValueFound addCorners(int level, boolean startOfLine) {
        int[][] corners = {{-level, -level}, {level, -level}, {level, level}, {-level, level}};

        for (int i = 0; i < 4; i++) {
            Coordinate probable = initial.add(corners[i][0], corners[i][1]);
            if (probable.fits()) {
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
                    if (MainEngine.coordinateUnknown(probable)) { // this block sets a low-priority answer if the currently examined corner is reachable from any side
                        DestinationWithDirection frst = calculateCorner(first, probable);
                        if (frst != null) {
                            priority2 = frst;
                            return ValueFound.PRIORITY_2;
                        }

                        DestinationWithDirection scnd = calculateCorner(second, probable);
                        if (scnd != null) {
                            priority2 = scnd;
                            return ValueFound.PRIORITY_2;
                        }

                        DestinationWithDirection thrd = calculateCorner(first.opposite(), probable);
                        if (thrd != null) {
                            priority3 = thrd;
                            return ValueFound.PRIORITY_3;
                        }

                        DestinationWithDirection frth = calculateCorner(second.opposite(), probable);
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
            if (probable.fits()) {
                if (MainEngine.coordinateUnknown(probable)) {
                    priority1 = new DestinationWithDirection(initial, direction);
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

                if (probable.fits()) {
                    if (MainEngine.coordinateUnknown(probable)) {
                        if (reachableAndKnown(from)) {
                            priority1 = new DestinationWithDirection(from, direction);
                        }

                        Direction p = direction.firstPerpendicular(); // probable coordinate might not be reachable going straight radially from the center, but if it has a "bridge" neighbour in the same ring, it would still be better than corner
                        if (reachableAndKnown(probable.add(p))) {
                            priority2 = new DestinationWithDirection(probable.add(p), p.opposite());
                        } else if (reachableAndKnown(probable.add(p.opposite()))) {
                            priority2 = new DestinationWithDirection(probable.add(p.opposite()), p);
                        }

                        if (reachableAndKnown(probable.add(direction))) {
                            priority3 = new DestinationWithDirection(probable.add(direction), direction.opposite());
                        }
                    }

                    current.add(probable);
                }
            }
        }

        addCorners(level, false);
    }

    DestinationWithDirection find() {
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

        int distanceToBottom = MainEngine.getMazeHeight() - initial.getY();
        int distanceToRight = MainEngine.getMazeWidth() - initial.getX();

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
