package edu.curtin.game;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private int row, col;
    private List<String> inventory = new ArrayList<>();

    public Player(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public void moveUp() { row--; }
    public void moveDown() { row++; }
    public void moveLeft() { col--; }
    public void moveRight() { col++; }

    public void addItem(String item) { inventory.add(item); }
    public List<String> getInventory() { return inventory; }
}
