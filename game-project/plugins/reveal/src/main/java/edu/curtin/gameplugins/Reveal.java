package edu.curtin.gameplugins;

import edu.curtin.api.GameAPI;
import edu.curtin.api.Plugin;

public class Reveal implements Plugin {
    private GameAPI api;

    @Override
    public void init(GameAPI api) {
        this.api = api;
        System.out.println("[Reveal] Loaded");
    }

    @Override public void onPlayerMove() { }

    @Override
    public void onItemAcquire(String itemName) {
        if (itemName == null) {
            return;
        }
        String s = itemName.toLowerCase();
        if (s.contains("map")) {
            for (int r = 0; r < api.getRows(); r++) {
                for (int c = 0; c < api.getCols(); c++) {
                    if (api.isGoalAt(r, c) || api.getItemAt(r, c) != null) {
                        api.reveal(r, c);
                    }
                }
            }
            System.out.println("[Reveal] Revealed goal and all item locations.");
        }
    }

    @Override public boolean hasButton() { return false; }
    @Override public void onPluginInvoked() {  }
}
