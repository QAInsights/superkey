package io.github.naveenkumar.jmeter.superkey;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.jmeter.gui.util.MenuFactory;

public class ComponentProvider {

    private static List<ComponentItem> cache;

    public static class ComponentItem {
        public final String name;
        public final String className;

        public ComponentItem(String name, String className) {
            this.name = name;
            this.className = className;
        }

        @Override
        public String toString() {
            return name;
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

        // Distinct by classname
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
