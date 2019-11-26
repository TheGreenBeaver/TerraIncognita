package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Cell;
import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;
import greenbeaver.terraincognita.model.cellConstruction.MoveResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;

public class MainEngine {

    private static class LocalsTree {
        private static class Level {
            private Level child;
            private Level parent;
            private boolean[][] previousAdj;
            private Coordinate.CoordinateState[][] previousCoordinateStates;
            private Coordinate coordinateToReturn;
            private Coordinate previousCurrentCellCoordinate;
            private Direction ledToBadPortal;
            private int previousFailCount;
            private Coordinate previousEnteringPortal;
            private boolean normalTransitionWasTried;
            private ArrayList<Pair<Coordinate, Coordinate>> previousAdjacencyLinks;
            private ArrayList<Pair<Coordinate, Coordinate.CoordinateState>> previousPath;

        }

        Level root = null;

        void add(Direction direction) {
            Level level = new Level();
            level.previousCurrentCellCoordinate = currentCell.getCoordinate().copy();
            level.ledToBadPortal = direction;
            level.previousFailCount = failCount;
            Coordinate.CoordinateState[][] coordinateStates = Coordinate.getCoordinateStates();
            if (root == null) {
                level.coordinateToReturn = currentCell.getCoordinate().copy();

                level.previousAdj = new boolean[cellAmount()][cellAmount()];
                for (int i = 0; i < cellAmount(); i++) {
                    System.arraycopy(adjacencyMatrix[i], 0, level.previousAdj[i], 0, cellAmount());
                }

                level.previousCoordinateStates = new Coordinate.CoordinateState[mazeHeight][mazeWidth];
                for (int i = 0; i < mazeHeight; i++) {
                    System.arraycopy(coordinateStates[i], 0, level.previousCoordinateStates[i], 0, mazeWidth);
                }

                root = level;
            } else {
                Level curr = root;
                while (curr.child != null) {
                    curr = curr.child;
                }

                level.coordinateToReturn = localCoordinate.copy();
                level.previousEnteringPortal = lastEnteringPortal.copy();
                level.normalTransitionWasTried = normalTransitionTried;
                level.previousAdjacencyLinks = new ArrayList<>(localAdjacencyLinks);
                level.previousPath = new ArrayList<>(localPath);

                level.previousAdj = new boolean[localCellAmount()][localCellAmount()];
                for (int i = 0; i < localCellAmount(); i++) {
                    System.arraycopy(localAdjacencyMatrix[i], 0, level.previousAdj[i], 0, localCellAmount());
                }

                level.previousCoordinateStates = new Coordinate.CoordinateState[mazeHeight * 2 + 1][mazeWidth * 2 + 1];
                for (int i = 0; i < mazeHeight * 2 + 1; i++) {
                    System.arraycopy(coordinateStates[i], 0, level.previousCoordinateStates[i], 0, mazeWidth * 2 + 1);
                }

                curr.child = level;
                level.parent = curr;
            }
        }

        Level previousState() {
            assert root != null;
            Level curr = root;
            while (curr.child != null) {
                curr = curr.child;
            }
            if (curr.parent != null) {
                curr.parent.child = null;
            } else {
                root = null;
            }
            return curr;
        }
    }

