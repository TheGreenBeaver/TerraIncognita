package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Cell;
import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;
import greenbeaver.terraincognita.model.cellConstruction.MoveResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;

public class MainEngine {

    private static class LocalsTree {
        private static class Level {
            private Level parent; // previous Level, root represents initial (non-blind) state
            private ArrayList<Level> parents; // ONLY USED FOR ROOT BECAUSE IT MIGHT BE REACHABLE FROM SEVERAL LEVELS
            private Coordinate portalFromParent; // local Coordinate (portal) where the Player was placed after travelling through portal from parent
            private Coordinate gPortalFromParent; // global analogue

            private HashMap<Level, Coordinate> children; // Next Levels with Coordinates of portals leading to them

            private boolean[][] lAdjacencyMatrix;
            private Coordinate.CoordinateState[][] lCoordinateStates;

            // Used when returning from a blocked position
            private Direction last; // Direction that led to the portal that put the Player into this level
            // These duplicate their analogues from MainEngine class itself
            private int lFailCount;
            private boolean lYCoordinateDefined;
            private boolean lXCoordinateDefined;
            private ArrayList<Pair<Coordinate, Coordinate.CoordinateState>> lLocalPath;
            private ArrayList<Pair<Coordinate, Coordinate>> lLocalAdjacencyLinks;

            private boolean pathToExit; // true if this level must be passed to reach exit; used when returning to previous levels to then find this place again

            private Level(Level parent, Coordinate portalFromParent, Coordinate gPortalFromParent) {
                this.parent = parent;
                this.portalFromParent = portalFromParent == null ? null : portalFromParent.copy();
                this.gPortalFromParent = gPortalFromParent == null ? null : gPortalFromParent.copy();

                int adjSize = parent != null ? localCellAmount() : cellAmount(); // if parent is null, than currentLevel is root and we are at the initial non-blind Level
                lAdjacencyMatrix = new boolean[adjSize][adjSize];

                int cStatesH = parent != null ? mazeHeight * 2 + 1 : mazeHeight;
                int cStatesW = parent != null ? mazeWidth * 2 + 1 : mazeWidth;
                lCoordinateStates = new Coordinate.CoordinateState[cStatesH][cStatesW];

                children = new HashMap<>();
            }

            // When ALREADY_VISITED_CELL is hit and radialCheck returns null, the first attempt (if in blind mode) is to try escaping through the portal that was an entrance for this level
            // This represents if such opportunity has already been used
            private boolean triedEscapingThroughEntrance() {
                return children.containsValue(portalFromParent);
            }
        }

        private Level root;

        private LocalsTree() {
            root = new Level(null, null, null);
            root.parents = new ArrayList<>();
        }

        // Used when falling into a portal. Creates new Level and saves the properties of the current one in case there'll be a need to return
        void add(Direction direction) {
            Coordinate p = currentLevel.equals(root) ? oldRealCoordinate.copy() : localCoordinate.copy();
            p = p.add(direction); // Coordinate of portal at the current Level

            saveCurrentState();

            blindMode = true;

            Level level = new Level(currentLevel, localCoordinate, newRealCoordinate);
            currentLevel.children.put(level, p);
            level.last = direction;
            currentLevel = level; // switch to new level
        }

        private void saveCurrentState() {
            Coordinate.CoordinateState[][] c = Coordinate.getCoordinateStates();
            for (int i = 0; i < c.length; i++) {
                System.arraycopy(c[i], 0, currentLevel.lCoordinateStates[i], 0, c[0].length);
            }

            boolean[][] adjToWork = currentLevel.equals(root) ? adjacencyMatrix : localAdjacencyMatrix;
            for (int i = 0; i < adjToWork.length; i++) {
                System.arraycopy(adjToWork[i], 0, currentLevel.lAdjacencyMatrix[i], 0, adjToWork[0].length);
            }

            currentLevel.lFailCount = failCount;
            currentLevel.lYCoordinateDefined = yCoordinateDefined;
            currentLevel.lXCoordinateDefined = xCoordinateDefined;
            if (!currentLevel.equals(root)) {
                currentLevel.lLocalPath = new ArrayList<>(localPath);
                currentLevel.lLocalAdjacencyLinks = new ArrayList<>(localAdjacencyLinks);
            }
        }

