package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Cell;
import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;
import greenbeaver.terraincognita.model.cellConstruction.MoveResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;

public class MainEngine {

    private static class MarkedCoordinate {
        Coordinate coordinate;
        boolean couldActuallyReach;

        private MarkedCoordinate(Coordinate coordinate, boolean couldActuallyReach) {
            this.coordinate = coordinate;
            this.couldActuallyReach = couldActuallyReach;
        }
    }

    private static int mazeHeight; // set by setMazeHeight() from MazeEditorController when the maze is created
    private static int mazeWidth; // set by setMazeWidth() from MazeEditorController when the maze is created
    private static boolean treasureCollected; // initially set to false in solve()
    private static Coordinate entrance; // set by setEntrance() from MazeEditorController when one of the cells is made an entrance
    private static Coordinate exit; // initially set to null in solve()
    private static Cell currentCell; // initially set to entrance in solve()
    private static Cell[][] maze; // set by setMaze() from MazeEditorController when the maze is created
    private static boolean[][] adjacencyMatrix; // initially set to a matrix filled by false in solve()
    private static boolean firstStep; // initially set to true in solve(); shows if the Player is now trying to reach the bottom right corner of the maze to then start scanning it in zigzags
    private static boolean shift; // initially set to false in solve(); shows if the Player should now change their X coordinate in case they are at the bottom or top border
    private static Direction general; // initially set to Direction.UP() in solve(); shows what overall direction the Player is moving now, not paying attention to firstStep or shift
    private static ArrayList<MarkedCoordinate> steps; // initially set to an ArrayList with only entrance in it in solve(); stores all the cells that the Player visited or tried to visit
    private static int verticalExitDist; // TODO: this will be used when portals are implemented
    private static int horizontalExitDist; // TODO: this will be used when portals are implemented
    private static MoveResult moveResult; // initially set to null in solve()
    private static Direction lastTried; // initially set to null in solve()
    private static int failCount; // initially set to 0 in solve; shows how much times in a row the Player failed to move in a calculated direction
    private static boolean lastCalculatedDirectionFailed; // initially set to false in solve()
    private static Coordinate current; // initially set to null in solve(); shows the coordinate where the Player was at the moment when a new direction is calculated
    private static boolean firstStepEmergencyStopV; // initially set to false in solve(); shows if the Player met an obstacle during the vertical stage of firstStep and thus should now move on to the horizontal stage
    private static boolean firstStepEmergencyStopH; // initially set to false in solve(); shows if the Player met an obstacle during the horizontal stage of firstStep and thus should now end firstStep
    private static Direction moment; // initially set to false in solve(); shows the direction that the Player should follow to reach the nearest unknown cell after bfs
    private static int bordersHit; // initially set to 0 in solve(); serves as a marker of completion ability for linear mazes
    private static Coordinate[] portalTransitions; // set by setPortalTransitions() from MazeEditorController when the maze is created
    private static Coordinate beforePortal; // initially set to null in solve(); Coordinate where the Player was before entering a portal, saved in case portal would be blocked and algorithm would need to return
    private static boolean blindMode; // initially set to false in solve(); shows if Player now knows his exact coordinate

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

    public static ArrayList<MarkedCoordinate> getSteps() {
        return steps;
    }

    public static int cellAmount() {
        return mazeHeight * mazeWidth;
    }