    private static int mazeHeight; // set by setMazeHeight() from MazeEditorController when the maze is created
    private static int mazeWidth; // set by setMazeWidth() from MazeEditorController when the maze is created
    private static boolean treasureCollected; // initially set to false in solve()
    private static Coordinate entrance; // set by setEntrance() from MazeEditorController when one of the cells is made an entrance
    private static Coordinate exit; // initially set to null in solve()
    private static Cell currentCell; // initially set to entrance in solve(); tracks the real position of the Player; the Cell where the Player was at the start of makeMove()
    private static Cell[][] maze; // set by setMaze() from MazeEditorController when the maze is created
    private static boolean[][] adjacencyMatrix; // initially set to a matrix filled by false in solve()
    private static boolean[][] localAdjacencyMatrix;
    private static boolean firstStep; // initially set to true in solve(); shows if the Player is now trying to reach the bottom right corner of the maze to then start scanning it in zigzags
    private static boolean shift; // initially set to false in solve(); shows if the Player should now change their X coordinate in case they are at the bottom or top border
    private static Direction general; // initially set to Direction.UP() in solve(); shows what overall direction the Player is moving now, not paying attention to firstStep or shift
    private static ArrayList<Pair<Coordinate, Boolean>> steps; // initially set to an ArrayList with only entrance in it in solve(); stores all the cells that the Player visited or tried to visit
    private static MoveResult moveResult; // initially set to null in solve()
    private static Direction lastTried; // initially set to null in solve()
    private static int failCount; // initially set to 0 in solve; shows how much times in a row the Player failed to move in a calculated direction
    private static boolean lastCalculatedDirectionFailed; // initially set to false in solve()
    private static Coordinate current; // initially set to null in solve(); shows the coordinate where the Player was at the moment when a new direction is calculated
    private static boolean firstStepEmergencyStopV; // initially set to false in solve(); shows if the Player met an obstacle during the vertical stage of firstStep and thus should now move on to the horizontal stage
    private static boolean firstStepEmergencyStopH; // initially set to false in solve(); shows if the Player met an obstacle during the horizontal stage of firstStep and thus should now end firstStep
    private static Direction moment; // initially set to null in solve(); shows the direction that the Player should follow to reach the nearest unknown cell after bfs
    private static int bordersHit; // initially set to 0 in solve(); serves as a marker of completion ability for linear mazes
    private static Coordinate[] portalTransitions; // set by setPortalTransitions() from MazeEditorController when the maze is created
    private static boolean blindMode; // initially set to false in solve(); shows if Player now knows his exact coordinate
    private static Coordinate localCoordinate; // initially set to null in solve(); used instead of real coordinate during blindMode
    private static boolean[] rowContainsPortal;
    private static boolean yCoordinateDefined;
    private static boolean xCoordinateDefined;
    private static int verticalTravelLength;
    private static int horizontalTravelLength;
    private static ArrayList<Pair<Coordinate, Coordinate.CoordinateState>> localPath;
    private static ArrayList<Pair<Coordinate, Coordinate>> localAdjacencyLinks;
    private static LocalsTree localsTree;
    private static Coordinate lastEnteringPortal;
    private static boolean normalTransitionTried;
    private static Coordinate newLocalCoordinate;
    private static Coordinate oldRealCoordinate;
    private static Cell newRealCell;
    private static Coordinate newRealCoordinate;
    private static boolean[][] treasureCabin;
    private static boolean[][] exitCabin;


    // Getters and setters
    public static Coordinate[] getPortalTransitions() {
        return portalTransitions;
    }

    public static void setPortalTransitions(Coordinate[] portalTransitions) {
        MainEngine.portalTransitions = portalTransitions;
    }

    public static int getMazeHeight() {
        return mazeHeight;
    }

    public static void setMazeHeight(int newMazeHeight) {
        mazeHeight = newMazeHeight;
    }

    public static int getMazeWidth() {
        return mazeWidth;
    }

    public static void setMazeWidth(int newMazeWidth) {
        mazeWidth = newMazeWidth;
    }

    public static Cell[][] getMaze() {
        return maze;
    }

    public static void setMaze(Cell[][] maze) {
        MainEngine.maze = maze;
    }

    public static void setEntrance(Coordinate entrance) {
        MainEngine.entrance = entrance;
    }

    public static ArrayList<Pair<Coordinate, Boolean>> getSteps() {
        return steps;
    }

    public static int cellAmount() {
        return mazeHeight * mazeWidth;
    }

    private static int localCellAmount() {
        return (mazeWidth * 2 + 1) * (mazeHeight * 2 + 1);
    }

    public static boolean isBlindMode() {
        return blindMode;
    }

    public static Coordinate getLocalCoordinate() {
        return localCoordinate;
    }