        // Used only if blocked
        void previousState() {
            Coordinate p = currentLevel.parent.children.get(currentLevel); // Coordinate of portal that transitioned the Player to the Level we're now leaving

            boolean withDelete = !currentLevel.pathToExit;
            // if the Level the Player's just explored is a part of the portal path to exit and they're just returning back to then visit this Level at the very end, we don't need to delete this level

            if (!withDelete) {
                saveCurrentState();
            } else {
                currentLevel.parent.children.remove(currentLevel);
            }

            lastTried = currentLevel.last;

            currentLevel = currentLevel.parent;
            // HIGHER LEVEL FROM NOW ON!!!
            currentLevel.pathToExit = !withDelete;

            lastCalculatedDirectionFailed = true;
            failCount = currentLevel.lFailCount + 1;
            // these two guarantee that when returned to the previous Level, the Player will search for a new Direction to move

            yCoordinateDefined = currentLevel.lYCoordinateDefined;
            xCoordinateDefined = currentLevel.lXCoordinateDefined;
            Coordinate newCurr = p.add(lastTried.opposite());
            if (!currentLevel.equals(root)) { // non-blind level just does not have such attributes
                localPath = new ArrayList<>(currentLevel.lLocalPath);
                localAdjacencyLinks = new ArrayList<>(currentLevel.lLocalAdjacencyLinks);
                localCoordinate = newCurr;
            } else {
                blindMode = false; // changing blindMode here...
            }

            // ... so that here, if the Player's returned to non-blind state, we would already change global Coordinate States
            Coordinate.setCoordinateStates(currentLevel.lCoordinateStates);
            boolean[][] adjToWork = currentLevel.equals(root) ? adjacencyMatrix : localAdjacencyMatrix;
            for (int i = 0; i < adjToWork.length; i++) {
                System.arraycopy(currentLevel.lAdjacencyMatrix[i], 0, adjToWork[i], 0, adjToWork.length);
            }

            // here, CoordinateState is also set already according to the current state of blindMode
            p.setCoordinateState(withDelete
                            ? Coordinate.CoordinateState.KNOWN_BAD_PORTAL
                            : Coordinate.CoordinateState.KNOWN_PORTAL_TO_EXIT,
                    null);
            if (!currentLevel.equals(root)) {
                Coordinate relation = currentLevel.gPortalFromParent.subtract(currentLevel.portalFromParent);
                newCurr = newCurr.add(relation.getX(), relation.getY());
            }
            currentCell = maze[newCurr.getY()][newCurr.getX()];
        }

        void unblind() {
            Level lParent = currentLevel.parent;
            blindMode = false;

            Coordinate enteringInParent = lParent.children.get(currentLevel);
            lParent.children.remove(currentLevel);

            if (!lParent.equals(root)) { // if the Player didn't manage to unblind after the very first Level
                lParent.children.put(root, enteringInParent);
                root.parents.add(lParent);
            } else { // otherwise the two only levels are just combined into one big non-blind root
                int from = enteringInParent.add(currentLevel.last.opposite()).getRawNumber();
                int to = currentLevel.gPortalFromParent.getRawNumber();
                adjacencyMatrix[from][to] = true;
            }

            for (Pair<Coordinate, Coordinate.CoordinateState> cAndState : localPath) {
                cAndState.getA().setCoordinateState(cAndState.getB(), null);
            }
            for (Pair<Coordinate, Coordinate> cAndC : localAdjacencyLinks) {
                int from = cAndC.getA().getRawNumber();
                int to = cAndC.getB().getRawNumber();
                adjacencyMatrix[from][to] = true;
                adjacencyMatrix[to][from] = true;
            }

            Coordinate relation = currentLevel.gPortalFromParent.subtract(currentLevel.portalFromParent);
            Coordinate newCurr = localCoordinate.add(relation.getX(), relation.getY());
            currentCell = maze[newCurr.getY()][newCurr.getX()];

            currentLevel = root;
        }
    }

    private static int mazeHeight; // set by setMazeHeight() from MazeEditorController when the maze is created
    private static int mazeWidth; // set by setMazeWidth() from MazeEditorController when the maze is created
    private static Coordinate entrance; // set by setEntrance() from MazeEditorController when one of the cells is made an entrance
    private static Pair<Coordinate, LocalsTree.Level> exit; // initially set to null in solve()
    private static Pair<Coordinate, LocalsTree.Level> treasure; // initially set to false in solve()
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
    private static ArrayList<Pair<Coordinate, Coordinate.CoordinateState>> localPath;
    private static ArrayList<Pair<Coordinate, Coordinate>> localAdjacencyLinks;
    private static LocalsTree localsTree;
    private static Coordinate newLocalCoordinate;
    private static Coordinate oldRealCoordinate;
    private static Cell newRealCell;
    private static Coordinate newRealCoordinate;
    private static LocalsTree.Level currentLevel;
    private static boolean mBorder;

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

