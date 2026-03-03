package io.github.naveenkumar.jmeter.superkey;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
    private List<ComponentProvider.ComponentItem> allComponents;
    private final Map<String, String> shortcutMap = new HashMap<>();

    public SuperKeyDialog() {
        super((Frame) null, "Super Key Search", true);
        setUndecorated(true);
        setSize(400, 300);
        setLocationRelativeTo(null);

        allComponents = ComponentProvider.getAllComponents();

        loadShortcuts();

        initUI();
        setupListeners();
        filterList("");
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

        JPanel searchPanel = new JPanel(new BorderLayout());

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

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
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(null);

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
            @Override
            public void keyPressed(KeyEvent e) {
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

        // Check if the typed text matches any defined shortcut
        String mappedComponentName = shortcutMap.get(originalLowerText);

        // The effective search text is either the mapped name (if it matches a
        // shortcut) or the original text
        final String searchTarget = (mappedComponentName != null) ? mappedComponentName : originalLowerText;

        List<ComponentProvider.ComponentItem> filtered = allComponents.stream()
                .filter(c -> c.name.toLowerCase().contains(searchTarget)
                        || c.className.toLowerCase().contains(searchTarget))
                .collect(Collectors.toList());

        for (ComponentProvider.ComponentItem item : filtered) {
            listModel.addElement(item);
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
        private static final int ANIMATION_DURATION = 1500; // 1.5 seconds full spin
        private static final int FADE_DURATION = 1000; // 1 second smooth fade out

        public AnimatedBorderPanel() {
            super(new BorderLayout());
            // 4px empty border provides space so child components don't draw over the
            // gradient line
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            // Re-draw animation roughly every 30ms for smooth 30+ fps
            timer = new Timer(30, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > ANIMATION_DURATION + FADE_DURATION) {
                    timer.stop();
                    repaint();
                    return;
                }

                // Increase step from 0.05f to 0.15f for a 3x faster animation spin
                angle += 0.15f;
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

            // Always draw a stable native base border underneath
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRect(0, 0, w - 1, h - 1);

            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > ANIMATION_DURATION + FADE_DURATION) {
                g2d.dispose();
                return;
            }

            float alpha = 1.0f;
            if (elapsed > ANIMATION_DURATION) {
                alpha = 1.0f - ((float) (elapsed - ANIMATION_DURATION) / FADE_DURATION);
                alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            }
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

            float cx = w / 2f;
            float cy = h / 2f;
            // Radius large enough to encircle the bounding box corners completely
            float r = (float) Math.hypot(cx, cy);

            float x1 = cx + (float) (Math.cos(angle) * r);
            float y1 = cy + (float) (Math.sin(angle) * r);
            float x2 = cx + (float) (Math.cos(angle + Math.PI) * r);
            float y2 = cy + (float) (Math.sin(angle + Math.PI) * r);

            // Google AI brand colors
            Color[] colors = {
                    new Color(66, 133, 244), // Blue
                    new Color(234, 67, 53), // Red
                    new Color(251, 188, 5), // Yellow
                    new Color(52, 168, 83), // Green
                    new Color(66, 133, 244) // Blue (for seamless wrap)
            };
            float[] fractions = { 0.0f, 0.25f, 0.5f, 0.75f, 1.0f };

            LinearGradientPaint paint = new LinearGradientPaint(
                    new Point2D.Float(x1, y1),
                    new Point2D.Float(x2, y2),
                    fractions, colors);

            g2d.setPaint(paint);
            // Thick border clipping allows 4px flush inside container rendering
            g2d.setStroke(new BasicStroke(8.0f));
            g2d.drawRect(0, 0, w, h);

            g2d.dispose();
        }
    }
}