    private static void linearMaze(boolean horizontal) {
        int entranceCoordinate = horizontal ? entrance.getX() : entrance.getY();
        int maxValue = (horizontal ? mazeWidth : mazeHeight) - 1;
        Direction positive = horizontal ? Direction.RIGHT : Direction.DOWN;

        if (lastCalculatedDirectionFailed) {
            general = general.opposite();
            bordersHit++;
        } else if (firstStep) {
            general = (entranceCoordinate < maxValue - entranceCoordinate) ? positive.opposite() : positive;
            firstStep = false;
        }
        int val = horizontal ? current.getX() : current.getY();
        if (val == 0 || val == maxValue) {
            bordersHit++;
            general = general.opposite();
        }
    }

    private static Direction turnAround(int criticalPoint, boolean vertical) {
        int point = vertical ? current.add(general).getY() : current.add(general).getX();
        if (point == criticalPoint) {
            if (shift) {
                shift = false;
                general = general.opposite();
            } else {
                shift = true;
                lastTried = general.firstPerpendicular();
                return lastTried;
            }
        }

        return null;
    }

    private static Direction calculateDirection() {

        current = blindMode ? localCoordinate : currentCell.getCoordinate(); // coordinate where the player was BEFORE moving!!!

        /*if (mazeHeight == 1) {
            linearMaze(true); // TODO: check this later
        } else if (mazeWidth == 1) {
            linearMaze(false); // TODO: check this later
        } else*/ if (firstStep) { // first of all, Player tries to reach the bottom right corner to then continue scanning the maze in zigzags
            if ((current.getY() < mazeHeight - 1 || blindMode) && !firstStepEmergencyStopV) { // if not at the bottom and haven't met any obstacles on the way down yet
                general = Direction.DOWN;
            } else if (yCoordinateDefined && rowContainsPortal[currentCell.getCoordinate().getY()]) {
                general = Direction.UP;
            } else if ((current.getX() < mazeWidth - 1 || blindMode) && !firstStepEmergencyStopH) { // if not at the right border and haven't met any obstacles on the way right yet
                general = Direction.RIGHT;
            } else {
                firstStep = false;
                general = Direction.UP;
            }
        } else if (lastCalculatedDirectionFailed) { // Player met a wall in a cell they tried to reach using the lastTried direction
            lastCalculatedDirectionFailed = false;
            Direction probable;
            switch (failCount) {
                case 1: {
                    probable = lastTried.firstPerpendicular();
                    Coordinate.CoordinateState probableState = current.add(probable).getCoordinateState();
                    if (probableState == Coordinate.CoordinateState.KNOWN_REACHABLE
                            || probableState == Coordinate.CoordinateState.UNKNOWN) { // don't return the direction if it knowingly leads out of maze borders or to a wall; this way we save some loop cycles
                        return probable;
                    }
                    failCount++; // try another direction immediately
                }
                case 2: {
                    probable = lastTried.firstPerpendicular().opposite();
                    Coordinate.CoordinateState probableState = current.add(probable).getCoordinateState();
                    if (probableState == Coordinate.CoordinateState.KNOWN_REACHABLE
                            || probableState == Coordinate.CoordinateState.UNKNOWN) { // the same
                        return probable;
                    }
                    failCount++;
                }
                case 3: {
                    return lastTried.opposite();
                }
            }
//        } else if (exit != null && treasureCollected) {
//            int temp;
//            if (verticalExitDist != 0) {
//                temp = verticalExitDist;
//                verticalExitDist += (verticalExitDist < 0) ? 1 : -1;
//                general = Direction.getByConstructor(0, -(int) Math.signum(temp));
//            } else {
//                temp = horizontalExitDist;
//                horizontalExitDist += (horizontalExitDist < 0) ? 1 : -1;
//                general = Direction.getByConstructor(-(int) Math.signum(temp), 0);
//            }
        } else if (current.equals(new Coordinate(0, 0))) { // all the shifts lead left, so this coordinate should be handled manually
            lastTried = Direction.DOWN;
            return Direction.DOWN;
        } else {
            Direction turn;
            int[] critical = {-1, mazeWidth, mazeHeight, -1};
            for (int i = 0; i < 4; i++) {
                turn = turnAround(critical[i], !Direction.values()[i].getHorizontal());
                if (turn != null) {
                    lastTried = turn;
                    return turn;
                }
            }
        }

        //Direction toReturn = finalCheck();
        lastTried = general;
        return general;
    }

//    private static Direction finalCheck() {
//        Direction toReturn = general;
//        Coordinate wouldBe = current.add(toReturn);
//        int count = 0;
//        while (!wouldBe.fits() ||
//                coordinateStates[wouldBe.getY()][wouldBe.getX()]
//                        && !maze[wouldBe.getY()][wouldBe.getX()].getCellType().isReachable()) {
//            switch (count++) {
//                case 0: {
//                    toReturn = general.firstPerpendicular();
//                    break;
//                }
//                case 1: {
//                    toReturn = general.firstPerpendicular().opposite();
//                    break;
//                }
//                case 2: {
//                    toReturn = general.opposite();
//                }
//            }
//            wouldBe = current.add(toReturn);
//        }
//        return toReturn;
//    }

