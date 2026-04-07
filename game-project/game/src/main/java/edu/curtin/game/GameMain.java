package edu.curtin.game;

import edu.curtin.api.Plugin;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

         /** Main UI */
public class GameMain {
    private static final int TILE_SIZE = 32;

    private JFrame frame;
    private JPanel pluginBar;
    private JMenu pluginsMenu;
    private JMenu gameMenu;
    private JMenuItem changeLocaleItem;
    private JLabel dateLabel;
    private JLabel invLabel;
    private JLabel statusLabel;

    private GridArea gridArea;
    private GameMap map;
    private Player player;
    private GameEngine engine;

    
    private List<Plugin> loadedPlugins = new ArrayList<>();

    // Base images
    private Image playerImg, fogImg, goalImg, obstacleImg, itemImgGeneric;

    
    private final Map<String, Image> itemImgs = new HashMap<>();

    // Inventory 
    private final DefaultListModel<String> invModel = new DefaultListModel<>();
    private final JList<String> invList = new JList<>(invModel);

    // Internationalisation & date
    private LocaleMessages msgs;
    private LocalDate gameDate = LocalDate.now();
    private DateTimeFormatter dateFmt;
    private int moves = 0; 

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: ./gradlew :game:run --args= <mapfile>");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
            Locale chosen = pickStartupLocale();
            new GameMain(args[0], chosen);
        });
    }

    // startup dialog  
    private static Locale pickStartupLocale() {
        Locale sys = Locale.getDefault();
        String sysTag = sys.toLanguageTag();
        String sysLabel = "System Default (" + sysTag + ")";

        String[] choices = new String[] { sysLabel, "English (en)", "Español (es)" };
        JComboBox<String> combo = new JComboBox<>(choices);
        JTextField custom = new JTextField();
        custom.setEnabled(false);

        combo.addItemListener(ev -> {
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                String sel = (String) ev.getItem();
                boolean enable = "Custom…".equals(sel);
                custom.setEnabled(enable);
            }
        });

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JLabel("Choose language / locale (IETF tag):"), BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(combo, BorderLayout.NORTH);
        center.add(custom, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(
            null, panel, "Language / Locale", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (res != JOptionPane.OK_OPTION) {
            return sys;
        }

        String sel = (String) combo.getSelectedItem();
        if (sel == null) {
            return sys;
        }
        if ("English (en)".equals(sel)) {
            return Locale.forLanguageTag("en");
        }
        if ("Español (es)".equals(sel)) {
            return Locale.forLanguageTag("es");
        }
        return sys;
    }

    public GameMain(String filename, Locale initialLocale) {
        
        msgs = new LocaleMessages(initialLocale);
        dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(msgs.getLocale());

        try (Reader reader = new InputStreamReader(new FileInputStream(filename), detectEncoding(filename))) {
            // Parse
            GameParser parser = new GameParser(reader);
            Map<?, ?> config = parser.Input();

            
            int rows     = getInt(config.get("rows"));
            int cols     = getInt(config.get("cols"));
            int startRow = getInt(config.get("startRow"));
            int startCol = getInt(config.get("startCol"));
            int goalRow  = getInt(config.get("goalRow"));
            int goalCol  = getInt(config.get("goalCol"));

            // Engine
            map = new GameMap(rows, cols);
            player = new Player(startRow, startCol);
            map.revealAround(player.getRow(), player.getCol());
            engine = new GameEngine(map, player, goalRow, goalCol);
            engine.setLocale(msgs.getLocale());
            map.setCell(goalRow, goalCol, GameMap.CellType.GOAL);

            // Items 
            Object itemsObj = config.get("items");
            if (itemsObj instanceof List<?> rawItems) {
                for (Object itemObj : rawItems) {
                    if (itemObj instanceof Map<?, ?> m) {
                        String name = getString(m.get("name"));
                        String message = getStringOrNull(m.get("message"));

                        
                        List<int[]> positions = new ArrayList<>();
                        Object posObj = m.get("positions");
                        if (posObj instanceof List<?> rawPos) {
                            for (Object p : rawPos) {
                                if (p instanceof int[] a && a.length >= 2) {
                                    positions.add(new int[]{a[0], a[1]});
                                } else if (p instanceof List<?> two
                                        && two.size() >= 2
                                        && two.get(0) instanceof Number n0
                                        && two.get(1) instanceof Number n1) {
                                    positions.add(new int[]{n0.intValue(), n1.intValue()});
                                }
                            }
                        }

                        for (int[] pos : positions) {
                            engine.addItemEntity(new Item(name, pos[0], pos[1], message));
                        }
                    }
                }
            }

            // Obstacles
            Object obsObj = config.get("obstacles");
            if (obsObj instanceof List<?> rawObs) {
                for (Object ob : rawObs) {
                    if (ob instanceof Map<?, ?> m) {
                        
                        List<int[]> positions = new ArrayList<>();
                        Object posObj = m.get("positions");
                        if (posObj instanceof List<?> rawPos) {
                            for (Object p : rawPos) {
                                if (p instanceof int[] a && a.length >= 2) {
                                    positions.add(new int[]{a[0], a[1]});
                                } else if (p instanceof List<?> two
                                        && two.size() >= 2
                                        && two.get(0) instanceof Number n0
                                        && two.get(1) instanceof Number n1) {
                                    positions.add(new int[]{n0.intValue(), n1.intValue()});
                                }
                            }
                        }

                       
                        List<String> req = new ArrayList<>();
                        Object reqObj = m.get("requiredItems");
                        if (reqObj instanceof List<?> rawReq) {
                            for (Object r : rawReq) {
                                if (r instanceof String s) {
                                    req.add(s);
                                }
                            }
                        }

                        for (int[] pos : positions) {
                            engine.addObstacleEntity(new Obstacle(pos[0], pos[1], req));
                        }
                    }
                }
            }

            // Plugins
            Object plugObj = config.get("plugins");
            if (plugObj instanceof List<?> rawPlugs) {
                List<String> pluginNames = new ArrayList<>();
                for (Object p : rawPlugs) {
                    if (p instanceof String s) {
                        pluginNames.add(s);
                    }
                }
                List<Plugin> plugins = PluginLoader.loadPlugins(pluginNames);
                engine.setPlugins(plugins);
                loadedPlugins = plugins; 
            }


            // GUI
            loadImages();               
            setupGUI(rows, cols);
            buildPluginUI();            
            bindKeys();                
            refreshDisplay();           

        } catch (IOException | ParseException e) {
            System.err.println("Error loading map: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Error loading map: " + e.getMessage());
        }
    }

    private static int getInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Expected number, got: " + o);
    }

    private static String getString(Object o) {
        if (o instanceof String s) {
            return s;
        }
        throw new IllegalArgumentException("Expected string, got: " + o);
    }

    private static String getStringOrNull(Object o) {
        return (o instanceof String s) ? s : null;
    }

    // ---------------- GUI Setup ----------------
    private void setupGUI(int rows, int cols) {
        gridArea = new GridArea(cols, rows);
        frame = new JFrame(msgs.tr("app.title"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(gridArea, BorderLayout.CENTER);

        // Menu bar
        JMenuBar mb = new JMenuBar();

        gameMenu = new JMenu(msgs.tr("menu.game"));
        changeLocaleItem = new JMenuItem(msgs.tr("menu.changeLocale"));
        changeLocaleItem.addActionListener(e -> promptForLocale());
        gameMenu.add(changeLocaleItem);
        mb.add(gameMenu);

        pluginsMenu = new JMenu(msgs.tr("menu.plugins")); 
        mb.add(pluginsMenu);

        frame.setJMenuBar(mb);

        // Top bar with date 
        JPanel top = new JPanel(new BorderLayout());
        dateLabel = new JLabel();
        updateDateLabel();
        top.add(dateLabel, BorderLayout.WEST);
        frame.add(top, BorderLayout.NORTH);

        // inventory panel
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(200, 0));
        invLabel = new JLabel(msgs.tr("label.inventory"));
        side.add(invLabel, BorderLayout.NORTH);
        invList.setVisibleRowCount(16);
        side.add(new JScrollPane(invList), BorderLayout.CENTER);
        frame.add(side, BorderLayout.EAST);

        
        JPanel south = new JPanel(new BorderLayout());

        
        pluginBar = new JPanel();
        pluginBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        south.add(pluginBar, BorderLayout.CENTER);

        // status bar 
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        statusLabel = new JLabel(msgs.tr("label.statusReady", "Ready."));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        south.add(statusPanel, BorderLayout.SOUTH);

        frame.add(south, BorderLayout.SOUTH);

        frame.setSize(900, 720);
        frame.setVisible(true);

        frame.getRootPane().requestFocusInWindow();
    }

    // Plugin UI
    private void buildPluginUI() {
        // Buttons
        pluginBar.removeAll();
        for (Plugin p : loadedPlugins) {
            if (p != null && p.hasButton()) {
                String label = p.getClass().getSimpleName();
                JButton btn = new JButton(label);
                btn.setFocusable(false);
                btn.addActionListener(ev -> {
                    p.onPluginInvoked();
                    refreshDisplay();
                    frame.getRootPane().requestFocusInWindow(); 
                });
                pluginBar.add(btn);
            }
        }
        pluginBar.revalidate();
        pluginBar.repaint();

        // Menu
        pluginsMenu.removeAll();
        for (Plugin p : loadedPlugins) {
            if (p != null && p.hasButton()) {
                final Plugin pluginRef = p;
                String label = p.getClass().getSimpleName();
                JMenuItem mi = new JMenuItem(label + " (T)");
                mi.addActionListener(ev -> {
                    pluginRef.onPluginInvoked();
                    refreshDisplay();
                    frame.getRootPane().requestFocusInWindow();
                });
                pluginsMenu.add(mi);
            }
        }
        pluginsMenu.revalidate();
        pluginsMenu.repaint();
    }

    // ---------- Key bindings ----------
    private void bindKeys() {
        JComponent root = frame.getRootPane();
        bindAction(root, "moveUp",    KeyEvent.VK_UP,    () -> moveAndCheck("up"));
        bindAction(root, "moveDown",  KeyEvent.VK_DOWN,  () -> moveAndCheck("down"));
        bindAction(root, "moveLeft",  KeyEvent.VK_LEFT,  () -> moveAndCheck("left"));
        bindAction(root, "moveRight", KeyEvent.VK_RIGHT, () -> moveAndCheck("right"));

        //'T' to invoke Teleport 
        bindAction(root, "teleportHotkey", KeyEvent.VK_T, this::invokeTeleportIfPresent);
    }

    private void bindAction(JComponent comp, String name, int keyCode, Runnable r) {
        comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(keyCode, 0), name);
        comp.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    private void moveAndCheck(String dir) {
        int[] before = engine.getPlayerPosition();
        engine.movePlayer(dir);
        int[] after = engine.getPlayerPosition();

        // Count a "day"  if the player moved to a new square
        if (after[0] != before[0] || after[1] != before[1]) {
            moves++;
            gameDate = gameDate.plusDays(1);
            updateDateLabel();
        }

        refreshDisplay();
        if (engine.isGoalAt(player.getRow(), player.getCol())) {
            JOptionPane.showMessageDialog(
                frame,
                msgs.tr("msg.goalReached", moves),
                msgs.tr("app.title"),
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    // trigger Teleport 
    private void invokeTeleportIfPresent() {
        for (Plugin p : loadedPlugins) {
            if (p != null && p.hasButton() && p.getClass().getName().endsWith(".Teleport")) {
                p.onPluginInvoked();
                refreshDisplay();
                break;
            }
        }
        frame.getRootPane().requestFocusInWindow();
    }

    // Locale switching
    private void promptForLocale() {
        String current = msgs.getLocale().toLanguageTag();
        String tag = JOptionPane.showInputDialog(
            frame,
            msgs.tr("prompt.locale", current),
            msgs.tr("menu.changeLocale"),
            JOptionPane.QUESTION_MESSAGE
        );
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }

        Locale newLoc = Locale.forLanguageTag(tag.trim());
        msgs.setLocale(newLoc);
        engine.setLocale(newLoc);
        dateFmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(newLoc);

        frame.setTitle(msgs.tr("app.title"));
        pluginsMenu.setText(msgs.tr("menu.plugins"));
        gameMenu.setText(msgs.tr("menu.game"));
        changeLocaleItem.setText(msgs.tr("menu.changeLocale"));
        invLabel.setText(msgs.tr("label.inventory"));
        updateDateLabel();
        buildPluginUI();
        refreshDisplay();
    }

    private void updateDateLabel() {
        dateLabel.setText(msgs.tr("label.date", dateFmt.format(gameDate)));
    }

    // Images
    private void loadImages() {
        playerImg       = loadOrFallback("player.png",   Color.BLUE);
        obstacleImg     = loadOrFallback("obstacle.png", Color.RED);
        goalImg         = loadOrFallback("goal.png",     Color.GREEN);
        itemImgGeneric  = loadOrFallback("item.png",     Color.YELLOW);
        fogImg          = solid(Color.DARK_GRAY); 
    }


    private Image getItemImageFor(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return itemImgGeneric;
        }
        String key = sanitize(itemName);
        Image cached = itemImgs.get(key);
        if (cached != null) {
            return cached;
        }

        String fileName = "item_" + key + ".png"; 
        Image img = null;
        try {
            img = loadPng(fileName);
        } catch (IOException ignored) {
            
        }
        if (img == null) {
            img = itemImgGeneric;
        }
        itemImgs.put(key, img);
        return img;
    }

    private static String sanitize(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    private Image loadOrFallback(String name, Color fallbackColor) {
        try {
            Image img = loadPng(name);
            if (img != null) {
                return img;
            }
        } catch (IOException ignored) {
            
        }
        return solid(fallbackColor);
    }

    private Image loadPng(String name) throws IOException {
        URL url = getClass().getResource("/images/" + name);
        if (url == null) {
            File f = new File(name);
            if (f.exists()) {
                return scaleToTile(ImageIO.read(f));
            }
            return null;
        }
        return scaleToTile(ImageIO.read(url));
    }

    private Image scaleToTile(Image src) {
        if (src == null) {
            return null;
        }
        return src.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
    }

    // Fallback if the image does not load
    private Image solid(Color color) {
        BufferedImage img = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.dispose();
        return img;
    }

    private void refreshDisplay() {
        gridArea.getIcons().clear();
        for (int r = 0; r < map.getRows(); r++) {
            for (int c = 0; c < map.getCols(); c++) {
                if (!map.isVisible(r, c)) {
                    gridArea.getIcons().add(new GridAreaIcon(c, r, 0, 1.0, fogImg, ""));
                    continue;
                }

                GameMap.CellType type = map.getCell(r, c);
                switch (type) {
                    case ITEM: {
                        String itemName = engine.getItemAt(r, c);
                        Image icon = getItemImageFor(itemName);
                        gridArea.getIcons().add(
                            new GridAreaIcon(c, r, 0, 1.0, icon, (itemName == null ? "Item" : itemName)));
                        break;
                    }
                    case OBSTACLE: {
                        gridArea.getIcons().add(new GridAreaIcon(c, r, 0, 1.0, obstacleImg, "Block"));
                        break;
                    }
                    case GOAL: {
                        gridArea.getIcons().add(new GridAreaIcon(c, r, 0, 1.0, goalImg, "Goal"));
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
        gridArea.getIcons().add(new GridAreaIcon(player.getCol(), player.getRow(), 0, 1.0, playerImg, "You"));
        gridArea.repaint();

        refreshInventory();

        // update status text 
        String msg = engine.getLastMessage();
        statusLabel.setText(
            (msg == null || msg.isBlank())
                ? msgs.tr("label.statusReady", "Ready.")
                : msg
        );
    }

    // Inventory list
    private void refreshInventory() {
        invModel.clear();
        for (String s : engine.getInventory()) {
            invModel.addElement(s);
        }
    }

    // Encoding Detection
    private static String detectEncoding(String filename) {
        if (filename.endsWith(".utf16.map")) {
            return "UTF-16";
        }
        if (filename.endsWith(".utf32.map")) {
            return "UTF-32";
        }
        return "UTF-8";
    }
}