    static boolean[][] getCurrentAdjacency() {
        return blindMode ? localAdjacencyMatrix : adjacencyMatrix;
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
        } else*/
        if (firstStep) { // first of all, Player tries to reach the bottom right corner to then continue scanning the maze in zigzags
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
                    if (handleDirectionFailure(probable)) return probable;
                }
                case 2: {
                    probable = lastTried.firstPerpendicular().opposite();
                    if (handleDirectionFailure(probable)) return probable;
                }
                case 3: {
                    return lastTried.opposite();
                }
            }
        } else if (current.equals(new Coordinate(0, 0))) { // all the shifts lead left, so this coordinate should be handled manually
            lastTried = Direction.DOWN;
            return Direction.DOWN;
        } else {
            if (blindMode) {
                if (mBorder) {
                    if (!shift) {
                        shift = true;
                        lastTried = general.firstPerpendicular();
                        return lastTried;
                    } else {
                        shift = false;
                        general = general.opposite();
                    }
                }
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
        }

        //Direction toReturn = finalCheck();
        lastTried = general;
        return general;
    }

    private static boolean handleDirectionFailure(Direction probable) {
        if (blindMode || current.add(probable).fits()) {
            Coordinate.CoordinateState probableState = current.add(probable).getCoordinateState();
            if (probableState == Coordinate.CoordinateState.KNOWN_REACHABLE
                    || probableState == Coordinate.CoordinateState.UNKNOWN) { // the same
                lastTried = probable;
                return true;
            }
        }
        failCount++;
        return false;
    }