    private static boolean successfulMoveScenario(Direction dir) {
        failCount = 0;
        steps.add(new Pair<>(newRealCoordinate, true));

        int from = blindMode ? localCoordinate.getRawNumber() : oldRealCoordinate.getRawNumber(); // where the move started; current was set to a proper value in calculateDirection() that happens before this
        int to = blindMode ? newLocalCoordinate.getRawNumber() : newRealCoordinate.getRawNumber(); // where the Player is now actually

        if (!blindMode) {
            adjacencyMatrix[from][to] = true;
            adjacencyMatrix[to][from] = true;
            currentCell = newRealCell;
        } else {
            localAdjacencyMatrix[from][to] = true;
            localAdjacencyMatrix[to][from] = true;
            localPath.add(new Pair<>(newRealCoordinate, Coordinate.CoordinateState.KNOWN_REACHABLE));
            localAdjacencyLinks.add(new Pair<>(oldRealCoordinate, newRealCoordinate));
            int addition = dir.isPositive() ? -1 : 1;
            if (!yCoordinateDefined && !dir.getHorizontal()) {
                verticalTravelLength += addition;
            } else if (!xCoordinateDefined && dir.getHorizontal()) {
                horizontalTravelLength += addition;
            }
            localCoordinate = newLocalCoordinate;
        }

        System.out.println("Visited " + newRealCoordinate.toString());


        switch (currentCell.getCellType()) {
            case TREASURE: {
                if (blindMode && treasureCollected) {
                    recalculateFromLocals();
                }
                treasureCollected = true;
                if (exit != null) {
//                    Coordinate dist = currentCell.getCoordinate().subtract(exit);
//                    verticalExitDist = dist.getY();
//                    horizontalExitDist = dist.getX();
                    ArrayList<Integer> path = bfs(currentCell.getCoordinate(), exit); // if the Player has already found an exit and than reached the treasure, then there's definitely a path bfs can find
                    for (Integer raw : path) {
                        Coordinate c = Coordinate.getByRawNumber(raw);
                        steps.add(new Pair<>(c, true));
                        System.out.println("Visited " + c.toString());
                    }
                    return true;
                }
                break;
            }

            case EXIT: {
                exit = currentCell.getCoordinate();
                if (treasureCollected) {
                    return true;
                }
                break;
            }
        }

        return false;
    }

    private static void emergency() {
        if (lastTried.getHorizontal()) {
            firstStepEmergencyStopH = true;
        } else {
            firstStepEmergencyStopV = true;
        }
    }

