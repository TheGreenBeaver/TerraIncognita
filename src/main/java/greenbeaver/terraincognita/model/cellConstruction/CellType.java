package greenbeaver.terraincognita.model.cellConstruction;

import greenbeaver.terraincognita.model.MainEngine;
import greenbeaver.terraincognita.model.Util;
import javafx.scene.image.Image;

public enum CellType {
    TREASURE(true, 1, 0, Util.TREASURE),
    ENTRANCE(true, 1, 0, Util.ENTRANCE),
    EXIT(true, 1, 0, Util.EXIT),

    EMPTY(true, MainEngine.cellAmount(), MainEngine.cellAmount(), Util.FLOOR),
    WALL(false, MainEngine.cellAmount(), 0, Util.WALL);

    private final boolean reachable;
    private int maxAmount;
    private final Image image;
    private int usedAmount;

    CellType(boolean reachable, int maxAmount, int usedAmount, Image image) {
        this.reachable = reachable;
        this.maxAmount = maxAmount;
        this.usedAmount = usedAmount;
        this.image = image;
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

    public static boolean fieldFilled() {
        return TREASURE.unavailable() && ENTRANCE.unavailable() && EXIT.unavailable();
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
