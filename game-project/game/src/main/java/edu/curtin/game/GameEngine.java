package edu.curtin.game;

import edu.curtin.api.GameAPI;
import edu.curtin.api.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class GameEngine implements GameAPI {
    private GameMap map;
    private Player player;
    private List<Item> items = new ArrayList<>();
    private List<Obstacle> obstacles = new ArrayList<>();
    private int goalRow, goalCol;
    private List<Plugin> plugins = new ArrayList<>();
    

    
    private Locale locale = Locale.getDefault();

   
    private Properties messagesProps = new Properties();

   
    private String lastMessage = "";

    public String getLastMessage() {
        return lastMessage;
    }

    private void setMessage(String msg) {
        lastMessage = (msg == null) ? "" : msg;
        System.out.println(lastMessage);
    }

    public GameEngine(GameMap map, Player player, int goalRow, int goalCol) {
        this.map = map;
        this.player = player;
        this.goalRow = goalRow;
        this.goalCol = goalCol;
        reloadMessages(); 
    }

    // Plugin
    public void setPlugins(List<Plugin> plugins) {
        this.plugins = plugins;
        for (Plugin p : plugins) {
            p.init(this);
        }
    }

    

    // Movement
    @Override
    public void movePlayer(String direction) {
        int r = player.getRow();
        int c = player.getCol();
        int nr = r;
        int nc = c;

        switch (direction.toLowerCase(Locale.ROOT)) {
            case "up": {
                nr--;
                break;
            }
            case "down": {
                nr++;
                break;
            }
            case "left": {
                nc--;
                break;
            }
            case "right": {
                nc++;
                break;
            }
            default: {
                return;
            }
        }

        if (!inBounds(nr, nc)) {
            setMessage(tr("status.invalidMove", "Can't move there."));
            return;
        }

        // Obstacle check
        Obstacle obstacle = getObstacleAt(nr, nc);
        if (obstacle != null && !canPass(obstacle)) {
            String need = String.join(", ", obstacle.getRequiredItems());
            setMessage(tr("obstacle.need", "You need: {0}", need));
            return;
        }

        player.setPosition(nr, nc);
        map.revealAround(nr, nc);

        // Item pickup
        Item item = findItemAt(nr, nc);
        if (item != null) {
            player.addItem(item.getName());
            items.remove(item);

            
            if (isGoalAt(nr, nc)) {
                map.setCell(nr, nc, GameMap.CellType.GOAL);
            } else {
                map.setCell(nr, nc, GameMap.CellType.EMPTY);
            }

            
            String key = "item." + sanitize(item.getName()) + ".msg";
            String defaultMsg = (item.getMessage() != null && !item.getMessage().isBlank())
                ? item.getMessage()
                : tr("status.pickup", "Picked up {0}!", item.getName());
            setMessage(tr(key, defaultMsg));

            notifyItemAcquire(item.getName());
        } else {
            setMessage("");
        }

        notifyMove();
        
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < map.getRows() && c < map.getCols();
    }

    private boolean canPass(Obstacle obs) {
        List<String> inv = player.getInventory();
        for (String need : obs.getRequiredItems()) {
            boolean found = inv.stream().anyMatch(i ->
                Normalizer.normalize(i, Normalizer.Form.NFKC)
                    .equalsIgnoreCase(Normalizer.normalize(need, Normalizer.Form.NFKC)));
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private void notifyMove() {
        for (Plugin p : plugins) {
            p.onPlayerMove();
        }
    }

    private void notifyItemAcquire(String name) {
        for (Plugin p : plugins) {
            p.onItemAcquire(name);
        }
    }

    // GameAPI Implementation
    @Override
    public void teleportTo(int row, int col) {
        if (inBounds(row, col)) {
            player.setPosition(row, col);
            map.revealAround(row, col);
            setMessage(tr("status.teleport", "Teleported to ({0},{1})", row, col));
        }
    }

    @Override
    public int[] getPlayerPosition() {
        return new int[] { player.getRow(), player.getCol() };
    }

    @Override public int getRows() { return map.getRows(); }
    @Override public int getCols() { return map.getCols(); }
    @Override public void reveal(int row, int col) { map.revealAround(row, col); }
    @Override public boolean isVisible(int row, int col) { return map.isVisible(row, col); }
    @Override public boolean isGoalAt(int row, int col) { return row == goalRow && col == goalCol; }

    @Override
    public String getItemAt(int row, int col) {
        Item item = findItemAt(row, col);
        return (item == null) ? null : item.getName();
    }

    @Override
    public List<String> getObstacleRequiresAt(int row, int col) {
        Obstacle obs = getObstacleAt(row, col);
        return (obs == null) ? List.of() : obs.getRequiredItems();
    }

    @Override
    public void addItem(String name) {
        player.addItem(name);
        setMessage(tr("status.acquired", "Acquired {0}.", name));
    }

    @Override
    public List<String> getInventory() {
        return player.getInventory();
    }

    @Override
    public String getMostRecentItem() {
        List<String> inv = player.getInventory();
        return inv.isEmpty() ? null : inv.get(inv.size() - 1);
    }

    @Override
    public void placeObstacle(int r, int c, List<String> requires) {
        obstacles.add(new Obstacle(r, c, requires));
        map.setCell(r, c, GameMap.CellType.OBSTACLE);
        setMessage(tr("status.penalty", "A penalty obstacle appeared!"));
    }

    @Override public Locale getLocale() { return locale; }

    @Override
    public void setLocale(Locale l) {
        this.locale = (l == null) ? Locale.getDefault() : l;
        reloadMessages();
    }

    @Override public long nowMillis() { return System.currentTimeMillis(); }

    
    private Item findItemAt(int r, int c) {
        return items.stream().filter(i -> i.getRow() == r && i.getCol() == c).findFirst().orElse(null);
    }

    private Obstacle getObstacleAt(int r, int c) {
        return obstacles.stream().filter(o -> o.getRow() == r && o.getCol() == c).findFirst().orElse(null);
    }
    
    public void addItemEntity(Item i) {
        items.add(i);
        map.setCell(i.getRow(), i.getCol(), GameMap.CellType.ITEM);
    }

    public void addObstacleEntity(Obstacle o) {
        obstacles.add(o);
        map.setCell(o.getRow(), o.getCol(), GameMap.CellType.OBSTACLE);
    }

    // Message loaders from map files
    
    private void reloadMessages() {
        Properties p = new Properties();
        
        loadInto(p, "/locale/en.properties");
       
        String lang = locale.getLanguage();
        if (lang != null && !lang.isBlank() && !"en".equalsIgnoreCase(lang)) {
            String path = "/locale/" + lang.toLowerCase(Locale.ROOT) + ".properties";
            loadInto(p, path);
        }
        messagesProps = p;
    }

    private static void loadInto(Properties target, String resourcePath) {
        try (InputStream in = GameEngine.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Properties tmp = new Properties();
                tmp.load(r);
                target.putAll(tmp);
            }
        } catch (IOException e) {
            System.out.println("[GameEngine] messages load error: " + e.getMessage());
        }
    }

    // message lookup
    private String tr(String key, String def, Object... args) {
        String pattern = messagesProps.getProperty(key, def);
        MessageFormat mf = new MessageFormat(pattern, locale);
        return mf.format(args);
    }

    private static String sanitize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
