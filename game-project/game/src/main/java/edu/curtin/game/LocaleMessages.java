package edu.curtin.game;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;


public final class LocaleMessages {
    private Locale locale;
    private Properties props;

    public LocaleMessages(Locale initial) {
        setLocaleInternal(initial == null ? Locale.getDefault() : initial);
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale newLoc) {
        setLocaleInternal(newLoc == null ? Locale.getDefault() : newLoc);
    }

    private void setLocaleInternal(Locale newLoc) {
        this.locale = newLoc;
        this.props = buildPropsFor(newLoc);
    }

    private static Properties buildPropsFor(Locale loc) {
        Properties p = new Properties();

        
        loadInto(p, "/locale/en.properties");

        
        String lang = loc.getLanguage();
        if (lang != null && !lang.isBlank() && !"en".equalsIgnoreCase(lang)) {
            String path = "/locale/" + lang.toLowerCase(Locale.ROOT) + ".properties";
            loadInto(p, path);
        }
        return p;
    }

    private static void loadInto(Properties target, String resourcePath) {
        try (InputStream in = LocaleMessages.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return;
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Properties tmp = new Properties();
                tmp.load(r);
                target.putAll(tmp);
            }
        } catch (IOException e) {
            System.out.println("[LocaleMessages] load error: " + e.getMessage());
        }
    }

    // Translate key
    public String tr(String key, Object... args) {
        String pattern = props.getProperty(key, key);
        MessageFormat mf = new MessageFormat(pattern, locale);
        return mf.format(args);
    }
}
