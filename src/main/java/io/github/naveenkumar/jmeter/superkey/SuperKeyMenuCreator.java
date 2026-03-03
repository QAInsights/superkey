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
import org.apache.jmeter.gui.action.Command;
import org.apache.jmeter.gui.plugin.MenuCreator;
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
            JMenuItem menuItem = new JMenuItem("Super Key", KeyEvent.VK_K);
            menuItem.setName("Super Key");

            int mask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, mask));

            menuItem.setActionCommand(ACTION_CMD);
            menuItem.addActionListener(ActionRouter.getInstance());

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

    private boolean toolbarInitialized = false;

    private void initToolbarButton() {
        if (toolbarInitialized) {
            return;
        }

        try {
            org.apache.jmeter.gui.GuiPackage guiInstance = org.apache.jmeter.gui.GuiPackage.getInstance();
            if (guiInstance != null && guiInstance.getMainToolbar() != null) {
                javax.swing.JToolBar toolbar = guiInstance.getMainToolbar();

                // Use JMeter's core find icon natively
                java.net.URL imageURL = org.apache.jmeter.util.JMeterUtils.class.getClassLoader()
                        .getResource("org/apache/jmeter/images/toolbar/22x22/edit-find-7.png");

                if (imageURL != null) {
                    javax.swing.JButton superKeyButton = new javax.swing.JButton(new javax.swing.ImageIcon(imageURL));
                    superKeyButton.setToolTipText("Super Key (Cmd+K / Ctrl+K)");
                    superKeyButton.addActionListener(org.apache.jmeter.gui.action.ActionRouter.getInstance());
                    superKeyButton.setActionCommand(ACTION_CMD);

                    // Style it flush like other JMeter native buttons
                    superKeyButton.setFocusable(false);
                    superKeyButton.setRolloverEnabled(true);

                    toolbar.addSeparator();
                    toolbar.add(superKeyButton);

                    toolbarInitialized = true;
                }
            }
        } catch (Exception ex) {
            log.error("Failed to add SuperKey button to the JMeter toolbar", ex);
        }
    }

    @Override
    public Set<String> getActionNames() {
        // Ensure toolbar button is added after the UI has been constructed
        javax.swing.SwingUtilities.invokeLater(() -> initToolbarButton());
        return commands;
    }
}
