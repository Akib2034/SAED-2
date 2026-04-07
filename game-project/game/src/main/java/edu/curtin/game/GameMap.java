package edu.curtin.game;

public class GameMap {
    public enum CellType { EMPTY, ITEM, OBSTACLE, GOAL }

    private int rows, cols;
    private CellType[][] cells;
    private boolean[][] visible;

    public GameMap(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        cells = new CellType[rows][cols];
        visible = new boolean[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = CellType.EMPTY;
            }
        }
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public void setCell(int r, int c, CellType type) {
        if (inBounds(r, c)) {
            cells[r][c] = type;
        }
    }

    public CellType getCell(int r, int c) {
        return inBounds(r, c) ? cells[r][c] : null;
    }

    public void revealAround(int r, int c) {
        int[][] d = {{0,0},{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] v : d) {
            int rr = r + v[0], cc = c + v[1];
            if (inBounds(rr, cc)) {
                visible[rr][cc] = true;
            }
        }
    }

    public boolean isVisible(int r, int c) {
        return inBounds(r, c) && visible[r][c];
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < rows && c < cols;
    }
}
