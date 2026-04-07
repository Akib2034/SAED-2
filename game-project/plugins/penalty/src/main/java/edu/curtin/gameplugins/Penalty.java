package edu.curtin.gameplugins;

import edu.curtin.api.GameAPI;
import edu.curtin.api.Plugin;
import java.util.List;

public class Penalty implements Plugin {
    private GameAPI api;
    private long lastMove;

    @Override
    public void init(GameAPI api) {
        this.api = api;
        this.lastMove = api.nowMillis();
        System.out.println("[Penalty] Loaded");
    }

    @Override
    public void onPlayerMove() {
        long now = api.nowMillis();
        if (now - lastMove > 5000) {
            int[] pos = api.getPlayerPosition();
            int r = pos[0];
            int c = pos[1];
            int[][] d = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

            boolean placed = false;
            for (int i = 0; i < d.length && !placed; i++) {
                int rr = r + d[i][0];
                int cc = c + d[i][1];

                if (rr < 0 || cc < 0 || rr >= api.getRows() || cc >= api.getCols()) {
                    continue;
                }

                var req = api.getObstacleRequiresAt(rr, cc);
                if (req == null || req.isEmpty()) {
                    api.placeObstacle(rr, cc, List.of("PenaltyKey"));
                    System.out.println("[Penalty] Placed penalty obstacle at (" + rr + "," + cc + ")");
                    placed = true;
                }
            }
        }
        lastMove = now;
    }

    @Override public void onItemAcquire(String itemName) { }
    @Override public boolean hasButton() { return false; }
    @Override public void onPluginInvoked() {  }
}
