package greenbeaver.terraincognita.model;

public class UIHandler {
    private static boolean continueWithDangerousInput = false;

    public static void setContinueWithDangerousInput(boolean continueWithDangerousInput) {
        UIHandler.continueWithDangerousInput = continueWithDangerousInput;
    }

    public static boolean getContinueWithDangerousInput() {
        return continueWithDangerousInput;
    }
}