    private static boolean makeMove() {
        Direction dir = moment == null ? calculateDirection() : moment; // if we've just used bfs and now just need to reach the actual nearest unknown cell, we don't need to calculate direction

//        if (bordersHit == 2) {
//            if (!treasureCollected) {
//                System.out.println("Treasure couldn't be reached");
//                return true;
//            }
//            if (exit == null) {
//                System.out.println("Exit couldn't be reached");
//                return true;
//            }
//        }

        moveResult = currentCell.move(dir);
        moment = null;

        // Old local Coordinate is localCoordinate itself
        newLocalCoordinate = localCoordinate.add(dir);

        // Old real Cell is currentCell itself
        oldRealCoordinate = currentCell.getCoordinate();

        newRealCell = moveResult.getResult();
        newRealCoordinate = newRealCell.getCoordinate();

        switch (moveResult) {
            case SUCCESSFUL: { // does count as a step
                return successfulMoveScenario(dir);
            }

            case UNREACHABLE_CELL: { // does count as a step
                Coordinate forCalculation = blindMode ? newLocalCoordinate : newRealCoordinate;
                forCalculation.setCoordinateState(Coordinate.CoordinateState.KNOWN_UNREACHABLE, null);
                steps.add(new Pair<>(newRealCoordinate, false));
                System.out.println("Tried to reach " + newRealCoordinate.toString() + ", but met an obstacle");
                if (!firstStep) {
                    lastCalculatedDirectionFailed = true;
                    failCount++;
                } else {
                    emergency();
                }
                if (failCount >= 4) {
                    System.out.println("Blocked at " + oldRealCoordinate.toString());
                    if (blindMode) {
                        backspace();
                    } else {
                        return true;
                    }
                }
                break;
            }

            case ALREADY_VISITED_CELL: { // does not count as a step (by now, while the portals aren't implemented)

                int from = blindMode ? localCoordinate.getRawNumber() : oldRealCoordinate.getRawNumber();
                int to = blindMode ? newLocalCoordinate.getRawNumber() : newRealCoordinate.getRawNumber();

                if (!blindMode) {
                    adjacencyMatrix[from][to] = true;
                    adjacencyMatrix[to][from] = true;
                } else {
                    localAdjacencyMatrix[from][to] = true;
                    localAdjacencyMatrix[to][from] = true;
                    localAdjacencyLinks.add(new Pair<>(oldRealCoordinate, newRealCoordinate));
                }
                failCount = 0;
                RadialCheck radialCheck = new RadialCheck(currentCell.getCoordinate()); // searches from a coordinate where the Player was before trying to make move
                Pair<Coordinate, Direction> d = radialCheck.find();
                Coordinate relation = oldRealCoordinate.subtract(localCoordinate);
                if (d == null) {
                    if (blindMode) {
                        if (!normalTransitionTried) {
                            ArrayList<Integer> pathToLastEnteringPortal = bfs(localCoordinate, lastEnteringPortal);
                            int s = pathToLastEnteringPortal.size();
                            Coordinate preLast = null;
                            for (int i = 0; i < s; i++) {
                                Coordinate localByRaw = Coordinate.getByRawNumber(pathToLastEnteringPortal.get(i));
                                Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                                if (i == s - 2) {
                                    preLast = realByLocal;
                                }
                                if (i < s - 1) {
                                    steps.add(new Pair<>(realByLocal, true));
                                } else {
                                    assert preLast != null;
                                    Coordinate lastMove = realByLocal.subtract(preLast);
                                    moment = Direction.getByConstructor(lastMove.getX(), lastMove.getY());
                                    break;
                                }
                            }
                            normalTransitionTried = true;
                        } else {
                            backspace();
                        }
                    } else {
                        if (!treasureCollected) {
                            System.out.println("Treasure couldn't be reached");
                        }
                        if (exit == null) {
                            System.out.println("Exit couldn't be reached");
                        }
                        return true;
                    }
                } else {
                    Coordinate nowGoingTo = d.getA();
                    moment = d.getB();

                    lastTried = moment;

                    Coordinate forCalculation = blindMode ? localCoordinate : oldRealCoordinate;
                    if (!nowGoingTo.equals(forCalculation)) {

                        ArrayList<Integer> path = bfs(forCalculation, nowGoingTo);
                        if (blindMode) {
                            Coordinate previous = localCoordinate;
                            for (int num : path) { // FIXME: 25.11.2019 add changes to verticalTravelLength and horizontalTravelLength
                                Coordinate localByRaw = Coordinate.getByRawNumber(num);
                                Coordinate change = localByRaw.subtract(previous);
                                previous = localByRaw;
                                Direction lDir = Direction.getByConstructor(change.getX(), change.getY());
                                int addition = lDir.isPositive() ? -1 : 1;
                                if (lDir.getHorizontal() && !xCoordinateDefined) {
                                    horizontalTravelLength += addition;
                                } else if (!lDir.getHorizontal() && !yCoordinateDefined) {
                                    verticalTravelLength += addition;
                                }
                                Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                                System.out.println("Visited " + realByLocal.toString());
                                steps.add(new Pair<>(realByLocal, true));
                            }
                        } else {
                            for (Integer num: path) {
                                System.out.println("Visited " + Coordinate.getByRawNumber(num));
                                steps.add(new Pair<>(Coordinate.getByRawNumber(num), true));
                            }
                        }
                        currentCell = maze[nowGoingTo.getY()][nowGoingTo.getX()];
                    }
                }

                break;
            }

            case MAZE_BORDER: {
                if (!dir.getHorizontal()) {
                    yCoordinateDefined = true;
                    firstStepEmergencyStopV = true;
                } else {
                    xCoordinateDefined = true;
                    firstStepEmergencyStopH = true;
                }
                localCoordinate.add(dir).setCoordinateState(Coordinate.CoordinateState.KNOWN_MAZE_BORDER, dir);

                if (yCoordinateDefined && xCoordinateDefined) {
                    ArrayList<Pair<Coordinate, Coordinate>> links = new ArrayList<>();
                    for (int i = 0; i < localAdjacencyMatrix.length; i++) {
                        for (int j = 0; j < localAdjacencyMatrix.length; j++) {
                            if (localAdjacencyMatrix[i][j]) {
                                links.add(
                                        new Pair<>(Coordinate.getByRawNumber(i).add(-horizontalTravelLength, -verticalTravelLength),
                                                Coordinate.getByRawNumber(j).add(-horizontalTravelLength, -verticalTravelLength)));
                            }
                        }
                    }
                    blindMode = false;
                    for (Pair<Coordinate, Coordinate> l: links) {
                        int from = l.getA().getRawNumber();
                        int to = l.getB().getRawNumber();
                        adjacencyMatrix[from][to] = true;
                    }
                    for (Pair<Coordinate, Coordinate.CoordinateState> c: localPath) {
                        Coordinate real = c.getA().add(-horizontalTravelLength, -verticalTravelLength);
                        real.setCoordinateState(c.getB(), null);
                    }
                    int dirIndex = 0;
                    boolean rf = false;
                    while (dirIndex < 4) {
                        Direction d = Direction.values()[dirIndex++];
                        Coordinate.CoordinateState s = currentCell.getCoordinate().add(d).getCoordinateState();
                        if (s == Coordinate.CoordinateState.UNKNOWN) {
                            moment = d;
                            break;
                        }
                        if (s == Coordinate.CoordinateState.KNOWN_REACHABLE) {
                            moment = d;
                            rf = true;
                        }
                        if (s == Coordinate.CoordinateState.KNOWN_PORTAL && !rf) {
                            moment = d;
                        }
                    }
                }

                break;
            }

            case PORTAL: {
                System.out.println("Travelled through portal from " + oldRealCoordinate.toString() + " to " + newRealCoordinate.toString());

                if (yCoordinateDefined) {
                    rowContainsPortal[currentCell.getCoordinate().getY()] = true;
                }

                currentCell.getCoordinate().add(dir).setCoordinateState(Coordinate.CoordinateState.KNOWN_PORTAL, null);
                localsTree.add(dir);

                currentCell = newRealCell;
                clearLocals();
            }
        }

        return false;
    }

