package greenbeaver.terraincognita.model;

import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;

class DestinationWithDirection {
    private Coordinate goingTo; // coordinate that is nearest to the coordinate that the Player is actually trying to reach
    private Direction toReachActual; // direction that should be followed to reach the actually needed coordinate

    DestinationWithDirection(Coordinate goingTo, Direction toReachActual) {
        this.goingTo = goingTo;
        this.toReachActual = toReachActual;
    }

    Coordinate getGoingTo() {
        return goingTo;
    }

    Direction getToReachActual() {
        return toReachActual;
    }
}
