package edu.curtin.game;

import edu.curtin.api.Plugin;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class PluginLoader {
    public static List<Plugin> loadPlugins(List<String> names) {
        List<Plugin> result = new ArrayList<>();
        for (String cls : names) {
            try {
                Class<?> c = Class.forName(cls);
                Plugin p = (Plugin) c.getDeclaredConstructor().newInstance();
                result.add(p);
                System.out.println("[PluginLoader] Loaded " + cls);
            } catch (ClassNotFoundException
                     | NoSuchMethodException
                     | InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException e) {
                System.out.println("[PluginLoader] Failed to load: " + cls + " — " + e.getMessage());
            }
        }
        return result;
    }
}
