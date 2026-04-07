package edu.curtin.game;

import java.util.List;

public class Obstacle {
    private int row, col;
    private List<String> requiredItems; 

    public Obstacle(int row, int col, List<String> requiredItems) {
        this.row = row;
        this.col = col;
        this.requiredItems = requiredItems;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public List<String> getRequiredItems() { return requiredItems; }
}
