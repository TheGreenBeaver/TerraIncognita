package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MainEngine {

    private static class AdjLink {
        private final Coordinate from;
        private final Coordinate to;
        private final boolean bothDir;

        private AdjLink(Coordinate from, Coordinate to, boolean bothDir) {
            this.from = from;
            this.to = to;
            this.bothDir = bothDir;
        }
    }

    private static class LocalsTree {
        private static class Level {
            private final int id;
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
            private ArrayList<AdjLink> lLocalAdjacencyLinks;

            private boolean pathToExit; // true if this level must be passed to reach exit; used when returning to previous levels to then find this place again

            private Level(Level parent, Coordinate portalFromParent, Coordinate gPortalFromParent) {
                this.id = levelId++;
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

            @Override
            public boolean equals(Object obj) {
                assert obj != null;

                if (!obj.getClass().equals(Level.class)) {
                    return false;
                }

                Level other = (Level) obj;

                return other.id == this.id;
            }
        }

        private static Level root;
        private static Stack<Direction> unblindLastMoves;

        private static void clear() {
            root = new Level(null, null, null);
            root.parents = new ArrayList<>();

            unblindLastMoves = new Stack<>();
        }

        // Used when falling into a portal. Creates new Level and saves the properties of the current one in case there'll be a need to return
        static void add(Direction direction) {
            Coordinate p = currentLevel.equals(root) ? oldRealCoordinate.copy() : localCoordinate.copy();
            p = p.add(direction); // Coordinate of portal at the current Level

            saveCurrentState();

            blindMode = true;

            Level level = new Level(currentLevel, new Coordinate(mazeWidth, mazeHeight), newRealCoordinate);
            currentLevel.children.put(level, p);
            level.last = direction;
            currentLevel = level; // switch to new level
        }

        private static void saveCurrentState() {
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
        static void previousState() {

            int lastPseudo = pseudo.pop();
            for (int i = steps.size() - 1; i >= lastPseudo; i--) {
                steps.get(i).setB(false);
            }

            Pair<Coordinate, Coordinate> toReturn = portalStack.pop(); // A is realCoordinate, B is localCoordinate or null if the portal was not met in the blindMode
            Coordinate rp = toReturn.getA(); // realCoordinate of the entering portal
            if (toReturn.getB() == null) { // if we travelled from unblind to unblind and don't need to change level
                rp.setCoordinateState(Coordinate.CoordinateState.KNOWN_BAD_PORTAL, null);
                Coordinate c = rp.add((blindMode ? currentLevel.last : unblindLastMoves.pop()).opposite());
                currentCell = maze[c.getY()][c.getX()];
                blindMode = false;
                currentLevel = root;
                return;
            }

            Coordinate lp = toReturn.getB();
            Level actualParent = currentLevel.parent;
            if (actualParent == null) {
                for (Level l : currentLevel.parents) {
                    for (Map.Entry<Level, Coordinate> entry : l.children.entrySet()) {
                        if (currentLevel.equals(entry.getKey())) {
                            actualParent = l;
                            break;
                        }
                    }
                }
            }

            boolean withDelete = !currentLevel.pathToExit;
            // if the Level the Player's just explored is a part of the portal path to exit and they're just returning back to then visit this Level at the very end, we don't need to delete this level

            if (!withDelete) {
                saveCurrentState();
            } else {
                Objects.requireNonNull(actualParent).children.remove(currentLevel);
            }

            lastTried = blindMode ? currentLevel.last : unblindLastMoves.pop();
            lastCalculatedDirectionFailed = true;
            shift = false;
            // these two guarantee that when returned to the previous Level, the Player will search for a new Direction to move
            currentLevel = actualParent;
            failCount = Objects.requireNonNull(currentLevel).lFailCount + 1;
            // HIGHER LEVEL FROM NOW ON!!!
            currentLevel.pathToExit = !withDelete;

            yCoordinateDefined = currentLevel.lYCoordinateDefined;
            xCoordinateDefined = currentLevel.lXCoordinateDefined;
            Coordinate newCurr = lp.add(lastTried.opposite());
            if (!currentLevel.equals(root)) { // non-blind level just does not have such attributes
                localPath = new ArrayList<>(currentLevel.lLocalPath);
                localAdjacencyLinks = new ArrayList<>(currentLevel.lLocalAdjacencyLinks);
                localCoordinate = newCurr;
                blindMode = true;
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
            lp.setCoordinateState(withDelete
                            ? Coordinate.CoordinateState.KNOWN_BAD_PORTAL
                            : Coordinate.CoordinateState.KNOWN_PORTAL_TO_EXIT,
                    null);
            Coordinate rC = rp.add(lastTried.opposite());
            currentCell = maze[rC.getY()][rC.getX()];
        }

        static void unblind() {
            Level lParent = currentLevel.parent;
            unblindLastMoves.push(currentLevel.last);
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
            for (AdjLink cAndC : localAdjacencyLinks) {
                int from = cAndC.from.getRawNumber();
                int to = cAndC.to.getRawNumber();
                adjacencyMatrix[from][to] = true;
                if (cAndC.bothDir) {
                    adjacencyMatrix[to][from] = true;
                }
            }

            Coordinate relation = currentLevel.gPortalFromParent.subtract(currentLevel.portalFromParent);
            Coordinate newCurr = localCoordinate.add(relation.getX(), relation.getY());
            currentCell = maze[newCurr.getY()][newCurr.getX()];

            if (exit != null && !exit.getB().equals(root) && exit.getB().equals(currentLevel)) {
                Coordinate e = exit.getA().add(relation.getX(), relation.getY());
                exit = new Pair<>(e, root);
            }
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
    private static int initialShift;
    private static boolean shift; // initially set to false in solve(); shows if the Player should now change their X coordinate in case they are at the bottom or top border
    private static Direction general; // initially set to Direction.UP() in solve(); shows what overall direction the Player is moving now, not paying attention to firstStep or shift
    private static ArrayList<Pair<Coordinate, Boolean>> steps; // initially set to an ArrayList with only entrance in it in solve(); stores all the cells that the Player visited or tried to visit
    private static MoveResult moveResult; // initially set to null in solve()
    private static Direction lastTried; // initially set to null in solve()
    private static int failCount; // initially set to 0 in solve; shows how much times in a row the Player failed to move in a calculated direction
    private static boolean lastCalculatedDirectionFailed; // initially set to false in solve()
    private static Coordinate current; // initially set to null in solve(); shows the coordinate where the Player was at the moment when a new direction is calculated
    private static Direction moment; // initially set to null in solve(); shows the direction that the Player should follow to reach the nearest unknown cell after bfs
    private static Coordinate[] portalTransitions; // set by setPortalTransitions() from MazeEditorController when the maze is created
    private static boolean blindMode; // initially set to false in solve(); shows if Player now knows his exact coordinate
    private static Coordinate localCoordinate; // initially set to null in solve(); used instead of real coordinate during blindMode
    private static boolean yCoordinateDefined;
    private static boolean xCoordinateDefined;
    private static ArrayList<Pair<Coordinate, Coordinate.CoordinateState>> localPath;
    private static ArrayList<AdjLink> localAdjacencyLinks;
    private static Coordinate newLocalCoordinate;
    private static Coordinate oldRealCoordinate;
    private static Cell newRealCell;
    private static Coordinate newRealCoordinate;
    private static LocalsTree.Level currentLevel;
    private static boolean mBorder;
    private static int impossibleDirections;
    private static int levelId;
    private static Coordinate rTreasure;
    private static int range;
    private static Stack<Pair<Coordinate, Coordinate>> portalStack;
    private static boolean inSearchForExit;
    private static Stack<Integer> pseudo;

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

    public static Coordinate getEntrance() { return entrance; }

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

    public static Coordinate getRTreasure() {
        return rTreasure;
    }

    public static boolean treasureFound() {
        return treasure != null && rTreasure != null;
    }

    public static boolean exitReached() {
        return exit != null;
    }

    private static Direction calculateDirection() {

        current = blindMode ? localCoordinate : currentCell.getCoordinate(); // coordinate where the player was BEFORE moving!!!

        if (firstStep) {
            int cutOff = general.getHorizontal() ? mazeHeight - 1 : mazeWidth - 1;
            int point = general.getHorizontal() ? current.getY() : current.getX();
            if ((point < cutOff && point > 0 || blindMode) && initialShift != 0) { // if not at the bottom and haven't met any obstacles on the way down yet
                if (initialShift < 0) {
                    initialShift++;
                    return general.firstPerpendicular();
                } else {
                    initialShift--;
                    return general.firstPerpendicular().opposite();
                }
            } else {
                firstStep = false;
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
        } else if (blindMode && mBorder) {
            if (!shift) {
                shift = true;
                general = lastTried;
                return lastTried.firstPerpendicular();
            } else {
                shift = false;
                mBorder = false;
                general = general.opposite();
            }
        } else if (!blindMode) {
            Coordinate wouldBe = current.add(general);
            if (!wouldBe.fits()) {
                if (shift) {
                    general = general.opposite();
                    shift = false;
                } else {
                    shift = true;
                    return general.firstPerpendicular();
                }
            }
        }

        return general;
    }

    private static boolean handleDirectionFailure(Direction probable) {
        Coordinate wouldBe = current.add(probable);
        if (wouldBe.fitsLocally() || wouldBe.fits()) {
            Coordinate.CoordinateState probableState = wouldBe.getCoordinateState();
            if (probableState == Coordinate.CoordinateState.UNKNOWN
                    || probableState == Coordinate.CoordinateState.KNOWN_REACHABLE) {
                return true;
            }
        }
        failCount++;
        return false;
    }

    private static Direction finalCheck(Direction start) {
        impossibleDirections = 0;

        Direction[] variants = {start,
                start.firstPerpendicular(),
                start.firstPerpendicular().opposite(),
                start.opposite()};

        for (Direction direction : variants) {
            Coordinate wouldBe = current.add(direction);
            if (wouldBe.fits() || wouldBe.fitsLocally()) {
                Coordinate.CoordinateState state = wouldBe.getCoordinateState();
                if (state == Coordinate.CoordinateState.UNKNOWN
                        || state == Coordinate.CoordinateState.KNOWN_REACHABLE) {
                    return direction;
                }
                if (state == Coordinate.CoordinateState.KNOWN_UNREACHABLE
                        || state == Coordinate.CoordinateState.KNOWN_MAZE_BORDER
                        || state == Coordinate.CoordinateState.KNOWN_BAD_PORTAL) {
                    impossibleDirections++;
                }
            } else {
                impossibleDirections++;
            }
        }

        return null;
    }

    private static boolean successfulMoveScenario() {
        failCount = 0;
        steps.add(new Pair<>(newRealCoordinate, true));

        neighbours(blindMode ? newLocalCoordinate : newRealCoordinate, newRealCoordinate);

        Coordinate fromC = blindMode ? localCoordinate : oldRealCoordinate;
        int from = fromC.getRawNumber();
        int to = blindMode ? newLocalCoordinate.getRawNumber() : newRealCoordinate.getRawNumber();
        boolean[][] adj = blindMode ? localAdjacencyMatrix : adjacencyMatrix;
        Coordinate.CoordinateState state = fromC.getCoordinateState();

        currentCell = newRealCell;

        adj[from][to] = true;
        boolean both = false;
        if (state != Coordinate.CoordinateState.KNOWN_PORTAL) {
            both = true;
            adj[to][from] = true;
        }
        if (state == Coordinate.CoordinateState.KNOWN_PORTAL && !blindMode) {
            HashMap<Coordinate, Integer> nop = UIHandler.getNumsOfPortals();
            int cn = nop.get(fromC);
            int cond = cn == 0 ? nop.size() : cn;
            Coordinate distantPortal = new Coordinate(0, 0);
            Iterator<Map.Entry<Coordinate, Integer>> iter = nop.entrySet().iterator();
            while (cond != 0) {
                distantPortal = iter.next().getKey();
                cond--;
            }
            for (Direction direction : Direction.values()) {
                int neighbour = fromC.add(direction).getRawNumber();
                if (adj[neighbour][distantPortal.getRawNumber()]) {
                    adj[to][distantPortal.getRawNumber()] = true;
                }
            }
        }
        if (!blindMode) {
            newRealCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_REACHABLE, null);
        } else {
            localAdjacencyLinks.add(new AdjLink(oldRealCoordinate.copy(), newRealCoordinate.copy(), both));
            localPath.add(new Pair<>(newRealCoordinate, Coordinate.CoordinateState.KNOWN_REACHABLE));
            localCoordinate = newLocalCoordinate;
            newLocalCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_REACHABLE, null);
        }

        switch (currentCell.getCellType()) {
            case TREASURE: {
                if (blindMode && treasure != null && treasure.getB().equals(LocalsTree.root)) { // if the treasure's been found while in non-blind mode, than it can be used as marker to unblind
                    LocalsTree.unblind();
                }
                if (treasure == null) {
                    treasure = new Pair<>(blindMode ? newLocalCoordinate : newRealCoordinate, currentLevel);
                    rTreasure = newRealCoordinate;
                }
                if (exit != null) {
                    if (exit.getB().equals(currentLevel)) {
                        try {
                            ArrayList<Integer> path =
                                    bfsSpotToSpot(blindMode ? newLocalCoordinate : newRealCoordinate, exit.getA(), adj);
                            manageBFSPath(path, newRealCoordinate, newLocalCoordinate);
                            return true;
                        } catch (NullPointerException e) {
                            inSearchForExit = true;
                            return false;
                        }
                    }

                    LocalsTree.Level levelOfExit = exit.getB();
                    Stack<LocalsTree.Level> pathToExit = findPathToExit(levelOfExit, currentLevel);

                    LocalsTree.Level currentLvl = currentLevel;
                    Coordinate currentC = blindMode ? newLocalCoordinate : newRealCoordinate;
                    if (pathToExit != null) {
                        while (!pathToExit.isEmpty()) {
                            LocalsTree.Level next = pathToExit.pop();
                            Coordinate nextC = new Coordinate(0, 0);
                            for (Map.Entry<LocalsTree.Level, Coordinate> entry : currentLvl.children.entrySet()) {
                                if (next.equals(entry.getKey())) {
                                    nextC = entry.getValue().add(next.last.opposite());
                                }
                            }
                            boolean[][] cAdj = currentLvl.equals(LocalsTree.root) ? adjacencyMatrix : currentLvl.lAdjacencyMatrix;
                            blindMode = !currentLvl.equals(LocalsTree.root);

                            ArrayList<Integer> path = bfsSpotToSpot(currentC, nextC, cAdj);
                            manageBFSPath(path, currentLvl.gPortalFromParent, currentLvl.portalFromParent);

                            currentLvl = next;
                            currentC = currentLvl.portalFromParent;
                        }

                        blindMode = !currentLvl.equals(LocalsTree.root);
                        ArrayList<Integer> path = bfsSpotToSpot(currentC, exit.getA(), currentLvl.lAdjacencyMatrix);
                        manageBFSPath(path, currentLvl.gPortalFromParent, currentLvl.portalFromParent);
                        return true;
                    }
                }
                break;
            }

            case ENTRANCE: {
                LocalsTree.unblind();
                break;
            }

            case EXIT: {

                if (exit != null && blindMode && exit.getB().equals(LocalsTree.root)) {
                    LocalsTree.unblind();
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

    private static void manageBFSPath(ArrayList<Integer> path, Coordinate subtractFrom, Coordinate toSubtract) {
        if (blindMode) {
            Coordinate relation = subtractFrom.subtract(toSubtract);
            for (int i = 1; i < path.size(); i++) {
                Coordinate localByRaw = Coordinate.getByRawNumber(path.get(i));
                Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                steps.add(new Pair<>(realByLocal, true));
            }
        } else {
            for (int i = 1; i < path.size(); i++) {
                Coordinate c = Coordinate.getByRawNumber(path.get(i));
                steps.add(new Pair<>(c, true));
            }
        }
    }

    private static Stack<LocalsTree.Level> findPathToExit(@NotNull LocalsTree.Level current,
                                                          @NotNull LocalsTree.Level searching) {
        Stack<LocalsTree.Level> path = new Stack<>();

        if (current.parent != null) {
            path.push(current);
        }

        LocalsTree.Level curr = current.parent == null ? current : current.parent;
        boolean found = current.parent != null && curr.equals(searching);

        while (!curr.equals(searching) && curr.parent != null) {
            path.push(curr);
            curr = curr.parent;
            if (curr.equals(searching)) {
                found = true;
            }
        }

        if (found) {
            return path;
        }

        if (curr.parents != null) {
            path.push(curr);
            ArrayList<LocalsTree.Level> probableAdditionToPath = new ArrayList<>();

            for (LocalsTree.Level p : curr.parents) {
                probableAdditionToPath.clear();
                LocalsTree.Level lvl = p;

                while (!lvl.equals(searching) && lvl.parent != null) {
                    probableAdditionToPath.add(lvl);
                    lvl = lvl.parent;
                    if (lvl.equals(searching)) {
                        found = true;
                    }
                }

                if (found) {
                    for (LocalsTree.Level l : probableAdditionToPath) {
                        path.push(l);
                    }
                    return path;
                }
            }
        }

        return null;
    }

    private static void neighbours(Coordinate center, Coordinate realCenter) {
        int cRaw = center.getRawNumber();

        Coordinate relation = realCenter.subtract(center);

        boolean[][] adj = blindMode ? localAdjacencyMatrix : adjacencyMatrix;

        for (Direction direction : Direction.values()) {
            Coordinate neighbour = center.add(direction);
            int nRaw = neighbour.getRawNumber();
            if (neighbour.fitsLocally() || neighbour.fits()) {
                Coordinate.CoordinateState nState = neighbour.getCoordinateState();
                Coordinate.CoordinateState cState = center.getCoordinateState();
                boolean both = false;
                boolean a = false;
                boolean inverse = false;
                if (nState == Coordinate.CoordinateState.KNOWN_REACHABLE) {
                    adj[cRaw][nRaw] = true;
                    if (cState != Coordinate.CoordinateState.KNOWN_PORTAL
                            && cState != Coordinate.CoordinateState.KNOWN_PORTAL_TO_EXIT) {
                        adj[nRaw][cRaw] = true;
                        both = true;
                    }
                    a = true;
                }
                if ((nState == Coordinate.CoordinateState.KNOWN_PORTAL || nState == Coordinate.CoordinateState.KNOWN_PORTAL_TO_EXIT)
                        && cState != Coordinate.CoordinateState.KNOWN_PORTAL
                        && cState != Coordinate.CoordinateState.KNOWN_PORTAL_TO_EXIT) {
                    adj[nRaw][cRaw] = true;
                    inverse = true;
                    a = true;
                }
                if (a && blindMode) {
                    Coordinate rNeighbour = neighbour.add(relation.getX(), relation.getY());
                    AdjLink toAdd = inverse
                            ? new AdjLink(rNeighbour, realCenter.copy(), false)
                            : new AdjLink(realCenter.copy(), rNeighbour, both);
                    localAdjacencyLinks.add(toAdd);
                }
            }
        }
    }

    private static boolean makeMove() {

        if (inSearchForExit && exit != null && exit.getB().equals(currentLevel)) {
            try {
                ArrayList<Integer> path =
                        bfsSpotToSpot(blindMode ? newLocalCoordinate : newRealCoordinate, exit.getA(), blindMode ? localAdjacencyMatrix : adjacencyMatrix);
                manageBFSPath(path, newRealCoordinate, newLocalCoordinate);
                return true;
            } catch (NullPointerException ignored) {
            }
        }
        Direction dir = moment == null ? finalCheck(calculateDirection()) : moment; // if we've just used some method that gives us the proper Direction for this moment, we don't need to calculate it

        if (dir == null) { // no more cells to try
            if (impossibleDirections == 4) { // not even a not-KNOWN_BAD portal nearby
                return block();
            } else {
                Coordinate p = blindMode
                        ? new Coordinate(mazeWidth, mazeHeight)
                        : currentLevel.gPortalFromParent;
                try {
                    Coordinate move = blindMode
                            ? p.subtract(localCoordinate)
                            : p.subtract(currentCell.getCoordinate());
                    moment = Direction.getByConstructor(move.getX(), move.getY());
                    p.setCoordinateState(Coordinate.CoordinateState.UNKNOWN, null);
                } catch (Exception e) {
                    for (Direction direction : Direction.values()) {
                        Coordinate coord = currentCell.getCoordinate().add(direction);
                        if ((coord.fits() || coord.fitsLocally()) && coord.getCoordinateState() == Coordinate.CoordinateState.KNOWN_PORTAL) {
                            moment = direction;
                            currentCell.getCoordinate().add(direction).setCoordinateState(Coordinate.CoordinateState.UNKNOWN, null);
                        }
                    }
                }
            }
            return false;
        }
        moveResult = currentCell.move(dir);
        moment = null;


        // Old local Coordinate is localCoordinate itself
        newLocalCoordinate = localCoordinate.add(dir);

        // Old real Cell is currentCell itself
        newRealCell = moveResult.getResult();

        oldRealCoordinate = currentCell.getCoordinate();
        newRealCoordinate = newRealCell.getCoordinate();

        switch (moveResult) {
            case SUCCESSFUL: { // does count as a step
                return successfulMoveScenario();
            }

            case UNREACHABLE_CELL: { // does count as a step
                Coordinate forCalculation = blindMode ? newLocalCoordinate : newRealCoordinate;
                forCalculation.setCoordinateState(Coordinate.CoordinateState.KNOWN_UNREACHABLE, null);
                steps.add(new Pair<>(newRealCoordinate.copy(), true)); // in steps, we always store real Coordinates to then show them to the User
                if (blindMode) {
                    localPath.add(new Pair<>(newRealCoordinate.copy(), Coordinate.CoordinateState.KNOWN_UNREACHABLE));
                }
                lastCalculatedDirectionFailed = true;
                if (failCount++ == 0) {
                    lastTried = dir;
                }
                firstStep = false;
                shift = false;

                if (failCount >= 4) {
                    if (block()) return true;
                }
                break;
            }

            case ALREADY_VISITED_CELL: { // does not count as a step

                neighbours(blindMode ? localCoordinate : oldRealCoordinate, oldRealCoordinate);

                failCount = 0;
                Pair<ArrayList<Integer>, Direction> d =
                        bfsNearest(blindMode ? localCoordinate : oldRealCoordinate,
                                blindMode ? localAdjacencyMatrix : adjacencyMatrix);
                if (d == null) { // if there are no more unknown cells possible to visit
                    if (blindMode) {
                        Coordinate relation = oldRealCoordinate.subtract(localCoordinate);
                        if (!currentLevel.triedEscapingThroughEntrance()) { // if in blindMode, the Player should first try escaping according to rules through the same portal they've got to the currentLevel
                            Coordinate pfp = currentLevel.portalFromParent;
                            ArrayList<Integer> temp = new ArrayList<>(4);
                            int pfpN = pfp.getRawNumber();
                            for (Direction direction : Direction.values()) {
                                Coordinate t = pfp.add(direction);
                                if (t.fitsLocally()
                                        && t.getCoordinateState() == Coordinate.CoordinateState.KNOWN_REACHABLE) {
                                    int tN = t.getRawNumber();
                                    temp.add(tN);
                                    localAdjacencyMatrix[tN][pfpN] = true;
                                }
                            }
                            ArrayList<Integer> pathToLastEnteringPortal =
                                    bfsSpotToSpot(localCoordinate, currentLevel.portalFromParent, localAdjacencyMatrix);
                            for (int t : temp) {
                                localAdjacencyMatrix[t][pfpN] = false;
                            }
                            int s = pathToLastEnteringPortal.size();
                            // last move in the calculated path must be made through makeMove
                            if (s == 1) { // if the Player only needs to make one step to reach the portal
                                Coordinate move = currentLevel.portalFromParent.subtract(localCoordinate);
                                moment = Direction.getByConstructor(move.getX(), move.getY());
                            } else {
                                Coordinate preLast = null;
                                for (int i = 1; i < s; i++) {
                                    Coordinate localByRaw = Coordinate.getByRawNumber(pathToLastEnteringPortal.get(i));
                                    Coordinate realByLocal = localByRaw.add(relation.getX(), relation.getY());
                                    if (i == s - 2) {
                                        preLast = realByLocal;
                                        currentCell = maze[realByLocal.getY()][realByLocal.getX()];
                                        localCoordinate = localByRaw;
                                    }
                                    if (i < s - 1) {
                                        steps.add(new Pair<>(realByLocal, true));
                                    } else {
                                        assert preLast != null;
                                        Coordinate lastMove = realByLocal.subtract(preLast);
                                        moment = Direction.getByConstructor(lastMove.getX(), lastMove.getY());
                                        lastTried = moment;
                                    }
                                }
                            }
                            // changing back to UNKNOWN so that moveResult.PORTAL triggers
                            currentLevel.portalFromParent.setCoordinateState(Coordinate.CoordinateState.UNKNOWN, null);
                        } else {
                            if (treasure != null && treasure.getB().equals(currentLevel)) { // if it turns out that the treasure is only reachable through this Level and this Level can't be left normally, than the game is unwinnable
                                return true;
                            }
                            LocalsTree.previousState();
                        }
                    } else {
                        return true;
                    }
                } else {
                    moment = d.getB();

                    lastTried = moment;

                    ArrayList<Integer> path = d.getA();

                    if (!path.isEmpty()) {
                        manageBFSPath(path, oldRealCoordinate, localCoordinate);

                        if (blindMode) {
                            Coordinate relation = oldRealCoordinate.subtract(localCoordinate);
                            localCoordinate = Coordinate.getByRawNumber(path.get(path.size() - 1));
                            Coordinate curr = localCoordinate.add(relation.getX(), relation.getY());
                            currentCell = maze[curr.getY()][curr.getX()];
                        } else {
                            oldRealCoordinate = Coordinate.getByRawNumber(path.get(path.size() - 1));
                            currentCell = maze[oldRealCoordinate.getY()][oldRealCoordinate.getX()];
                        }

                    }
                }

                break;
            }

            // can only happen in blindMode
            case MAZE_BORDER: {
                Coordinate overTheBorder = oldRealCoordinate.add(dir);

                if (!dir.getHorizontal()) {
                    yCoordinateDefined = true;
                } else {
                    xCoordinateDefined = true;
                }

                firstStep = false;
                mBorder = true;
                shift = false;
                failCount++;

                lastTried = dir;

                steps.add(new Pair<>(overTheBorder, true));
                newLocalCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_MAZE_BORDER, dir);

                if (yCoordinateDefined && xCoordinateDefined) {
                    LocalsTree.unblind();
                }

                break;
            }

            case PORTAL: {
                Coordinate inPortal = blindMode ? localCoordinate.add(dir) : oldRealCoordinate.add(dir);

                int comparingCoordinateIndex = steps.size() - 1;
                while (comparingCoordinateIndex >= 0
                        && (!steps.get(comparingCoordinateIndex).getA().fits()
                        || !steps.get(comparingCoordinateIndex).getA().getCell().getCellType().isReachable())) {
                    comparingCoordinateIndex--;
                }

                if (!steps.get(comparingCoordinateIndex).getA().equals(oldRealCoordinate)) {
                    steps.add(new Pair<>(oldRealCoordinate, true));
                }
                pseudo.push(steps.size());
                steps.add(new Pair<>(oldRealCoordinate.add(dir), true));
                steps.add(new Pair<>(newRealCoordinate, true));

                inPortal.setCoordinateState(Coordinate.CoordinateState.KNOWN_PORTAL, null);
                portalStack.push(new Pair<>(oldRealCoordinate.add(dir), blindMode ? localCoordinate.add(dir) : null));
                LocalsTree.add(dir);

                localCoordinate = new Coordinate(mazeWidth, mazeHeight);
                currentCell = newRealCell;
                clearLocals();
                localCoordinate.setCoordinateState(Coordinate.CoordinateState.KNOWN_PORTAL, null);
            }
        }

        return false;
    }

    private static boolean block() {
        if ((treasure == null || !treasure.getB().equals(currentLevel)) && !portalStack.isEmpty()) {
            LocalsTree.previousState();
        } else {
            if (treasure != null && treasure.getB().equals(currentLevel)) {
                treasure = null;
                rTreasure = null;
            }
            return true;
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

        Random random = new Random();
        int directionIndex = random.nextInt(2) + range;
        if (directionIndex > 3) {
            directionIndex = 0;
        }

        general = Direction.values()[directionIndex];
        int bound = mazeWidth <= 9 ? 3 : mazeWidth / 3;
        initialShift = random.nextInt(bound) * (random.nextInt(3) - 1);
        shift = false;
        failCount = 0;
        Coordinate.clearLocalCoordinateStates();
        yCoordinateDefined = false;
        xCoordinateDefined = false;
        localPath.clear();
        localAdjacencyLinks.clear();
        mBorder = false;
    }

    private static Pair<ArrayList<Integer>, Direction> bfsNearest(Coordinate startC, boolean[][] matrixToUse) {
        int start = startC.getRawNumber();
        int actualSize = matrixToUse.length;
        ArrayList<ArrayList<Integer>> paths = new ArrayList<>(actualSize);
        for (int i = 0; i < actualSize; i++) {
            paths.add(new ArrayList<>());
        }
        boolean[] visited = new boolean[actualSize];
        visited[start] = true;
        ArrayDeque<Integer> queue = new ArrayDeque<>(actualSize);
        queue.add(start);

        while (!queue.isEmpty()) {
            int a = queue.poll();
            for (int i = 0; i < actualSize; i++) {
                Coordinate aC = Coordinate.getByRawNumber(a);
                Coordinate iC = Coordinate.getByRawNumber(i);
                int yDiff = iC.getY() - aC.getY();
                int xDiff = iC.getX() - aC.getX();
                if (Math.abs(yDiff) + Math.abs(xDiff) == 1 && iC.getCoordinateState() == Coordinate.CoordinateState.UNKNOWN) {
                    Direction direction = Direction.getByConstructor(xDiff, yDiff);
                    return new Pair<>(paths.get(a), direction);
                }
                if (matrixToUse[a][i] && !visited[i]) {
                    paths.set(i, new ArrayList<>(paths.get(a)));
                    paths.get(i).add(i);
                    visited[i] = true;
                    queue.add(i);
                }
            }
        }

        return null;
    }

    private static ArrayList<Integer> bfsSpotToSpot(Coordinate startC, Coordinate destC, boolean[][] matrixToUse) {
        int start = startC.getRawNumber();
        int dest = destC.getRawNumber();

        int actualSize = matrixToUse.length;
        ArrayList<ArrayList<Integer>> paths = new ArrayList<>(actualSize);
        for (int i = 0; i < actualSize; i++) {
            paths.add(new ArrayList<>());
        }

        boolean[] visited = new boolean[actualSize];
        visited[start] = true;

        ArrayDeque<Integer> queue = new ArrayDeque<>(actualSize);
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

    public static void solve(int range) {
        exit = null;
        treasure = null;
        currentCell = maze[entrance.getY()][entrance.getX()];
        adjacencyMatrix = new boolean[cellAmount()][cellAmount()];
        localAdjacencyMatrix = new boolean[localCellAmount()][localCellAmount()];
        firstStep = true;
        initialShift = 0;
        shift = false;
        general = Direction.UP;
        steps = new ArrayList<>(Collections.singletonList(new Pair<>(entrance, true)));
        moveResult = null;
        lastTried = general;
        failCount = 0;
        lastCalculatedDirectionFailed = false;
        current = null;
        moment = null;
        blindMode = false;
        localCoordinate = new Coordinate(mazeWidth, mazeHeight);
        yCoordinateDefined = true;
        xCoordinateDefined = true;
        localPath = new ArrayList<>();
        localAdjacencyLinks = new ArrayList<>();
        levelId = 0;
        LocalsTree.clear();
        newLocalCoordinate = null;
        oldRealCoordinate = null;
        newRealCell = null;
        newRealCoordinate = null;
        currentLevel = LocalsTree.root;
        mBorder = false;
        Coordinate.setNewField();
        localPath = new ArrayList<>();
        impossibleDirections = 0;
        rTreasure = null;
        MainEngine.range = range;
        portalStack = new Stack<>();
        inSearchForExit = false;
        pseudo = new Stack<>();

        boolean completed = false;

        int cntr = 0;

        while (!completed && cntr++ < Integer.MAX_VALUE / 2) {
            completed = makeMove();
        }
    }
}
