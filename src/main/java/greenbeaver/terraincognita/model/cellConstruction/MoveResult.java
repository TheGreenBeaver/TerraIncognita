package greenbeaver.terraincognita.model.cellConstruction;

public enum MoveResult {
    SUCCESSFUL,
    MAZE_BORDER,
    UNREACHABLE_CELL,
    ALREADY_VISITED_CELL;

    static Cell result = null;

    static void setResult(Cell newResult) {
        result = newResult;
    }

    public Cell getResult() {
        return result;
    }
}
