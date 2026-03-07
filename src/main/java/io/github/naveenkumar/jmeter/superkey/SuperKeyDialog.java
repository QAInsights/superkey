package io.github.naveenkumar.jmeter.superkey;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
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
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
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
    /** Non-null when a Pro dialog style is active; null in OSS mode. */
    private String activeProStyle = null;
    
    // Hotkey configuration
    private int hotkeyModifierMask;
    private String hotkeyModifierText; // For display, e.g. "Alt"
    private boolean showHotkeys;

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
        loadHotkeyConfig();

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
            if (panelBg == null)
                panelBg = getBackground();
            if (textFg == null)
                textFg = Color.BLACK;
            if (textBg == null)
                textBg = Color.WHITE;
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

            // Spinner: match the panel theme colours
            countSpinner.setBackground(panelBg);
            countSpinner.setForeground(textFg);
            Color sepColor = javax.swing.UIManager.getColor("Separator.foreground");
            countSpinner.setBorder(BorderFactory.createLineBorder(
                    sepColor != null ? sepColor : Color.GRAY));
            JComponent spinEditor = countSpinner.getEditor();
            if (spinEditor instanceof JSpinner.DefaultEditor de) {
                de.getTextField().setBackground(panelBg);
                de.getTextField().setForeground(textFg);
                de.getTextField().setCaretColor(caretColor);
                de.getTextField().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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

    private void loadHotkeyConfig() {
        String modProp = JMeterUtils.getPropDefault("jmeter.superkey.hotkey.modifier", "Alt");
        showHotkeys = JMeterUtils.getPropDefault("jmeter.superkey.hotkey.show", true);

        // Parse modifier
        hotkeyModifierText = modProp; // Default text
        if ("Ctrl".equalsIgnoreCase(modProp) || "Control".equalsIgnoreCase(modProp)) {
            hotkeyModifierMask = InputEvent.CTRL_DOWN_MASK;
            hotkeyModifierText = "Ctrl";
        } else if ("Shift".equalsIgnoreCase(modProp)) {
            hotkeyModifierMask = InputEvent.SHIFT_DOWN_MASK;
            hotkeyModifierText = "Shift";
        } else if ("Meta".equalsIgnoreCase(modProp) || "Cmd".equalsIgnoreCase(modProp)) {
            hotkeyModifierMask = InputEvent.META_DOWN_MASK;
            hotkeyModifierText = "Cmd";
        } else {
            hotkeyModifierMask = InputEvent.ALT_DOWN_MASK;
            hotkeyModifierText = "Alt";
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

        countSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        countSpinner.setFont(new Font("SansSerif", Font.PLAIN, 16));
        countSpinner.setToolTipText("Number of elements to add");
        JComponent editor = countSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setFont(new Font("SansSerif", Font.PLAIN, 16));
            ((JSpinner.DefaultEditor) editor).getTextField().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(countSpinner, BorderLayout.EAST);

        listModel = new DefaultListModel<>();
        resultList = new JList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                ((javax.swing.JComponent) c).setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                if (showHotkeys && index < 10 && c instanceof JLabel) {
                    int num = (index + 1) % 10; // 1..9, then 0
                    String text = ((JLabel) c).getText();
                    // Prepend hotkey in a subtle way (e.g. [Alt+1])
                    ((JLabel) c).setText("<html><b style='color:gray'>[" + hotkeyModifierText + "+" + num + "]</b>&nbsp;&nbsp;" + text + "</html>");
                }
                return c;
            }
        });

        scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(null);
        scrollPane.setVisible(false);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

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

            @Override
            public void keyPressed(KeyEvent e) {
                // Hotkey Logic (Modifier + 0-9)
                if ((e.getModifiersEx() & hotkeyModifierMask) == hotkeyModifierMask) {
                    int idx = -1;
                    if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9) {
                        idx = e.getKeyCode() - KeyEvent.VK_1; // 0 to 8
                    } else if (e.getKeyCode() == KeyEvent.VK_0) {
                        idx = 9;
                    }

                    if (idx != -1) {
                        if (idx < listModel.getSize()) {
                            resultList.setSelectedIndex(idx);
                            injectSelected();
                        }
                        e.consume();
                        return;
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
                    if (listModel.getSize() > 0) {
                        resultList.setSelectedIndex(0);
                        injectSelected();
                    }
                }
            }
        });

        resultList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Hotkey Logic (Modifier + 0-9)
                if ((e.getModifiersEx() & hotkeyModifierMask) == hotkeyModifierMask) {
                    int idx = -1;
                    if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9) {
                        idx = e.getKeyCode() - KeyEvent.VK_1; // 0 to 8
                    } else if (e.getKeyCode() == KeyEvent.VK_0) {
                        idx = 9;
                    }

                    if (idx != -1) {
                        if (idx < listModel.getSize()) {
                            resultList.setSelectedIndex(idx);
                            injectSelected();
                        }
                        e.consume();
                        return;
                    }
                }

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
            if (!e.getValueIsAdjusting()) {
                ComponentProvider.ComponentItem selectedItem = resultList.getSelectedValue();
                if (selectedItem != null) {
                    countSpinner.setEnabled(!selectedItem.isAction);
                }
            }
        });
    }

    private void filterList(String text) {
        listModel.clear();
        String originalLowerText = text.toLowerCase().trim();

        // FLOATING_SHADOW needs extra pixels for the shadow to render outside
        // the content area. For other styles the padding is zero.
        int shadowPad = "FLOATING_SHADOW".equals(activeProStyle) ? 24 : 0; // 2 × SHADOW_PAD(12)
        int collapsedW = 600 + shadowPad;
        int collapsedH = 54 + shadowPad;
        int expandedW = 600 + shadowPad;
        int expandedH = 300 + shadowPad;

        if (originalLowerText.isEmpty()) {
            scrollPane.setVisible(false);
            setSize(collapsedW, collapsedH);
            applyProShape(collapsedW, collapsedH);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
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

        if (!filtered.isEmpty()) {
            scrollPane.setVisible(true);
            setSize(expandedW, expandedH);
            applyProShape(expandedW, expandedH);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
        } else {
            scrollPane.setVisible(false);
            setSize(collapsedW, collapsedH);
            applyProShape(collapsedW, collapsedH);
            if (!hasBeenDragged)
                setLocationRelativeTo(null);
        }
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
        ComponentProvider.ComponentItem selected = resultList.getSelectedValue();
        if (selected != null) {
            dispose();

            if (selected.isAction) {
                // Execute JMeter GUI Action
                ActionRouter.getInstance().doActionNow(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, selected.className));
            } else {
                // Execute standard component insertion
                int count = (Integer) countSpinner.getValue();
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
