package io.github.naveenkumar.jmeter.superkey;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.apache.jmeter.gui.action.AbstractAction;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.plugin.MenuCreator;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperKeyMenuCreator extends AbstractAction implements MenuCreator {
    private static final Logger log = LoggerFactory.getLogger(SuperKeyMenuCreator.class);
    public static final String ACTION_CMD = "super_key_action";
    private static final Set<String> commands = new HashSet<>();

    static {
        commands.add(ACTION_CMD);
    }

    public SuperKeyMenuCreator() {
        super();
    }

    @Override
    public JMenuItem[] getMenuItemsAtLocation(MENU_LOCATION location) {
        if (location == MENU_LOCATION.SEARCH) {
            JMenuItem menuItem = new JMenuItem("Super Key");
            menuItem.setName("Super Key");

            // Look for user-defined shortcut property
            String customShortcut = JMeterUtils.getProperty("jmeter.superkey.custom");
            KeyStroke keyStroke = null;
            String shortcutText = "Cmd+K / Ctrl+K"; // Default tooltip

            if (customShortcut != null && !customShortcut.trim().isEmpty()) {
                try {
                    // Java's KeyStroke expects "control COMMA" or "ctrl K"
                    // User might provide "ctrl+," or "cmd+shift+y"
                    String parsedConfig = customShortcut.toLowerCase()
                            .replace("+", " ")
                            .replace(",", "COMMA");

                    // Intelligent OS Smart-Swap
                    // If the user configured "cmd" but is on Windows/Linux, swap to "ctrl"
                    // If the user configured "ctrl" but is on macOS, swap to "cmd" (meta)
                    boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
                    boolean isLinux = System.getProperty("os.name").toLowerCase().contains("linux");
                    if (isMac) {
                        parsedConfig = parsedConfig.replace("ctrl", "meta").replace("cmd", "meta").replace("control",
                                "meta");
                    } else if (isLinux) {
                        parsedConfig = parsedConfig.replace("cmd", "ctrl").replace("meta", "ctrl");
                    } else {
                        parsedConfig = parsedConfig.replace("cmd", "ctrl").replace("meta", "ctrl");
                    }

                    keyStroke = KeyStroke.getKeyStroke(parsedConfig);
                    if (keyStroke != null) {
                        shortcutText = customShortcut;
                        log.info("Registered custom SuperKey shortcut: {}", customShortcut);
                    } else {
                        log.warn("Failed to parse custom SuperKey shortcut: '{}'. Falling back to default.",
                                customShortcut);
                    }
                } catch (Exception ex) {
                    log.warn("Error parsing custom SuperKey shortcut: '{}'. Falling back to default.", customShortcut,
                            ex);
                }
            }

            // Fallback to default (Ctrl+K / Cmd+K)
            if (keyStroke == null) {
                int mask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, mask);
            }

            menuItem.setAccelerator(keyStroke);
            menuItem.setActionCommand(ACTION_CMD);
            menuItem.addActionListener(ActionRouter.getInstance());

            // Save shortcut tooltips for toolbar injection
            menuItem.putClientProperty("shortcutText", shortcutText);

            return new JMenuItem[] { menuItem };
        }
        return new JMenuItem[0];
    }

    @Override
    public JMenu[] getTopLevelMenus() {
        return new JMenu[0];
    }

    @Override
    public boolean localeChanged(javax.swing.MenuElement menu) {
        return false;
    }

    @Override
    public void localeChanged() {
    }

    @Override
    public void doAction(ActionEvent e) {
        log.info("Super Key Action triggered!");
        SuperKeyDialog dialog = new SuperKeyDialog();
        dialog.setVisible(true);
    }

    private void scheduleToolbarInjection() {
        java.util.Timer timer = new java.util.Timer("SuperKey-Toolbar-Injector-Timer", true);
        timer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    org.apache.jmeter.gui.GuiPackage guiInstance = org.apache.jmeter.gui.GuiPackage.getInstance();
                    if (guiInstance != null && guiInstance.getMainToolbar() != null) {
                        // Found it! Execute injection on the EDT
                        javax.swing.SwingUtilities.invokeLater(() -> injectToolbarButton(guiInstance.getMainToolbar()));
                        timer.cancel(); // Stop polling
                    }
                } catch (Exception e) {
                    // Ignore, GUI package might not be fully initialized yet
                }
            }
        }, 500, 500); // Check every 500ms
    }

    private void injectToolbarButton(javax.swing.JToolBar toolbar) {
        try {
            // Check if button already exists to prevent duplicates
            for (java.awt.Component comp : toolbar.getComponents()) {
                if (comp instanceof javax.swing.JButton) {
                    if (ACTION_CMD.equals(((javax.swing.JButton) comp).getActionCommand())) {
                        return; // Already initialized
                    }
                }
            }

            String iconSize = org.apache.jmeter.util.JMeterUtils.getPropDefault("jmeter.toolbar.icons.size", "22x22");
            String sizePrefix = iconSize.split("x")[0];

            java.net.URL imageURL = SuperKeyMenuCreator.class.getClassLoader()
                    .getResource("io/github/naveenkumar/jmeter/resources/" + sizePrefix + ".png");

            if (imageURL != null) {
                javax.swing.JButton superKeyButton = new javax.swing.JButton(new javax.swing.ImageIcon(imageURL));

                // Get the mapped shortcut dynamically from the instantiated menu items to
                // determine tooltip text
                String tooltip = "Super Key (Cmd+K / Ctrl+K)";
                JMenuItem[] searchMenus = getMenuItemsAtLocation(MENU_LOCATION.SEARCH);
                if (searchMenus != null && searchMenus.length > 0) {
                    Object parsedTooltip = searchMenus[0].getClientProperty("shortcutText");
                    if (parsedTooltip != null) {
                        tooltip = "Super Key (" + parsedTooltip + ")";
                    }
                }

                superKeyButton.setToolTipText(tooltip);
                superKeyButton.addActionListener(org.apache.jmeter.gui.action.ActionRouter.getInstance());
                superKeyButton.setActionCommand(ACTION_CMD);

                // Style it flush like other JMeter native buttons
                superKeyButton.setFocusable(false);
                superKeyButton.setRolloverEnabled(true);

                // Find the index of the "start" button to insert before it
                int insertIndex = toolbar.getComponentCount();
                for (int i = 0; i < toolbar.getComponentCount(); i++) {
                    java.awt.Component comp = toolbar.getComponent(i);
                    if (comp instanceof javax.swing.JButton) {
                        String cmd = ((javax.swing.JButton) comp).getActionCommand();
                        // JMeter's standard start action commands
                        if ("start".equals(cmd) || "test_start".equals(cmd) || "start_no_timers".equals(cmd)) {
                            insertIndex = i;
                            // Optionally, if there's a separator right before the start button,
                            // we might want to insert before the separator.
                            if (i > 0 && toolbar.getComponent(i - 1) instanceof javax.swing.JToolBar.Separator) {
                                insertIndex = i - 1;
                            }
                            break;
                        }
                    }
                }

                if (insertIndex < toolbar.getComponentCount()) {
                    toolbar.add(superKeyButton, insertIndex);
                    // Optionally push a separator after it to group it nicely
                    // toolbar.add(new javax.swing.JToolBar.Separator(), insertIndex + 1);
                } else {
                    toolbar.addSeparator();
                    toolbar.add(superKeyButton);
                }

                toolbar.revalidate();
                toolbar.repaint();
                log.info("Super Key search button successfully injected into JMeter toolbar before run button.");
            }
        } catch (Exception ex) {
            log.error("Failed to add SuperKey button to the JMeter toolbar", ex);
        }
    }

    @Override
    public Set<String> getActionNames() {
        scheduleToolbarInjection();
        return commands;
    }
}
