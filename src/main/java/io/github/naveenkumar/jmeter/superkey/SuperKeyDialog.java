package io.github.naveenkumar.jmeter.superkey;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
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
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

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
            int count = (Integer) countSpinner.getValue();
            SuperKeyInjector.injectComponent(selected.className, count);
        }
    }
}