    private static void recalculateFromLocals() {
        // TODO
    }

    private static void backspace() {
        System.out.println("BACKSPACE");
        LocalsTree.Level previous = localsTree.previousState();
        Coordinate beforePortal = previous.coordinateToReturn.copy();
        Coordinate pCurrent = previous.previousCurrentCellCoordinate.copy();
        currentCell = maze[pCurrent.getY()][pCurrent.getX()];
        failCount = previous.previousFailCount + 1;
        lastCalculatedDirectionFailed = true;
        lastTried = previous.ledToBadPortal;
        if (previous.parent == null) {
            blindMode = false;
            for (int i = 0; i < adjacencyMatrix.length; i++) {
                System.arraycopy(previous.previousAdj[i], 0, adjacencyMatrix[i], 0, adjacencyMatrix.length);
            }
            recalculateFromLocals();
        } else {
            localCoordinate = beforePortal.copy();
            normalTransitionTried = previous.normalTransitionWasTried;
            lastEnteringPortal = previous.previousEnteringPortal.copy();
            for (int i = 0; i < localAdjacencyMatrix.length; i++) {
                System.arraycopy(previous.previousAdj[i], 0, localAdjacencyMatrix[i], 0, localAdjacencyMatrix.length);
            }
            Coordinate.setCoordinateStates(previous.previousCoordinateStates);
            localPath = new ArrayList<>(previous.previousPath);
            localAdjacencyLinks = new ArrayList<>(previous.previousAdjacencyLinks);
        }
        beforePortal.add(lastTried).setCoordinateState(Coordinate.CoordinateState.KNOWN_BAD_PORTAL, null);
    }

