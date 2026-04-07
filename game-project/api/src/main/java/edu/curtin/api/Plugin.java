package edu.curtin.api;

public interface Plugin
{
    
    void init(GameAPI api);

  
    default void onPlayerMove() {}

    
    default void onItemAcquire(String itemName) {}

    
    default boolean hasButton() { return false; }

    
    default void onPluginInvoked() {}
}
