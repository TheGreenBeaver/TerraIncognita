package greenbeaver.terraincognita;

import greenbeaver.terraincognita.model.cellConstruction.Coordinate;
import greenbeaver.terraincognita.model.cellConstruction.Direction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Tester {

    // checks that Direction returns proper perpendicular Direction
    @Test
    public void perpendicular() {
        Direction left = Direction.LEFT;
        assertEquals(left.firstPerpendicular(), Direction.UP);
        Direction down = Direction.DOWN;
        assertEquals(Direction.LEFT, down.firstPerpendicular());
    }

    // checks that Direction returns proper opposite Direction
    @Test
    public void opposite() {
        Direction up = Direction.UP;
        assertEquals(Direction.DOWN, up.opposite());
        Direction right = Direction.RIGHT;
        assertEquals(Direction.LEFT, right.opposite());
    }

    // checks that Direction can be received correctly by its toX and toY parameters
    @Test
    public void byConstructor() {
        boolean exceptionHappened = false;
        try {
            Direction d = Direction.getByConstructor(6, 7);
        } catch (IllegalStateException e) {
            exceptionHappened = true;
        }
        assertTrue(exceptionHappened);

        assertEquals(Direction.LEFT, Direction.getByConstructor(-1, 0));
    }

    // checks that Directions add proper values to X and Y coordinates
    @Test
    public void xy() {
        assertEquals(1, Direction.DOWN.getToY());
        assertEquals(0, Direction.UP.getToX());
    }

    // checks that Coordinate is properly converted to String
    @Test
    public void coordinateString() {
        assertEquals("X: 45; Y: 11", new Coordinate(45, 11).toString());
    }
}
