package io.github.naveenkumar.jmeter.superkey;

import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.util.MenuFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentProvider {
    private static final Logger log = LoggerFactory.getLogger(ComponentProvider.class);

    private static final Set<String> IGNORED_ACTIONS = new HashSet<>(Arrays.asList(
            "add", "add_all", "addParent", "change_language", "laf", "Change Parent",
            "check_dirty", "check_remove", "check_cut",
            "duplicate", "remove", "insert_after", "insert_before", "drag_n_drop", "change_parent"));

    private static List<ComponentItem> cache;

    public static class ComponentItem {
        public final String name;
        public final String className;
        public final boolean isAction;

        public ComponentItem(String name, String className) {
            this(name, className, false);
        }

        public ComponentItem(String name, String className, boolean isAction) {
            this.name = name;
            this.className = className;
            this.isAction = isAction;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ComponentItem that = (ComponentItem) o;
            return java.util.Objects.equals(className, that.className);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(className);
        }
    }

    public static synchronized List<ComponentItem> getAllComponents() {
        if (cache != null) {
            return cache;
        }

        List<ComponentItem> items = new ArrayList<>();
        String[] categories = {
                MenuFactory.SAMPLERS,
                MenuFactory.CONTROLLERS,
                MenuFactory.CONFIG_ELEMENTS,
                MenuFactory.TIMERS,
                MenuFactory.PRE_PROCESSORS,
                MenuFactory.POST_PROCESSORS,
                MenuFactory.ASSERTIONS,
                MenuFactory.LISTENERS,
                MenuFactory.THREADS
        };

        for (String cat : categories) {
            JMenu menu = MenuFactory.makeMenu(cat, "Add");
            extractItems(menu, items);
        }

        // Reflection to dynamically add all ActionNames
        try {
            Field[] fields = ActionNames.class.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers())
                        && field.getType() == String.class) {
                    try {
                        String actionCommand = (String) field.get(null);

                        if (IGNORED_ACTIONS.contains(actionCommand) || actionCommand == null
                                || actionCommand.trim().isEmpty()) {
                            continue;
                        }

                        // Convert "test_start" or "zoom_in" to "Test Start" or "Zoom In"
                        String humanReadableName = Arrays.stream(actionCommand.split("_"))
                                .map(word -> word.isEmpty() ? ""
                                        : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                                .collect(Collectors.joining(" "));

                        items.add(new ComponentItem("Action: " + humanReadableName, actionCommand, true));
                    } catch (IllegalAccessException e) {
                        // ignore
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load JMeter ActionNames reflectively", e);
        }

        // Distinct by classname/actionCommand
        cache = items.stream()
                .collect(Collectors.toMap(
                        c -> c.className,
                        c -> c,
                        (existing, replacement) -> existing))
                .values()
                .stream()
                .sorted((a, b) -> a.name.compareToIgnoreCase(b.name))
                .collect(Collectors.toList());

        return cache;
    }

    private static void extractItems(Component comp, List<ComponentItem> result) {
        if (comp instanceof JMenuItem && !(comp instanceof JMenu) && !(comp instanceof JPopupMenu)) {
            JMenuItem item = (JMenuItem) comp;
            String text = item.getText();
            String className = item.getName(); // MenuFactory sets className as the name of JMenuItem
            if (className != null && !className.trim().isEmpty() && text != null && !text.trim().isEmpty()) {
                result.add(new ComponentItem(text, className));
            }
        }

        if (comp instanceof JMenu) {
            for (Component child : ((JMenu) comp).getMenuComponents()) {
                extractItems(child, result);
            }
        } else if (comp instanceof JPopupMenu) {
            for (Component child : ((JPopupMenu) comp).getComponents()) {
                extractItems(child, result);
            }
        }
    }
}
