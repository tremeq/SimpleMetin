package me.simplemetin.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps user files up to date with the defaults bundled in the jar: missing keys are added together with
 * their comments, existing values are never touched. Paper keeps the user's own comments when saving.
 */
public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    /** Opens a resource bundled in the plugin jar (works without a running server, e.g. in tests). */
    public static InputStream resource(String name) {
        return ConfigUpdater.class.getClassLoader().getResourceAsStream(name);
    }

    public static YamlConfiguration loadResource(String name) {
        var in = resource(name);
        if (in == null) return new YamlConfiguration();
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException e) {
            return new YamlConfiguration();
        }
    }

    /** Copies the bundled resource to the file if the file does not exist yet. Returns true if it was created. */
    public static boolean saveIfMissing(String resourceName, File file) throws IOException {
        if (file.exists()) return false;
        var in = resource(resourceName);
        if (in == null) throw new IOException("Missing bundled resource " + resourceName);
        file.getParentFile().mkdirs();
        try (in) {
            Files.copy(in, file.toPath());
        }
        return true;
    }

    /**
     * Adds keys from the bundled resource that are missing in the file.
     *
     * @param userSections sections whose children belong to the user (e.g. "crystals"): if the section exists
     *                     it is left alone, otherwise it is added with the default content
     * @return the added paths (empty if the file was already complete)
     */
    public static List<String> update(File file, String resourceName, Set<String> userSections) throws IOException {
        var defaults = loadResource(resourceName);
        // loadConfiguration silently returns an empty config on malformed YAML; saving that would destroy edits.
        var user = new YamlConfiguration();
        if (file.exists()) {
            try {
                user.load(file);
            } catch (InvalidConfigurationException e) {
                throw new IOException("Invalid YAML in " + file.getName() + "; file left unchanged", e);
            }
        }
        var added = new ArrayList<String>();
        var addedUserRoots = new HashSet<String>();

        for (var path : defaults.getKeys(true)) {
            if (user.contains(path, true)) continue;
            if (hasScalarParent(user, path)) continue;

            var root = userRoot(path, userSections);
            if (root != null && !root.equals(path) && !addedUserRoots.contains(root)) continue;
            if (root != null && root.equals(path)) addedUserRoots.add(root);

            if (defaults.isConfigurationSection(path)) {
                user.createSection(path);
            } else {
                user.set(path, defaults.get(path));
            }
            user.setComments(path, defaults.getComments(path));
            user.setInlineComments(path, defaults.getInlineComments(path));

            // Report only top-most additions, not every child of a new section
            if (added.stream().noneMatch(a -> path.startsWith(a + "."))) {
                added.add(path);
            }
        }

        if (!added.isEmpty()) {
            user.save(file);
        }
        return added;
    }

    private static boolean hasScalarParent(YamlConfiguration user, String path) {
        for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1)) {
            var parent = path.substring(0, dot);
            if (user.contains(parent, true) && !user.isConfigurationSection(parent)) return true;
        }
        return false;
    }

    private static String userRoot(String path, Set<String> userSections) {
        for (var section : userSections) {
            if (path.equals(section) || path.startsWith(section + ".")) return section;
        }
        return null;
    }
}