//    private static Direction finalCheck() { TODO: turns out this is actually needed if !blindMode
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

    private static boolean successfulMoveScenario() {
        failCount = 0;
        steps.add(new Pair<>(newRealCoordinate, true));

        System.out.println("Visited " + newRealCoordinate.toString());

        neighbours();

        int from = blindMode ? localCoordinate.getRawNumber() : oldRealCoordinate.getRawNumber();
        int to = blindMode ? newLocalCoordinate.getRawNumber() : newRealCoordinate.getRawNumber();

        currentCell = newRealCell;

        if (!blindMode) {
            adjacencyMatrix[from][to] = true;
            adjacencyMatrix[to][from] = true;
            newRealCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_REACHABLE, null);
        } else {
            localAdjacencyMatrix[from][to] = true;
            localAdjacencyMatrix[to][from] = true;
            localAdjacencyLinks.add(new Pair<>(oldRealCoordinate.copy(), newRealCoordinate.copy()));
            localPath.add(new Pair<>(newRealCoordinate, Coordinate.CoordinateState.KNOWN_REACHABLE));
            localCoordinate = newLocalCoordinate;
            newLocalCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_REACHABLE, null);
        }

        switch (currentCell.getCellType()) {
            case TREASURE: {
                if (blindMode && treasure != null && treasure.getB().equals(localsTree.root)) { // if the treasure's been found while in non-blind mode, than it can be used as marker to unblind
                    localsTree.unblind();
                }
                if (treasure == null) {
                    treasure = new Pair<>(blindMode ? newLocalCoordinate : newRealCoordinate, currentLevel);
                }
                if (exit != null) {
                    if (exit.getB().equals(currentLevel)) {
                        ArrayList<Integer> path = bfs(blindMode ? newLocalCoordinate : newRealCoordinate, exit.getA());
                        manageBFSPath(path, newLocalCoordinate);

                        return true;
                    }

                    LocalsTree.Level temp = exit.getB();
                    Stack<LocalsTree.Level> toExit = new Stack<>();
                    findPathToExit(temp, currentLevel, toExit, false);
                    if (!toExit.isEmpty()) {
                        while (!toExit.isEmpty()) {
                            LocalsTree.Level next = toExit.pop();
                            Coordinate nextC = toExit.isEmpty() ? exit.getA() : currentLevel.children.get(next);
                            ArrayList<Integer> path = bfs(blindMode ? newLocalCoordinate : newRealCoordinate, nextC);
                            manageBFSPath(path, newLocalCoordinate);
                        }
                        return true;
                    }
                }
                break;
            }

            case ENTRANCE: {
                localsTree.unblind();
                break;
            }

            case EXIT: {

                if (exit != null && blindMode && exit.getB().equals(localsTree.root)) {
                    localsTree.unblind();
                }

                if (exit == null) {
                    exit = new Pair<>(blindMode ? newLocalCoordinate : newRealCoordinate, currentLevel);
                }

                if (treasure != null) {
                    return true;
                }

                currentLevel.pathToExit = true;

                break;
            }
        }

        return false;
    }

    private static void manageBFSPath(ArrayList<Integer> path, Coordinate l) {
        if (blindMode) {
            Coordinate relation = newRealCoordinate.subtract(l);
            for (Integer raw : path) {
                Coordinate localByRaw = Coordinate.getByRawNumber(raw);
                Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                steps.add(new Pair<>(realByLocal, true));
                System.out.println("Visited " + realByLocal.toString());
            }
        } else {
            for (Integer raw : path) {
                Coordinate c = Coordinate.getByRawNumber(raw);
                steps.add(new Pair<>(c, true));
                System.out.println("Visited " + c.toString());
            }
        }
    }

    private static void findPathToExit(LocalsTree.Level current,
                                       LocalsTree.Level searching,
                                       Stack<LocalsTree.Level> stack,
                                       boolean alreadyPassedRoot) {
        if (current.parent == null && current.parents == null || current.equals(searching)) {
            return;
        }

        stack.push(current);

        if (current.parent != null) {
            findPathToExit(current.parent, searching, stack, alreadyPassedRoot);
        } else if (current.parents != null && !alreadyPassedRoot) {
            for (LocalsTree.Level l : current.parents) {
                findPathToExit(l, searching, stack, true);
            }
        }

        stack.pop();
    }

    private static void emergency() {
        if (lastTried.getHorizontal()) {
            firstStepEmergencyStopH = true;
        } else {
            firstStepEmergencyStopV = true;
        }
    }

    private static void neighbours() {
        Coordinate center = blindMode ? localCoordinate : oldRealCoordinate;
        int cRaw = center.getRawNumber();

        Coordinate relation = oldRealCoordinate.subtract(localCoordinate);

        boolean[][] adj = blindMode ? localAdjacencyMatrix : adjacencyMatrix;

        for (Direction direction : Direction.values()) {
            Coordinate neighbour = center.add(direction);
            int nRaw = neighbour.getRawNumber();
            if ((neighbour.fitsLocally() || neighbour.fits())
                    && neighbour.getCoordinateState() == Coordinate.CoordinateState.KNOWN_REACHABLE) {
                adj[nRaw][cRaw] = true;
                adj[cRaw][nRaw] = true;
                if (blindMode) {
                    Coordinate rNeighbour = neighbour.add(relation.getX(), relation.getY());
                    localAdjacencyLinks.add(new Pair<>(oldRealCoordinate.copy(), rNeighbour));
                }
            }
        }
    }

    private static boolean makeMove() {
        Direction dir = moment == null ? calculateDirection() : moment; // if we've just used some method that gives us the proper Direction for this moment, we don't need to calculate it

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
                return successfulMoveScenario();
            }

            case UNREACHABLE_CELL: { // does count as a step
                Coordinate forCalculation = blindMode ? newLocalCoordinate : newRealCoordinate;
                forCalculation.setCoordinateState(Coordinate.CoordinateState.KNOWN_UNREACHABLE, null);
                steps.add(new Pair<>(newRealCoordinate.copy(), false)); // in steps, we always store real Coordinates to then show them to the User
                if (blindMode) {
                    localPath.add(new Pair<>(newRealCoordinate.copy(), Coordinate.CoordinateState.KNOWN_UNREACHABLE));
                }
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
                        localsTree.previousState();
                    } else {
                        return true;
                    }
                }
                break;
            }

            case ALREADY_VISITED_CELL: { // does not count as a step

                neighbours();

                failCount = 0;
                RadialCheck radialCheck = new RadialCheck(blindMode ? localCoordinate : oldRealCoordinate); // searches from a coordinate where the Player was before trying to make move
                Pair<Coordinate, Direction> d = radialCheck.find();
                if (d == null) { // if there are no more unknown cells possible to visit
                    if (blindMode) {
                        Coordinate relation = oldRealCoordinate.subtract(localCoordinate);
                        if (!currentLevel.triedEscapingThroughEntrance()) { // if in blindMode, the Player should first try escaping according to rules through the same portal they've got to the currentLevel
                            ArrayList<Integer> pathToLastEnteringPortal = bfs(localCoordinate, currentLevel.portalFromParent);
                            int s = pathToLastEnteringPortal.size();
                            // last move in the calculated path must be made through makeMove
                            if (s == 1) { // if the Player only needs to make one step to reach the portal
                                Coordinate move = currentLevel.portalFromParent.subtract(localCoordinate);
                                moment = Direction.getByConstructor(move.getX(), move.getY());
                            } else {
                                Coordinate preLast = null;
                                for (int i = 0; i < s; i++) {
                                    Coordinate localByRaw = Coordinate.getByRawNumber(pathToLastEnteringPortal.get(i));
                                    Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                                    if (i == s - 2) {
                                        preLast = realByLocal;
                                        currentCell = maze[realByLocal.getY()][realByLocal.getX()];
                                        localCoordinate = localByRaw;
                                    }
                                    if (i < s - 1) {
                                        steps.add(new Pair<>(realByLocal, true));
                                        System.out.println("Visited " + realByLocal.toString());
                                    } else {
                                        assert preLast != null;
                                        Coordinate lastMove = realByLocal.subtract(preLast);
                                        moment = Direction.getByConstructor(lastMove.getX(), lastMove.getY());
                                    }
                                }
                            }
                            // changing back to UNKNOWN so that moveResult.PORTAL triggers
                            currentLevel.portalFromParent.setCoordinateState(Coordinate.CoordinateState.UNKNOWN, null);
                        } else {
                            if (treasure != null && treasure.getB().equals(currentLevel)) { // if it turns out that the treasure is only reachable through this Level and this Level can't be left normally, than the game is unwinnable
                                System.out.println("Treasure couldn't be reached");
                                return true;
                            }
                            localsTree.previousState();
                        }
                    } else {
                        if (treasure == null) {
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
                            Coordinate relation = oldRealCoordinate.subtract(localCoordinate);
                            for (int num : path) {
                                Coordinate localByRaw = Coordinate.getByRawNumber(num);
                                Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                                System.out.println("Visited " + realByLocal.toString());
                                steps.add(new Pair<>(realByLocal, true));
                            }
                            localCoordinate = nowGoingTo;
                            Coordinate curr = nowGoingTo.add(relation.getX(), relation.getY());
                            currentCell = maze[curr.getY()][curr.getX()];
                        } else {
                            for (Integer num : path) {
                                System.out.println("Visited " + Coordinate.getByRawNumber(num));
                                steps.add(new Pair<>(Coordinate.getByRawNumber(num), true));
                            }
                            currentCell = maze[nowGoingTo.getY()][nowGoingTo.getX()];
                        }
                    }
                }

                break;
            }

            // can only happen in blindMode
            case MAZE_BORDER: {
                Coordinate overTheBorder = oldRealCoordinate.add(dir);
                System.out.println("Tried to reach " + overTheBorder.toString() + ", but met a maze border");
                steps.add(new Pair<>(overTheBorder, false));
                if (!dir.getHorizontal()) {
                    yCoordinateDefined = true;
                    firstStepEmergencyStopV = true;
                } else {
                    xCoordinateDefined = true;
                    firstStepEmergencyStopH = true;
                }
                mBorder = true;
                steps.add(new Pair<>(overTheBorder, false));
                newLocalCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_MAZE_BORDER, dir);

                if (yCoordinateDefined && xCoordinateDefined) {
                    localsTree.unblind();
                }

                break;
            }

            case PORTAL: {
                System.out.println("Travelled through portal from " + oldRealCoordinate.toString() + " to " + newRealCoordinate.toString());

                steps.add(new Pair<>(oldRealCoordinate, true));
                steps.add(new Pair<>(oldRealCoordinate.add(dir), true));
                steps.add(new Pair<>(newRealCoordinate, true));

                if (yCoordinateDefined) {
                    rowContainsPortal[oldRealCoordinate.getY()] = true;
                }

                localCoordinate = new Coordinate(mazeWidth, mazeHeight);
                oldRealCoordinate.add(dir).setCoordinateState(Coordinate.CoordinateState.KNOWN_PORTAL, null);
                localsTree.add(dir);

                currentCell = newRealCell;
                clearLocals();
                localCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_PORTAL, null);
            }
        }

        return false;
    }

    private static void clearLocals() {

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
        Coordinate.clearLocalCoordinateStates();
        yCoordinateDefined = false;
        xCoordinateDefined = false;
        localPath.clear();
        localAdjacencyLinks.clear();
        mBorder = false;
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
        exit = null;
        treasure = null;
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
        localCoordinate = new Coordinate(mazeWidth, mazeHeight);
        rowContainsPortal = new boolean[mazeHeight];
        yCoordinateDefined = true;
        xCoordinateDefined = true;
        localPath = new ArrayList<>();
        localAdjacencyLinks = new ArrayList<>();
        localsTree = new LocalsTree();
        newLocalCoordinate = null;
        oldRealCoordinate = null;
        newRealCell = null;
        newRealCoordinate = null;
        currentLevel = localsTree.root;
        mBorder = false;
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
