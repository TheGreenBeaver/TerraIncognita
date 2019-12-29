package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;
import greenbeaver.terraincognita.model.Util;
import javafx.scene.image.Image;

public enum CellType {
    TREASURE(true, 1, 0, Util.TREASURE, Util.H_TREASURE, Util.PH_TREASURE),
    ENTRANCE(true, 1, 0, Util.ENTRANCE, Util.H_ENTRANCE, Util.PH_ENTRANCE),
    EXIT(true, 1, 0, Util.EXIT, Util.H_EXIT, Util.PH_EXIT),

    EMPTY(true, MainEngine.cellAmount(), MainEngine.cellAmount(), Util.FLOOR, Util.H_FLOOR, Util.PH_FLOOR),
    WALL(false, MainEngine.cellAmount(), 0, Util.WALL, Util.H_WALL, Util.PH_WALL),
    PORTAL(true, 10, 0, Util.PORTAL, null, null);

    private final boolean reachable;
    private int maxAmount;
    private final Image image;
    private final Image hImage;
    private final Image phImage;
    private int usedAmount;

    CellType(boolean reachable, int maxAmount, int usedAmount, Image image, Image hImage, Image phImage) {
        this.reachable = reachable;
        this.maxAmount = maxAmount;
        this.usedAmount = usedAmount;
        this.image = image;
        this.hImage = hImage;
        this.phImage = phImage;
    }

    public boolean isReachable() {
        return reachable;
    }

    private boolean unavailable() {
        return usedAmount >= maxAmount;
    }

    public CellType switchType() {
        usedAmount--;

        int index = (ordinal() + 1 == values().length) ? 0 : ordinal() + 1;
        while (values()[index].unavailable()) {
            index = (index == values().length - 1) ? 0 : index + 1;
        }

        CellType actual = values()[index];
        actual.usedAmount++;
        return actual;
    }

    public Image getImage() {
        return image;
    }

    public Image getHImage() {
        return hImage;
    }

    public Image getPhImage() {
        return phImage;
    }

    public enum FieldState {
        GOOD,
        ONE_PORTAL,
        UNUSED_ESSENTIALS
    }

    public static FieldState fieldFilled() {
        if (TREASURE.unavailable() && ENTRANCE.unavailable() && EXIT.unavailable() && PORTAL.usedAmount != 1) {
            return FieldState.GOOD;
        }

        if (PORTAL.usedAmount == 1) {
            return FieldState.ONE_PORTAL;
        }

        return FieldState.UNUSED_ESSENTIALS;
    }

    public int getUsedAmount() {
        return usedAmount;
    }

    public static void flush() {
        for (CellType type: values()) {
            type.usedAmount = type == EMPTY ? MainEngine.cellAmount() : 0;
            if (type == WALL || type == EMPTY) {
                type.maxAmount = MainEngine.cellAmount();
            }
        }
    }
}
