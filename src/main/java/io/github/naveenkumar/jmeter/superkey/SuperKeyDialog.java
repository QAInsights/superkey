package io.github.naveenkumar.jmeter.superkey;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperKeyDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(SuperKeyDialog.class);

    private JTextField searchField;
    private JSpinner countSpinner;
    private JList<ComponentProvider.ComponentItem> resultList;
    private DefaultListModel<ComponentProvider.ComponentItem> listModel;
    private JScrollPane scrollPane;
    private List<ComponentProvider.ComponentItem> allComponents;
    private final Map<String, String> shortcutMap = new HashMap<>();
    private java.awt.Point dragOffset;
    private boolean hasBeenDragged = false;

    // Pro Basket Feature UI
    private JLabel badgeLabel;
    private JPanel bannerContainer;
    private final java.util.Set<ComponentProvider.ComponentItem> globalBasket = new java.util.LinkedHashSet<>();
    private boolean isFiltering = false;
    /** Non-null when a Pro dialog style is active; null in OSS mode. */
    private String activeProStyle = null;

    private static final int ARC = 20;

    public SuperKeyDialog() {
        super((Frame) null, "Super Key Search", true);
        setUndecorated(true);
        setSize(600, 54);
        setLocationRelativeTo(null);
        // Shape is applied by filterList("") → applyProShape() at end of constructor,
        // which respects the active Pro style. Do NOT set it here.

        // Make the dialog draggable
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                hasBeenDragged = true;
                java.awt.Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOffset.x, loc.y + e.getY() - dragOffset.y);
            }
        });

        allComponents = ComponentProvider.getAllComponents();

        loadShortcuts();

        initUI();
        applyProStyleIfEnabled();
        setupListeners();
        filterList("");
    }

    /**
     * If a Pro dialog style is configured (via {@code superkey.dialog.style} in
     * user.properties)
     * and the Pro JAR is present, this method replaces the OSS
     * {@link AnimatedBorderPanel}
     * with a {@code StyledDialogPanel} loaded via reflection.
     *
     * <p>
     * When running in OSS mode (no Pro JAR), or when no style property is
     * configured,
     * this method is a no-op — the dialog is left exactly as initialised by
     * {@link #initUI()}.
     *
     * <p>
     * <b>Important:</b> This method must NEVER import any class from the
     * {@code pro} package.
     * All Pro classes are accessed exclusively via {@link LicenseBridge} and
     * reflection.
     */
    private void applyProStyleIfEnabled() {
        String styleName = LicenseBridge.getDialogStyle();
        if (styleName == null) {
            return; // OSS path — nothing to do
        }

        try {
            // Load DialogStyle enum and resolve the constant
            Class<?> styleEnumClass = Class.forName(
                    "io.github.naveenkumar.jmeter.superkey.pro.DialogStyle");
            Object styleConstant = java.util.Arrays.stream(styleEnumClass.getEnumConstants())
                    .filter(e -> ((Enum<?>) e).name().equals(styleName))
                    .findFirst()
                    .orElse(null);
            if (styleConstant == null) {
                log.warn("SuperKey Pro: unknown dialog style '{}', falling back to OSS style", styleName);
                return;
            }

            // Construct a StyledDialogPanel for the chosen style
            Class<?> panelClass = Class.forName(
                    "io.github.naveenkumar.jmeter.superkey.pro.StyledDialogPanel");
            java.awt.Container styledPanel = (java.awt.Container) panelClass.getConstructor(styleEnumClass)
                    .newInstance(styleConstant);

            // Migrate existing children from the OSS panel into the Pro panel.
            // IMPORTANT: snapshot both the component references AND their BorderLayout
            // constraints BEFORE removing anything — once a component is removed from
            // the layout its constraint can no longer be looked up.
            java.awt.Container ossPanel = (java.awt.Container) getContentPane().getComponent(0);
            java.awt.Component[] children = ossPanel.getComponents();
            String[] constraints = new String[children.length];
            for (int i = 0; i < children.length; i++) {
                constraints[i] = guessConstraint(children[i], ossPanel);
            }
            for (int i = 0; i < children.length; i++) {
                ossPanel.remove(children[i]);
                styledPanel.add(children[i], constraints[i]);
            }

            // Swap the root panel
            getContentPane().remove(ossPanel);
            getContentPane().add(styledPanel);

            // Apply LaF-aware colours so components match the active JMeter theme.
            // We read from UIManager rather than hardcoding so dark themes (Darcula)
            // and light themes (Nimbus, Metal) both look correct.
            Color panelBg = javax.swing.UIManager.getColor("Panel.background");
            Color textFg = javax.swing.UIManager.getColor("TextField.foreground");
            Color textBg = javax.swing.UIManager.getColor("TextField.background");
            Color listBg = javax.swing.UIManager.getColor("List.background");
            Color listFg = javax.swing.UIManager.getColor("List.foreground");
            Color listSelBg = javax.swing.UIManager.getColor("List.selectionBackground");
            Color listSelFg = javax.swing.UIManager.getColor("List.selectionForeground");
            Color caretColor = javax.swing.UIManager.getColor("TextField.caretForeground");
            
            // Fallback defaults with intelligent contrast detection
            if (panelBg == null)
                panelBg = getBackground();
            if (textBg == null)
                textBg = panelBg != null ? panelBg : Color.WHITE;
            
            // Choose text color based on background brightness for proper contrast
            if (textFg == null) {
                textFg = isDarkBackground(textBg) ? Color.WHITE : Color.BLACK;
            }
            
            // Debug logging
            log.debug("SuperKey Pro: panelBg={}, textBg={}, textFg={}", panelBg, textBg, textFg);
            if (listBg == null)
                listBg = panelBg;
            if (listFg == null)
                listFg = textFg;
            if (listSelBg == null)
                listSelBg = new Color(70, 80, 140);
            if (listSelFg == null)
                listSelFg = Color.WHITE;
            if (caretColor == null)
                caretColor = textFg;

            searchField.setForeground(textFg);
            searchField.setCaretColor(caretColor);
            searchField.setSelectionColor(listSelBg);
            searchField.setSelectedTextColor(listSelFg);

            resultList.setBackground(listBg);
            resultList.setForeground(listFg);
            resultList.setSelectionBackground(listSelBg);
            resultList.setSelectionForeground(listSelFg);

            scrollPane.setBackground(listBg);
            scrollPane.getViewport().setBackground(listBg);

            // Spinner and Badge: match the panel theme colours
            // Override UI to force borderless appearance
            countSpinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI());
            countSpinner.setBackground(Color.WHITE);
            countSpinner.setForeground(Color.BLACK);
            countSpinner.setOpaque(true);
            countSpinner.setBorder(BorderFactory.createEmptyBorder());
            
            countSpinner.setEditor(new JSpinner.NumberEditor(countSpinner, "#"));
            
            JComponent spinEditor = countSpinner.getEditor();
            if (spinEditor instanceof JSpinner.DefaultEditor de) {
                de.setBorder(BorderFactory.createEmptyBorder());
                de.setOpaque(true);
                de.setBackground(Color.WHITE);
                
                JTextField spinTextField = de.getTextField();
                spinTextField.setBackground(Color.WHITE);
                spinTextField.setForeground(Color.BLACK);
                spinTextField.setCaretColor(Color.BLACK);
                spinTextField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
                spinTextField.setOpaque(true);
                spinTextField.setHorizontalAlignment(JTextField.CENTER);
                // Clear the auto-selection on focus so no selection highlight
                // rectangle renders at all (keeps the number clearly visible as
                // black-on-white).
                spinTextField.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override
                    public void focusGained(java.awt.event.FocusEvent e) {
                        javax.swing.SwingUtilities.invokeLater(() -> spinTextField.select(0, 0));
                    }
                });
                // Also clear immediately after creation
                javax.swing.SwingUtilities.invokeLater(() -> spinTextField.select(0, 0));
            }
            
            // Make spinner buttons borderless
            for (Component comp : countSpinner.getComponents()) {
                if (comp instanceof JButton button) {
                    button.setBorder(BorderFactory.createEmptyBorder());
                    button.setContentAreaFilled(false);
                    button.setFocusPainted(false);
                }
            }
            if (badgeLabel != null) {
                badgeLabel.setForeground(textFg);
            }

            // Record which Pro style is active so filterList() uses the right shapes
            activeProStyle = styleName;

            // Adjust window shape for the chosen style
            try {
                java.lang.reflect.Method clipMethod = panelClass.getMethod(
                        "getDialogClipShape", int.class, int.class);
                java.awt.Shape clip = (java.awt.Shape) clipMethod.invoke(
                        styledPanel, getWidth(), getHeight());
                setShape(clip); // null = no clip (SHARP rectangular)
            } catch (Exception ex) {
                log.debug("SuperKey Pro: could not set dialog clip shape", ex);
            }

            // Make the dialog and content pane fully transparent so nothing
            // bleeds through outside the StyledDialogPanel's painted region.
            // Without this, the content pane's default background shows as
            // a visible rectangle around the styled shape.
            setBackground(new java.awt.Color(0, 0, 0, 0));
            getContentPane().setBackground(new java.awt.Color(0, 0, 0, 0));
            if (getContentPane() instanceof JComponent jc) {
                jc.setOpaque(false);
            }

            log.info("SuperKey Pro: applied '{}' dialog style", styleName);
        } catch (ClassNotFoundException e) {
            // Pro JAR not on classpath — expected in OSS mode, do nothing
        } catch (Exception e) {
            log.warn("SuperKey Pro: could not apply dialog style '{}', using OSS fallback", styleName, e);
        }
    }

    /**
     * Attempts to determine the {@link java.awt.BorderLayout} constraint string
     * for an existing child component by inspecting its current position in the
     * parent container's layout.
     */
    private String guessConstraint(java.awt.Component child, java.awt.Container parent) {
        if (parent.getLayout() instanceof java.awt.BorderLayout bl) {
            // BorderLayout.getConstraints(Component) is package-private; map via known
            // positions
            if (child == bl.getLayoutComponent(java.awt.BorderLayout.NORTH))
                return java.awt.BorderLayout.NORTH;
            if (child == bl.getLayoutComponent(java.awt.BorderLayout.SOUTH))
                return java.awt.BorderLayout.SOUTH;
            if (child == bl.getLayoutComponent(java.awt.BorderLayout.EAST))
                return java.awt.BorderLayout.EAST;
            if (child == bl.getLayoutComponent(java.awt.BorderLayout.WEST))
                return java.awt.BorderLayout.WEST;
            if (child == bl.getLayoutComponent(java.awt.BorderLayout.CENTER))
                return java.awt.BorderLayout.CENTER;
        }
        return null;
    }

    /**
     * Determines if a background color is dark using relative luminance calculation.
     * Uses the WCAG formula for perceived brightness.
     * 
     * @param bg the background color to test
     * @return true if the background is dark (luminance < 0.5), false otherwise
     */
    private boolean isDarkBackground(Color bg) {
        if (bg == null) {
            return false; // Assume light background if unknown
        }
        
        // Calculate relative luminance using WCAG formula
        // https://www.w3.org/TR/WCAG20/#relativeluminancedef
        double r = bg.getRed() / 255.0;
        double g = bg.getGreen() / 255.0;
        double b = bg.getBlue() / 255.0;
        
        // Apply gamma correction
        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        
        // Calculate luminance
        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        
        // Dark if luminance is less than 0.5 (midpoint)
        return luminance < 0.5;
    }

    private void loadShortcuts() {
        // Properties defined by the user directly in jmeter.properties or
        // user.properties
        Properties jmeterProps = JMeterUtils.getJMeterProperties();

        if (jmeterProps == null) {
            return;
        }

        // We will scan ALL properties.
        // Reason: The user might define `jmeter.superkey.shortcuts=tg, thread group`
        // (Single line map)
        // or they might accidentally define a multi-line value WITHOUT the `\`
        // continuation character:
        // jmeter.superkey.shortcuts=
        // tg, thread group;
        // csv, csv data set config;
        // In this case, Java properties parser treats `tg, thread group;` as an actual
        // KEY with an empty value.
        // So we will look for any property KEY or VALUE that contains a valid shortcut
        // mapping (a comma).

        for (Map.Entry<Object, Object> entry : jmeterProps.entrySet()) {
            String propKey = String.valueOf(entry.getKey()).trim();
            String propVal = String.valueOf(entry.getValue()).trim();

            if (propKey.startsWith("jmeter.superkey.shortcut")) {
                // If it's explicitly defined as a standard property line:
                // e.g. jmeter.superkey.shortcuts=tg, thread group; csv, csv data config
                parseShortcutString(propVal);
            } else if (propKey.contains(",") && propKey.length() < 50) {
                // It's possible the user defined it without a trailing slash,
                // causing standard keys to be parsed as "tg," and value as "thread group;"
                // We combine the key and value with a space to reconstruct the line
                String fullLine = propKey + (propVal.isEmpty() ? "" : " " + propVal);
                if (fullLine.endsWith(";")) {
                    fullLine = fullLine.substring(0, fullLine.length() - 1);
                }
                parseShortcutString(fullLine);
            }
        }
        log.info("SuperKey loaded shortcuts mapping: {}", shortcutMap);
    }

    private void parseShortcutString(String mappedString) {
        if (mappedString != null && !mappedString.trim().isEmpty()) {
            String[] mappings = mappedString.split(";");
            for (String mapping : mappings) {
                String[] parts = mapping.split(",");
                if (parts.length == 2) {
                    String shortcut = parts[0].trim().toLowerCase();
                    String componentName = parts[1].trim().toLowerCase();
                    shortcutMap.put(shortcut, componentName);
                }
            }
        }
    }

    private void initUI() {
        JPanel panel = new AnimatedBorderPanel();

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Load SuperKey icon
        java.net.URL iconURL = getClass().getClassLoader()
                .getResource("io/github/naveenkumar/jmeter/resources/icon.png");
        if (iconURL != null) {
            ImageIcon rawIcon = new ImageIcon(iconURL);
            java.awt.Image scaled = rawIcon.getImage().getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
            JLabel iconLabel = new JLabel(new ImageIcon(scaled));
            iconLabel.setVerticalAlignment(JLabel.CENTER);
            iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
            searchPanel.add(iconLabel, BorderLayout.WEST);
        }

        searchField = new JTextField() {
            private static final String[] PLACEHOLDERS = {
                    "What can I help you test today?",
                    "Your JMeter command center — type anything",
                    "Search smarter, not harder",
                    "Find. Insert. Dominate.",
                    "Need a sampler? An assertion? Just type.",
            };
            private final String PLACEHOLDER = PLACEHOLDERS[new java.util.Random().nextInt(PLACEHOLDERS.length)];

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(100, 100, 100));
                    g2.setFont(getFont());
                    java.awt.Insets ins = getInsets();
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    int y = ins.top + (getHeight() - ins.top - ins.bottom - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(PLACEHOLDER, ins.left + 2, y);
                    g2.dispose();
                }
            }
        };
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.setOpaque(false);
        searchField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Create a custom borderless spinner by overriding paint
        countSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1)) {
            @Override
            public void updateUI() {
                super.updateUI();
                // Force remove border after UI update
                setBorder(BorderFactory.createEmptyBorder());
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // Don't paint any border
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                // Paint with white background, no border
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        countSpinner.setFont(new Font("SansSerif", Font.PLAIN, 14));
        countSpinner.setToolTipText("Number of elements to add");
        countSpinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI());
        countSpinner.setEnabled(true); // Explicitly enable
        countSpinner.setOpaque(true); // Make it opaque
        countSpinner.setBackground(Color.WHITE); // White background
        countSpinner.setForeground(Color.BLACK); // Black text
        countSpinner.setPreferredSize(new java.awt.Dimension(50, 28));
        countSpinner.setBorder(BorderFactory.createEmptyBorder());
        
        countSpinner.setEditor(new JSpinner.NumberEditor(countSpinner, "#"));
        
        JComponent editor = countSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.setBorder(BorderFactory.createEmptyBorder());
            de.setOpaque(true);
            de.setBackground(Color.WHITE);
            JTextField textField = de.getTextField();
            textField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            textField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            textField.setOpaque(true);
            textField.setBackground(Color.WHITE);
            textField.setForeground(Color.BLACK);
            textField.setHorizontalAlignment(JTextField.CENTER);
            // Clear the auto-selection on focus so no selection highlight
            // rectangle renders (keeps the number clearly visible).
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    javax.swing.SwingUtilities.invokeLater(() -> textField.select(0, 0));
                }
            });
            // Clear immediately after creation
            javax.swing.SwingUtilities.invokeLater(() -> textField.select(0, 0));
        }
        
        // Style the spinner buttons
        for (Component comp : countSpinner.getComponents()) {
            if (comp instanceof JButton button) {
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
            }
        }

        // Create a panel to hold the search field with the spinner inside it
        JPanel searchFieldPanel = new JPanel(new BorderLayout(4, 0));
        searchFieldPanel.setOpaque(false);
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        searchFieldPanel.add(countSpinner, BorderLayout.EAST);

        searchPanel.add(searchFieldPanel, BorderLayout.CENTER);

        // Badge label for multi-selection (Pro feature)
        badgeLabel = new JLabel("");
        badgeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        badgeLabel.setForeground(new Color(150, 150, 150));
        badgeLabel.setVisible(false); // Only visible when >1 items selected
        
        searchPanel.add(badgeLabel, BorderLayout.EAST);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                ((javax.swing.JComponent) c).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return c;
            }
        });

        scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(null);
        scrollPane.setVisible(false);

        bannerContainer = new JPanel(new BorderLayout());
        bannerContainer.setOpaque(false);
        bannerContainer.setVisible(false);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bannerContainer, BorderLayout.SOUTH);

        getContentPane().add(panel);
    }

    private void setupListeners() {
        KeyAdapter closeAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                }
            }
        };

        searchField.addKeyListener(closeAdapter);
        resultList.addKeyListener(closeAdapter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterList(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterList(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterList(searchField.getText());
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            // Konami code sequence: ↑↑↓↓←→←→BA
            private static final int[] KONAMI = {
                    KeyEvent.VK_UP, KeyEvent.VK_UP,
                    KeyEvent.VK_DOWN, KeyEvent.VK_DOWN,
                    KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                    KeyEvent.VK_B, KeyEvent.VK_A
            };
            private int konamiIdx = 0;
            
            // Git command history navigation
            private List<String> gitHistory = null;
            private int historyIndex = -1;
            private String currentInput = "";

            @Override
            public void keyPressed(KeyEvent e) {
                String text = searchField.getText().trim();
                
                // Handle UP arrow for Git command history
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    // Only handle history if we're in Git command mode
                    if (text.startsWith("git ") || text.equals("git") || historyIndex >= 0) {
                        if (LicenseBridge.isPro()) {
                            handleGitHistoryNavigation(true);
                            return; // Don't process other UP arrow logic
                        }
                    }
                }
                
                // Handle DOWN arrow for Git command history
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Only handle history if we're navigating history
                    if (historyIndex >= 0) {
                        if (LicenseBridge.isPro()) {
                            handleGitHistoryNavigation(false);
                            return; // Don't process other DOWN arrow logic
                        }
                    }
                }
                
                // Track Konami code
                if (e.getKeyCode() == KONAMI[konamiIdx]) {
                    konamiIdx++;
                    if (konamiIdx == KONAMI.length) {
                        konamiIdx = 0;
                        EasterEggHandler.showKonamiConfetti(SuperKeyDialog.this);
                        return;
                    }
                } else {
                    konamiIdx = 0;
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (listModel.getSize() > 0) {
                        resultList.setSelectedIndex(0);
                        resultList.requestFocus();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Reset history navigation on enter
                    historyIndex = -1;
                    gitHistory = null;
                    
                    // Check if this is a direct Git command (not a suggestion)
                    if (text.startsWith("git ") && LicenseBridge.isPro()) {
                        // Execute Git command directly
                        dispose();
                        handleGitCommand(text);
                        return;
                    }
                    
                    if (listModel.getSize() > 0) {
                        resultList.setSelectedIndex(0);
                        injectSelected();
                    }
                }
            }
            
            /**
             * Handle Git command history navigation with UP/DOWN arrows.
             * 
             * @param navigateUp true for UP arrow (older commands), false for DOWN arrow (newer commands)
             */
            private void handleGitHistoryNavigation(boolean navigateUp) {
                try {
                    // Load history on first navigation
                    if (gitHistory == null) {
                        Class<?> handlerClass = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitCommandHandler");
                        Object handler = handlerClass.getDeclaredConstructor().newInstance();
                        java.lang.reflect.Method getHistoryMethod = handlerClass.getMethod("getCommandHistory");
                        @SuppressWarnings("unchecked")
                        List<String> history = (List<String>) getHistoryMethod.invoke(handler);
                        
                        if (history == null || history.isEmpty()) {
                            return; // No history available
                        }
                        
                        gitHistory = history;
                        historyIndex = -1;
                        currentInput = searchField.getText();
                    }
                    
                    if (navigateUp) {
                        // Navigate to older commands (UP arrow)
                        if (historyIndex < gitHistory.size() - 1) {
                            historyIndex++;
                            searchField.setText(gitHistory.get(historyIndex));
                        }
                    } else {
                        // Navigate to newer commands (DOWN arrow)
                        if (historyIndex > 0) {
                            historyIndex--;
                            searchField.setText(gitHistory.get(historyIndex));
                        } else if (historyIndex == 0) {
                            // Return to original input
                            historyIndex = -1;
                            searchField.setText(currentInput);
                            gitHistory = null; // Reset for next navigation
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Could not load Git command history", ex);
                }
            }
        });

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    injectSelected();
                } else if (e.getKeyCode() == KeyEvent.VK_UP && resultList.getSelectedIndex() == 0) {
                    searchField.requestFocus();
                }
            }
        });

        resultList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    injectSelected();
                }
            }
        });

        resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !isFiltering) {
                List<ComponentProvider.ComponentItem> visibleSelected = resultList.getSelectedValuesList();
                for (int i = 0; i < listModel.size(); i++) {
                    ComponentProvider.ComponentItem item = listModel.getElementAt(i);
                    if (visibleSelected.contains(item)) {
                        globalBasket.add(item);
                    } else {
                        globalBasket.remove(item);
                    }
                }
                updateBasketUI();
            }
        });
    }

    private void updateBasketUI() {
        int count = globalBasket.size();

        if (count == 0) {
            countSpinner.setEnabled(true);
            badgeLabel.setVisible(false);
            if (bannerContainer.isVisible()) {
                bannerContainer.setVisible(false);
                bannerContainer.removeAll();
                refreshLayout();
            }
            return;
        }

        // Disable spinner if ANY selected item is an action (actions don't honor
        // counts)
        boolean hasAction = globalBasket.stream().anyMatch(item -> item.isAction);
        countSpinner.setEnabled(!hasAction);

        if (count > 1) {
            if (LicenseBridge.isPro()) {
                badgeLabel.setText(count + " items selected");
                badgeLabel.setVisible(true);
                if (bannerContainer.isVisible()) {
                    bannerContainer.setVisible(false);
                    bannerContainer.removeAll();
                    refreshLayout();
                }
            } else {
                badgeLabel.setVisible(false);
                if (!bannerContainer.isVisible()) {
                    bannerContainer.removeAll();
                    bannerContainer.add(LicenseBridge.getUpgradeBanner("Multiple Element Selection"));
                    bannerContainer.setVisible(true);
                    refreshLayout();
                }
            }
        } else {
            // count == 1
            badgeLabel.setVisible(false);
            if (bannerContainer.isVisible()) {
                bannerContainer.setVisible(false);
                bannerContainer.removeAll();
                refreshLayout();
            }
        }
    }

    private void refreshLayout() {
        if (!scrollPane.isVisible())
            return; // Don't expand if collapsed
        int shadowPad = "FLOATING_SHADOW".equals(activeProStyle) ? 24 : 0;
        int expandedW = 600 + shadowPad;
        int expandedH = 300 + shadowPad;
        int finalH = bannerContainer.isVisible() ? expandedH + bannerContainer.getPreferredSize().height : expandedH;
        setSize(expandedW, finalH);
        applyProShape(expandedW, finalH);
        revalidate();
        repaint();
    }

    private void filterList(String text) {
        isFiltering = true;
        listModel.clear();
        String originalLowerText = text.toLowerCase().trim();

        // FLOATING_SHADOW needs extra pixels for the shadow to render outside
        // the content area. For other styles the padding is zero.
        int shadowPad = "FLOATING_SHADOW".equals(activeProStyle) ? 24 : 0; // 2 × SHADOW_PAD(12)
        int collapsedW = 600 + shadowPad;
        int collapsedH = 54 + shadowPad;

        if (originalLowerText.isEmpty()) {
            scrollPane.setVisible(false);
            setSize(collapsedW, collapsedH);
            applyProShape(collapsedW, collapsedH);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
            isFiltering = false;
            return;
        }

        // --- Git Command Check (complete command with space) ---
        if (originalLowerText.startsWith("git ") && originalLowerText.length() > 4) {
            
            // ALWAYS show suggestions for Git commands while typing
            // Execution happens only when user presses Enter
            showGitCommandSuggestions(originalLowerText);
            isFiltering = false;
            return;
        }

        // --- Git Command Autocomplete (typing "git" without space or partial) ---
        if (originalLowerText.startsWith("git") && !originalLowerText.startsWith("git ") && LicenseBridge.isPro()) {
            showGitCommandSuggestions(originalLowerText);
            isFiltering = false;
            return;
        }

        // --- Easter Egg Check (exact match only, before normal search) ---
        if (EasterEggHandler.check(originalLowerText, this)) {
            // hide results, don't pollute search
            scrollPane.setVisible(false);
            setSize(collapsedW, collapsedH);
            applyProShape(collapsedW, collapsedH);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
            // Cannot call setText() directly from inside a DocumentListener notification
            // — that causes "Attempt to mutate in notification". Defer it.
            javax.swing.SwingUtilities.invokeLater(() -> searchField.setText(""));
            isFiltering = false;
            return;
        }

        // Check if the typed text matches any defined shortcut
        String mappedComponentName = shortcutMap.get(originalLowerText);

        // Search with BOTH the original text AND the mapped shortcut name
        // so natural matches (e.g. "HTTP Request" for "http") always appear
        List<ComponentProvider.ComponentItem> filtered = allComponents.stream()
                .filter(c -> {
                    String nameLower = c.name.toLowerCase();
                    String classLower = c.className.toLowerCase();
                    boolean matchesOriginal = nameLower.contains(originalLowerText)
                            || classLower.contains(originalLowerText);
                    boolean matchesMapped = mappedComponentName != null
                            && (nameLower.contains(mappedComponentName) || classLower.contains(mappedComponentName));
                    return matchesOriginal || matchesMapped;
                })
                .collect(Collectors.toList());

        for (ComponentProvider.ComponentItem item : filtered) {
            listModel.addElement(item);
        }

        // Restore selection state in resultList for items in globalBasket
        List<Integer> selectedIndices = new java.util.ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            if (globalBasket.contains(listModel.getElementAt(i))) {
                selectedIndices.add(i);
            }
        }
        if (!selectedIndices.isEmpty()) {
            int[] indices = selectedIndices.stream().mapToInt(Integer::intValue).toArray();
            resultList.setSelectedIndices(indices);
        }

        if (!filtered.isEmpty()) {
            scrollPane.setVisible(true);
            refreshLayout();
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
        } else {
            scrollPane.setVisible(false);
            setSize(collapsedW, collapsedH);
            applyProShape(collapsedW, collapsedH);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
        }
        isFiltering = false;
    }

    /**
     * Show Git command suggestions when user types "git".
     * Requirements: 26.5, 27.1
     */
    private void showGitCommandSuggestions(String partialText) {
        String gitPart = "";
        if (partialText.startsWith("git ") && partialText.length() > 4) {
            gitPart = partialText.substring(4).trim();
        }
        
        // Context-specific suggestions based on what user is typing
        String[] suggestions = null;
        
        if (gitPart.equals("branch") || gitPart.startsWith("branch ")) {
            suggestions = new String[] {
                "git branch - List all branches",
                "git branch <name> - Create new branch",
                "git branch -d <name> - Delete branch",
                "git branch -D <name> - Force delete branch",
                "git branch -m <old> <new> - Rename branch"
            };
        } else if (gitPart.equals("tag") || gitPart.startsWith("tag ")) {
            suggestions = new String[] {
                "git tag - List all tags",
                "git tag <name> - Create lightweight tag",
                "git tag -a <name> -m \"msg\" - Create annotated tag",
                "git tag -d <name> - Delete tag"
            };
        } else if (gitPart.equals("stash") || gitPart.startsWith("stash ")) {
            suggestions = new String[] {
                "git stash - Stash current changes",
                "git stash list - List all stashes",
                "git stash pop - Apply and remove latest stash",
                "git stash apply - Apply latest stash (keep it)",
                "git stash drop - Remove latest stash",
                "git stash clear - Remove all stashes"
            };
        } else if (gitPart.equals("remote") || gitPart.startsWith("remote ")) {
            suggestions = new String[] {
                "git remote - List remotes",
                "git remote -v - List remotes with URLs",
                "git remote add <name> <url> - Add remote",
                "git remote remove <name> - Remove remote",
                "git remote show <name> - Show remote details"
            };
        } else if (gitPart.equals("checkout") || gitPart.startsWith("checkout ")) {
            suggestions = new String[] {
                "git checkout <branch> - Switch to branch",
                "git checkout -b <name> - Create and switch to new branch",
                "git checkout <file> - Discard changes in file"
            };
        } else if (gitPart.equals("merge") || gitPart.startsWith("merge ")) {
            suggestions = new String[] {
                "git merge <branch> - Merge branch into current",
                "git merge --abort - Abort merge in progress"
            };
        } else if (gitPart.equals("reset") || gitPart.startsWith("reset ")) {
            suggestions = new String[] {
                "git reset - Unstage all files (mixed)",
                "git reset <file> - Unstage specific file",
                "git reset --soft <commit> - Move HEAD, keep changes staged",
                "git reset --mixed <commit> - Move HEAD, unstage changes",
                "git reset --hard <commit> - Move HEAD, discard all changes"
            };
        } else if (gitPart.equals("push") || gitPart.startsWith("push ")) {
            suggestions = new String[] {
                "git push - Push to default remote/branch",
                "git push <remote> <branch> - Push to specific remote/branch",
                "git push -u origin <branch> - Push and set upstream"
            };
        } else if (gitPart.equals("pull") || gitPart.startsWith("pull ")) {
            suggestions = new String[] {
                "git pull - Pull from default remote/branch",
                "git pull <remote> <branch> - Pull from specific remote/branch"
            };
        } else if (gitPart.equals("fetch") || gitPart.startsWith("fetch ")) {
            suggestions = new String[] {
                "git fetch - Fetch from default remote",
                "git fetch <remote> - Fetch from specific remote",
                "git fetch --all - Fetch from all remotes"
            };
        } else if (gitPart.equals("commit") || gitPart.startsWith("commit ")) {
            suggestions = new String[] {
                "git commit -m \"message\" - Create a commit",
                "git commit -am \"message\" - Stage all and commit",
                "git commit --amend - Amend last commit"
            };
        } else if (gitPart.equals("add") || gitPart.startsWith("add ")) {
            suggestions = new String[] {
                "git add . - Stage all changes",
                "git add <file> - Stage specific file",
                "git add -A - Stage all (including deletions)"
            };
        } else if (gitPart.equals("restore") || gitPart.startsWith("restore ")) {
            suggestions = new String[] {
                "git restore <file> - Discard changes in file",
                "git restore --staged <file> - Unstage file",
                "git restore . - Discard all changes"
            };
        } else if (gitPart.equals("config") || gitPart.startsWith("config ")) {
            suggestions = new String[] {
                "git config user.name \"Name\" - Set user name",
                "git config user.email \"email\" - Set user email",
                "git config --list - List all config"
            };
        } else if (gitPart.equals("clone") || gitPart.startsWith("clone ")) {
            suggestions = new String[] {
                "git clone <url> - Clone repository",
                "git clone <url> -b <branch> - Clone specific branch"
            };
        } else if (gitPart.equals("blame") || gitPart.startsWith("blame ")) {
            suggestions = new String[] {
                "git blame <file> - Show who changed each line"
            };
        } else if (gitPart.equals("grep") || gitPart.startsWith("grep ")) {
            suggestions = new String[] {
                "git grep <pattern> - Search for pattern in tracked files"
            };
        } else if (gitPart.equals("pop")) {
            // Special case: "git pop" is not valid, suggest correct command
            suggestions = new String[] {
                "git stash pop - Apply and remove latest stash",
                "Note: Use 'git stash pop' not 'git pop'"
            };
        } else {
            // Generic Git commands when just typing "git"
            suggestions = new String[] {
                "git init - Initialize a new repository",
                "git status - Show working tree status",
                "git add . - Stage all changes",
                "git commit -m \"msg\" - Create a commit",
                "git log - View commit history",
                "git diff - Show unstaged changes",
                "git branch - List/manage branches",
                "git checkout <branch> - Switch branches",
                "git merge <branch> - Merge branches",
                "git push - Push to remote",
                "git pull - Pull from remote",
                "git stash - Stash changes",
                "git tag - Manage tags",
                "git remote - Manage remotes"
            };
        }
        
        // Add suggestions to list
        listModel.clear();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().contains(gitPart.toLowerCase()) || gitPart.isEmpty() || gitPart.equals("git")) {
                ComponentProvider.ComponentItem item = new ComponentProvider.ComponentItem(
                    suggestion, 
                    "git-command", 
                    true  // Mark as action so it doesn't try to inject as component
                );
                listModel.addElement(item);
            }
        }
        
        if (listModel.getSize() > 0) {
            scrollPane.setVisible(true);
            refreshLayout();
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
        } else {
            // No matches - collapse
            int shadowPad = "FLOATING_SHADOW".equals(activeProStyle) ? 24 : 0;
            scrollPane.setVisible(false);
            setSize(600 + shadowPad, 54 + shadowPad);
            applyProShape(600 + shadowPad, 54 + shadowPad);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
        }
    }

    /**
     * Handle Git command execution.
     * Requirements: 1.1, 1.3, 23.1, 23.5, 26.1, 26.2, 26.3
     */
    private void handleGitCommand(String commandText) {
        // Check Pro license first
        if (!LicenseBridge.isPro()) {
            // Show Pro upgrade banner
            scrollPane.setVisible(false);
            bannerContainer.removeAll();
            bannerContainer.add(LicenseBridge.getUpgradeBanner("Git Commands"));
            bannerContainer.setVisible(true);
            
            int shadowPad = "FLOATING_SHADOW".equals(activeProStyle) ? 24 : 0;
            int w = 600 + shadowPad;
            int h = 54 + shadowPad + bannerContainer.getPreferredSize().height;
            setSize(w, h);
            applyProShape(w, h);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
            return;
        }

        // Pro license active - execute Git command
        try {
            // Load Git classes via reflection to avoid compile-time dependency
            Class<?> parserClass = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitCommandParser");
            Class<?> handlerClass = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitCommandHandler");
            Class<?> commandClass = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitCommand");
            Class<?> exceptionClass = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitParseException");
            
            // Create parser and parse command
            Object parser = parserClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method parseMethod = parserClass.getMethod("parse", String.class);
            Object gitCommand = parseMethod.invoke(parser, commandText);
            
            if (gitCommand == null) {
                showGitError("Not a valid Git command");
                return;
            }
            
            // Close dialog before executing command
            dispose();
            
            // Create handler and execute async
            Object handler = handlerClass.getDeclaredConstructor().newInstance();
            
            // Create callback using reflection
            Class<?> callbackInterface = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitResultCallback");
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { callbackInterface },
                (proxy, method, args) -> {
                    if ("onSuccess".equals(method.getName())) {
                        String result = (String) args[0];
                        showGitResult(result, true);
                    } else if ("onError".equals(method.getName())) {
                        String error = (String) args[0];
                        showGitResult(error, false);
                    }
                    return null;
                }
            );
            
            // Execute async with command string for history tracking
            java.lang.reflect.Method executeMethod = handlerClass.getMethod("executeAsync", String.class, commandClass, callbackInterface);
            executeMethod.invoke(handler, commandText, gitCommand, callback);
            
        } catch (ClassNotFoundException e) {
            // Git classes not found - shouldn't happen if Pro license is active
            log.error("Git command classes not found", e);
            showGitError("Git command support not available");
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null && cause.getClass().getSimpleName().equals("GitParseException")) {
                showGitError(cause.getMessage());
            } else {
                log.error("Error executing Git command", e);
                showGitError("Error: " + (cause != null ? cause.getMessage() : e.getMessage()));
            }
        } catch (Exception e) {
            log.error("Error handling Git command", e);
            showGitError("Error: " + e.getMessage());
        }
    }

    /**
     * Show Git command result in a dialog.
     * Requirements: 23.1, 23.5
     */
    private void showGitResult(String message, boolean success) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // Try to use Pro GitResultDialog if available
                Class<?> dialogClass = Class.forName("io.github.naveenkumar.jmeter.superkey.pro.git.GitResultDialog");
                Object resultDialog = dialogClass.getDeclaredConstructor().newInstance();
                
                if (success) {
                    java.lang.reflect.Method showSuccessMethod = dialogClass.getMethod("showSuccess", String.class);
                    showSuccessMethod.invoke(resultDialog, message);
                } else {
                    java.lang.reflect.Method showErrorMethod = dialogClass.getMethod("showError", String.class);
                    showErrorMethod.invoke(resultDialog, message);
                }
            } catch (ClassNotFoundException e) {
                // Fallback to compact JOptionPane
                showCompactGitMessage(message, success);
            } catch (Exception e) {
                log.error("Error showing Git result", e);
                showCompactGitMessage(message, success);
            }
        });
    }

    /**
     * Show a compact Git message using JOptionPane.
     */
    private void showCompactGitMessage(String message, boolean success) {
        // Create a compact dialog
        JDialog dialog = new JDialog((Frame) null, success ? "Git Success" : "Git Error", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Calculate size based on message length
        int lines = message.split("\n").length;
        int width = Math.min(500, Math.max(300, message.length() * 6));
        int height = Math.min(400, Math.max(150, lines * 20 + 100));
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Status label
        JLabel statusLabel = new JLabel(success ? "✓ Success" : "✗ Error");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(success ? new Color(34, 139, 34) : new Color(204, 0, 0));
        panel.add(statusLabel, BorderLayout.NORTH);
        
        // Message area
        JTextArea textArea = new JTextArea(message);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setCaretPosition(0);
        
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add keyboard navigation
        // ESC or Tab to close
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Set close button as default button (Enter key activates it)
        dialog.getRootPane().setDefaultButton(closeButton);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    /**
     * Show Git command error inline in the dialog.
     */
    private void showGitError(String error) {
        scrollPane.setVisible(false);
        bannerContainer.removeAll();
        
        JLabel errorLabel = new JLabel("<html><div style='padding: 10px; color: #cc0000;'>" + error + "</div></html>");
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        bannerContainer.add(errorLabel);
        bannerContainer.setVisible(true);
        
        int shadowPad = "FLOATING_SHADOW".equals(activeProStyle) ? 24 : 0;
        int w = 600 + shadowPad;
        int h = 54 + shadowPad + bannerContainer.getPreferredSize().height;
        setSize(w, h);
        applyProShape(w, h);
        if (!hasBeenDragged)
            setLocationRelativeTo(null);
        
        // Auto-hide error after 3 seconds
        Timer timer = new Timer(3000, e -> {
            bannerContainer.setVisible(false);
            bannerContainer.removeAll();
            int collapsedH = 54 + shadowPad;
            setSize(w, collapsedH);
            applyProShape(w, collapsedH);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Applies the correct window shape / clip for the current state.
     * In OSS mode (activeProStyle == null) uses the standard ARC=20 rounded rect.
     * In Pro mode uses the style-appropriate shape so setShape() is never
     * overwritten with incompatible values during filterList() resize calls.
     */
    private void applyProShape(int w, int h) {
        if (activeProStyle == null) {
            // OSS default
            setShape(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));
            return;
        }
        switch (activeProStyle) {
            case "SHARP" ->
                setShape(null); // rectangular — no clip needed
            case "PILL" -> {
                // Collapsed: true pill (arc = height). Expanded: softly rounded rect
                // so the results list doesn't become a 300-px-tall oval.
                int arc = (h <= 60) ? h : 24;
                setShape(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));
            }
            case "FLOATING_SHADOW" ->
                // Larger than bounds so shadow padding shows through
                setShape(null);
            default ->
                setShape(new RoundRectangle2D.Double(0, 0, w, h, ARC, ARC));
        }
    }

    private void injectSelected() {
        List<ComponentProvider.ComponentItem> selectedItems = new java.util.ArrayList<>(globalBasket);

        if (selectedItems.isEmpty()) {
            return;
        }

        if (selectedItems.size() > 1 && !LicenseBridge.isPro()) {
            // OSS users cannot inject multiple elements
            return;
        }

        // Check if this is a Git command suggestion
        if (!selectedItems.isEmpty() && "git-command".equals(selectedItems.get(0).className)) {
            // Extract the actual command from the display text (before the " - " description)
            String displayText = selectedItems.get(0).name;
            String gitCommand = displayText.split(" - ")[0].trim();
            
            // Close dialog and execute the Git command
            dispose();
            handleGitCommand(gitCommand);
            return;
        }

        dispose();

        int count = (Integer) countSpinner.getValue();

        for (ComponentProvider.ComponentItem selected : selectedItems) {
            if (selected.isAction) {
                // Execute JMeter GUI Action (once per action selected, ignoring spinner count)
                ActionRouter.getInstance().doActionNow(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected.className));
            } else {
                // Execute standard component insertion
                SuperKeyInjector.injectComponent(selected.className, count);
            }
        }
    }

    private class AnimatedBorderPanel extends JPanel {
        private float angle = 0;
        private Timer timer;
        private long startTime;

        public AnimatedBorderPanel() {
            super(new BorderLayout());
            // 4px empty border provides space so child components don't draw over the
            // gradient line
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            // Re-draw animation roughly every 30ms for smooth 30+ fps
            timer = new Timer(30, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > GradientBorderPainter.ANIM_DURATION + GradientBorderPainter.ANIM_FADE) {
                    timer.stop();
                    repaint();
                    return;
                }

                angle += GradientBorderPainter.ANGLE_STEP;
                if (angle > Math.PI * 2) {
                    angle -= Math.PI * 2;
                }
                repaint();
            });
        }

        @Override
        public void addNotify() {
            super.addNotify();
            startTime = System.currentTimeMillis();
            timer.start();
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            timer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Fill background with rounded rect
            g2d.setColor(getBackground());
            g2d.fillRoundRect(0, 0, w, h, ARC, ARC);

            // Always draw a stable base border
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

            // Animated gradient overlay — delegated to shared utility
            long elapsed = System.currentTimeMillis() - startTime;
            float alpha = GradientBorderPainter.computeAlpha(elapsed);
            if (alpha > 0f) {
                GradientBorderPainter.paint(g2d, angle, alpha,
                        2, 2, w - 4, h - 4, ARC);
            }

            g2d.dispose();
        }
    }
}
