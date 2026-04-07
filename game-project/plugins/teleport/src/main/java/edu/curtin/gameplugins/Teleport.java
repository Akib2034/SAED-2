package edu.curtin.gameplugins;

import edu.curtin.api.GameAPI;
import edu.curtin.api.Plugin;
import java.util.Random;

public class Teleport implements Plugin {
    private GameAPI api;
    private boolean used = false;
    private final Random rnd = new Random();

    @Override
    public void init(GameAPI api) {
        this.api = api;
        System.out.println("[Teleport] Loaded");
    }

    @Override public void onPlayerMove() { }
    @Override public void onItemAcquire(String item) { }
    @Override public boolean hasButton() { return true; }

    @Override
    public void onPluginInvoked() {
        if (used) {
            System.out.println("[Teleport] Already used.");
            return;
        }

        int rows = api.getRows();
        int cols = api.getCols();
        boolean teleported = false;

        for (int tries = 0; tries < 200 && !teleported; tries++) {
            int r = rnd.nextInt(rows);
            int c = rnd.nextInt(cols);

            var req = api.getObstacleRequiresAt(r, c);
            if (req != null && !req.isEmpty()) {
                continue;
            }

            api.teleportTo(r, c);
            System.out.println("[Teleport] Teleported to (" + r + "," + c + ")");
            teleported = true;
        }

        if (teleported) {
            used = true;
        } else {
            System.out.println("[Teleport] No safe spot found.");
        }
    }
}
