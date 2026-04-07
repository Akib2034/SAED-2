package edu.curtin.game;

public class Item {
    private String name;
    private int row, col;
    private String message;

    public Item(String name, int row, int col, String message) {
        this.name = name;
        this.row = row;
        this.col = col;
        this.message = message;
    }

    public String getName() { return name; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public String getMessage() { return message; }
}