    public static boolean isBlindMode() {
        return blindMode;
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

        current = currentCell.getCoordinate(); // coordinate where the player was BEFORE moving!!!

        if (mazeHeight == 1) {
            linearMaze(true);
        } else if (mazeWidth == 1) {
            linearMaze(false);
        } else if (firstStep) { // first of all, Player tries to reach the bottom right corner to then continue scanning the maze in zigzags
            if (current.getY() < mazeHeight - 1 && !firstStepEmergencyStopV) { // if not at the bottom and haven't met any obstacles on the way down yet
                general = Direction.DOWN;
            } else if (current.getX() < mazeWidth - 1 && !firstStepEmergencyStopH) { // if not at the right border and haven't met any obstacles on the way right yet
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
                    Coordinate.CoordinateState probableState = blindMode
                            ? current.add(probable).getLocalCoordinateState()
                            : current.add(probable).getCoordinateState();
                    if (probableState == Coordinate.CoordinateState.KNOWN_REACHABLE
                            || probableState == Coordinate.CoordinateState.UNKNOWN) { // don't return the direction if it knowingly leads out of maze borders or to a wall; this way we save some loop cycles
                        return probable;
                    }
                    failCount++; // try another direction immediately
                }
                case 2: {
                    probable = lastTried.firstPerpendicular().opposite();
                    Coordinate.CoordinateState probableState = blindMode
                            ? current.add(probable).getLocalCoordinateState()
                            : current.add(probable).getCoordinateState();
                    if (probableState == Coordinate.CoordinateState.KNOWN_REACHABLE
                            || probableState == Coordinate.CoordinateState.UNKNOWN) { // the same
                        return probable;
                    }
                    failCount++;
                }
                case 3: {
                    return lastTried.opposite(); // TODO: 22.11.2019 Finished somewhere over here!!! 
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

        Direction toReturn = finalCheck();
        lastTried = toReturn;
        return toReturn;
    }

    private static Direction finalCheck() {
        Direction toReturn = general;
        Coordinate wouldBe = current.add(toReturn);
        int count = 0;
        while (!wouldBe.fits() ||
                coordinateStates[wouldBe.getY()][wouldBe.getX()]
                        && !maze[wouldBe.getY()][wouldBe.getX()].getCellType().isReachable()) {
            switch (count++) {
                case 0: {
                    toReturn = general.firstPerpendicular();
                    break;
                }
                case 1: {
                    toReturn = general.firstPerpendicular().opposite();
                    break;
                }
                case 2: {
                    toReturn = general.opposite();
                }
            }
            wouldBe = current.add(toReturn);
        }
        return toReturn;
    }

    private static boolean successfulMoveScenario() {
        int from = currentCell.getCoordinate().getRawNumber(); // where the move started; current was set to a proper value in calculateDirection() that happens before this
        currentCell = moveResult.getResult();
        Coordinate tempCurrent = currentCell.getCoordinate();
        int to = tempCurrent.getRawNumber(); // where the Player is now actually
        steps.add(new MarkedCoordinate(tempCurrent, true));
        System.out.println("Visited " + tempCurrent.toString());
        adjacencyMatrix[from][to] = true;
        adjacencyMatrix[to][from] = true;

        switch (currentCell.getCellType()) {
            case TREASURE: {
                treasureCollected = true;
                pickedTreasure = "Picked Treasure at " + currentCell.getCoordinate().toString();
                if (exit != null) {
//                    Coordinate dist = currentCell.getCoordinate().subtract(exit);
//                    verticalExitDist = dist.getY();
//                    horizontalExitDist = dist.getX();
                    ArrayList<Integer> path = bfs(currentCell.getCoordinate(), exit); // if the Player has already found an exit and than reached the treasure, then there's definitely a path bfs can find
                    for (Integer raw : path) {
                        Coordinate c = Coordinate.getByRawNumber(raw);
                        steps.add(new MarkedCoordinate(c, true));
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
        Direction dir = (moment == null) ? calculateDirection() : moment; // if we've just used bfs and now just need to reach the actual nearest unknown cell, we don't need to calculate direction

        if (bordersHit == 2) {
            if (!treasureCollected) {
                System.out.println("Treasure couldn't be reached");
                return true;
            }
            if (exit == null) {
                System.out.println("Exit couldn't be reached");
                return true;
            }
        }

        moveResult = currentCell.move(dir);
        moment = null;

        switch (moveResult) {
            case SUCCESSFUL: { // does count as a step
                failCount = 0;
                return successfulMoveScenario();
            }

            case UNREACHABLE_CELL: { // does count as a step
                Coordinate resC = moveResult.getResult().getCoordinate();
                coordinateStates[resC.getY()][resC.getX()] = true; // coordinate the Player tried to reach still gets known, it's just that it's now known as unreachable
                steps.add(new MarkedCoordinate(resC, false));
                System.out.println("Tried to reach " + resC.toString() + ", but met an obstacle");
                if (!firstStep) {
                    lastCalculatedDirectionFailed = true;
                    failCount++;
                } else {
                    emergency();
                }
                if (failCount == 4) {
                    System.out.println("Blocked at " + current.toString());
                    return true;
                }
                break;
            }

            case ALREADY_VISITED_CELL: { // does not count as a step (by now, while the portals aren't implemented)
                int from = currentCell.getCoordinate().getRawNumber(); // where the move started
                int to = moveResult.getResult().getCoordinate().getRawNumber();
                adjacencyMatrix[from][to] = true;
                adjacencyMatrix[to][from] = true;
                failCount = 0;
                RadialCheck radialCheck = new RadialCheck(currentCell.getCoordinate()); // searches from a coordinate where the Player was before trying to make move
                DestinationWithDirection d = radialCheck.find();
                if (d == null) {
                    if (!treasureCollected) {
                        System.out.println("Treasure couldn't be reached");
                    }
                    if (exit == null) {
                        System.out.println("Exit couldn't be reached");
                    }
                    return true;
                }
                Coordinate nowGoingTo = d.getGoingTo();
                moment = d.getToReachActual();

                lastTried = moment;

                if (!nowGoingTo.equals(currentCell.getCoordinate())) {

                    ArrayList<Integer> path = bfs(current, nowGoingTo);
                    for (Integer num : path) {
                        System.out.println("Visited " + Coordinate.getByRawNumber(num).toString());
                        steps.add(new MarkedCoordinate(Coordinate.getByRawNumber(num), true));
                    }
                    currentCell = maze[nowGoingTo.getY()][nowGoingTo.getX()];
                }

                break;
            }

            case MAZE_BORDER: {
                // TODO: Implement when portals are included so current coordinate might not be computable
            }

            case PORTAL: {
                System.out.println("Travelled through portal from " + currentCell.getCoordinate().toString() + " to " + moveResult.getResult().getCoordinate().toString());
                int from = currentCell.getCoordinate().getRawNumber(); // where the move started
                int to = moveResult.getResult().getCoordinate().getRawNumber();
                adjacencyMatrix[from][to] = true;
                blindMode = true;
                beforePortal = currentCell.getCoordinate();
            }
        }

        return false;
    }

    private static ArrayList<Integer> bfs(Coordinate startC, Coordinate destC) {
        int start = startC.getRawNumber();
        int dest = destC.getRawNumber();

        ArrayList<ArrayList<Integer>> paths = new ArrayList<>(cellAmount());
        for (int i = 0; i < cellAmount(); i++) {
            paths.add(new ArrayList<>());
        }

        boolean[] visited = new boolean[cellAmount()];
        visited[start] = true;

        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(cellAmount());
        queue.add(start);

        while (!queue.isEmpty()) {
            int a = queue.poll();
            for (int i = 0; i < cellAmount(); i++) {
                if (adjacencyMatrix[a][i] && !visited[i]) {
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
        firstStep = true;
        shift = false;
        general = Direction.UP;
        steps = new ArrayList<>(Collections.singletonList(new MarkedCoordinate(entrance, true)));
        moveResult = null;
        lastTried = null;
        failCount = 0;
        lastCalculatedDirectionFailed = false;
        firstStepEmergencyStopH = false;
        firstStepEmergencyStopV = false;
        moment = null;
        bordersHit = 0;
        beforePortal = null;
        blindMode = false;
        Coordinate.setNewField();

        boolean completed = false;

        while (!completed) {
            completed = makeMove();
        }
        System.out.println("COMPLETE");
        System.out.println();
    }
}
