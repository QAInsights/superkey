package io.github.naveenkumar.jmeter.superkey;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

public class SuperKeyDialog extends JDialog {

    private JTextField searchField;
    private JList<ComponentProvider.ComponentItem> resultList;
    private DefaultListModel<ComponentProvider.ComponentItem> listModel;
    private List<ComponentProvider.ComponentItem> allComponents;

    public SuperKeyDialog() {
        super((Frame) null, "Super Key Search", true);
        setUndecorated(true);
        setSize(400, 300);
        setLocationRelativeTo(null);

        allComponents = ComponentProvider.getAllComponents();

        initUI();
        setupListeners();
        filterList("");
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

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

        panel.add(searchField, BorderLayout.NORTH);
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
        String lowerText = text.toLowerCase();
        List<ComponentProvider.ComponentItem> filtered = allComponents.stream()
                .filter(c -> c.name.toLowerCase().contains(lowerText) || c.className.toLowerCase().contains(lowerText))
                .collect(Collectors.toList());

        for (ComponentProvider.ComponentItem item : filtered) {
            listModel.addElement(item);
        }
    }

    private void injectSelected() {
        ComponentProvider.ComponentItem selected = resultList.getSelectedValue();
        if (selected != null) {
            dispose();
            SuperKeyInjector.injectComponent(selected.className);
        }
    }
}