    private static void clearLocals() {

        blindMode = true;

        for (int i = 0; i < localCellAmount(); i++) {
            for (int j = 0; j < localCellAmount(); j++) {
                localAdjacencyMatrix[i][j] = false;
            }
        }

        firstStep = true;
        firstStepEmergencyStopH = false;
        firstStepEmergencyStopV = false;

        general = Direction.UP;
        shift = false;

        localCoordinate = new Coordinate(mazeWidth, mazeHeight);
        Coordinate.clearLocalCoordinateStates();
        verticalTravelLength = 0;
        horizontalTravelLength = 0;
        yCoordinateDefined = false;
        xCoordinateDefined = false;
        localPath.clear();
        localAdjacencyLinks.clear();
    }

    private static ArrayList<Integer> bfs(Coordinate startC, Coordinate destC) {
        int start = startC.getRawNumber();
        int dest = destC.getRawNumber();

        int actualSize = blindMode ? localCellAmount() : cellAmount();
        boolean[][] matrixToUse = blindMode ? localAdjacencyMatrix : adjacencyMatrix;
        ArrayList<ArrayList<Integer>> paths = new ArrayList<>(actualSize);
        for (int i = 0; i < actualSize; i++) {
            paths.add(new ArrayList<>());
        }

        boolean[] visited = new boolean[actualSize];
        visited[start] = true;

        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(actualSize);
        queue.add(start);

        while (!queue.isEmpty()) {
            int a = queue.poll();
            for (int i = 0; i < actualSize; i++) {
                if (matrixToUse[a][i] && !visited[i]) {
                    paths.set(i, new ArrayList<>(paths.get(a)));
                    paths.get(i).add(i);
                    if (i == dest) {
                        return paths.get(i);
                    }
                    visited[i] = true;
                    queue.add(i);
                }
            }
        }

        throw new NullPointerException("No path from " + startC.toString() + " to " + destC.toString());
    }

    public static void solve() {
        treasureCollected = false;
        exit = null;
        currentCell = maze[entrance.getY()][entrance.getX()];
        adjacencyMatrix = new boolean[cellAmount()][cellAmount()];
        localAdjacencyMatrix = new boolean[localCellAmount()][localCellAmount()];
        firstStep = true;
        shift = false;
        general = Direction.UP;
        steps = new ArrayList<>(Collections.singletonList(new Pair<>(entrance, true)));
        moveResult = null;
        lastTried = null;
        failCount = 0;
        lastCalculatedDirectionFailed = false;
        current = null;
        firstStepEmergencyStopH = false;
        firstStepEmergencyStopV = false;
        moment = null;
        bordersHit = 0;
        blindMode = false;
        localCoordinate = null;
        rowContainsPortal = new boolean[mazeHeight];
        yCoordinateDefined = true;
        xCoordinateDefined = true;
        Coordinate.setNewField();
        localPath = new ArrayList<>();

        boolean completed = false;

        while (!completed) {
            completed = makeMove();
        }
        System.out.println("COMPLETE");
        System.out.println();
    }
}
