package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.CellType;
import greenbeaver.terraincognita.model.cellConstruction.Coordinate;

import java.util.HashMap;
import java.util.Map;

public class UIHandler {
    private static Coordinate currentPortal;
    private static boolean continueWithDangerousInput;
    private static final HashMap<Coordinate, Integer> numsOfPortals;
    private static final int[] amounts;
    static {
        numsOfPortals = new HashMap<>();
        amounts = new int[10];
    }

    public static void clearUIHandler() {
        continueWithDangerousInput = false;
        currentPortal = null;
        numsOfPortals.clear();
        for (int i = 0; i < 10; i++) {
            amounts[i] = 0;
        }
    }

    public static Coordinate getCurrentPortal() {
        return currentPortal;
    }

    public static int getCurrentPortalNum() {
        return numsOfPortals.get(currentPortal);
    }

    public static void setCurrentPortal(Coordinate currentPortal) {
        UIHandler.currentPortal = currentPortal;
    }

    public static void createPortal(Coordinate coordinate) {
        numsOfPortals.put(coordinate, 0);
        amounts[0]++;
    }

    public static void removePortal(Coordinate coordinate) {
        amounts[numsOfPortals.get(coordinate)]--;
        numsOfPortals.remove(coordinate);
    }

    public static void setPortalNum(Coordinate portal, int num) {
        amounts[numsOfPortals.get(portal)]--;
        numsOfPortals.replace(portal, num);
        amounts[num]++;
    }

    public static boolean portalNumsOK() {
        for (int i = 0; i < 10; i++) {
            if (amounts[i] != 0 && numsOfPortals.size() <= i || amounts[i] > 1) {
                return false;
            }
        }

        return true;
    }

    public static Coordinate[] getPortalTransitions() {
        int amount = CellType.PORTAL.getUsedAmount();
        Coordinate[] transitions = new Coordinate[amount];
        for (Map.Entry<Coordinate, Integer> entry: numsOfPortals.entrySet()) {
            transitions[entry.getValue()] = entry.getKey();
        }
        return transitions;
    }

    public static int getNumOfPortal(Coordinate portal) {
        for (Map.Entry<Coordinate, Integer> e : numsOfPortals.entrySet()) {
            if (e.getKey().equals(portal)) {
                return e.getValue();
            }
        }
        throw new NullPointerException("No portal for coordinate " + portal.toString());
    }

    public static void setContinueWithDangerousInput(boolean continueWithDangerousInput) {
        UIHandler.continueWithDangerousInput = continueWithDangerousInput;
    }

    public static boolean getContinueWithDangerousInput() {
        return continueWithDangerousInput;
    }

    public static HashMap<Coordinate, Integer> getNumsOfPortals() {
        return numsOfPortals;
    }
}
