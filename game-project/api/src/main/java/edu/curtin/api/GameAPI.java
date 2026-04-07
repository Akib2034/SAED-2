package edu.curtin.api;

import java.util.List;
import java.util.Locale;


public interface GameAPI {
    // Movement
    void movePlayer(String direction);           
    void teleportTo(int row, int col);          
    int[] getPlayerPosition();

    // Grid & visibility
    int getRows();
    int getCols();
    void reveal(int row, int col);             
    boolean isVisible(int row, int col);

    // Contents 
    boolean isGoalAt(int row, int col);
    String getItemAt(int row, int col);        
    List<String> getObstacleRequiresAt(int row, int col); 

    // Inventory
    void addItem(String itemName);
    List<String> getInventory();
    String getMostRecentItem();

    // Obstacles
    void placeObstacle(int row, int col, List<String> requires);

    // Locale
    Locale getLocale();
    void setLocale(Locale locale);

    // Time 
    long nowMillis();
}
