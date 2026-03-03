package io.github.naveenkumar.jmeter.superkey;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.gui.util.MenuFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import javax.swing.tree.TreePath;

public class SuperKeyInjector {
    private static final Logger log = LoggerFactory.getLogger(SuperKeyInjector.class);

    public static void injectComponent(String className) {
        try {
            GuiPackage guiPackage = GuiPackage.getInstance();
            if (guiPackage == null) {
                log.warn("GuiPackage is null, cannot inject component");
                return;
            }

            JMeterTreeNode currentNode = guiPackage.getCurrentNode();
            if (currentNode == null) {
                log.warn("No node currently selected");
                return;
            }

            TestElement testElement = guiPackage.createTestElement(className);
            if (testElement == null) {
                log.error("Failed to create TestElement for class: " + className);
                return;
            }

            JMeterTreeModel treeModel = guiPackage.getTreeModel();

            // Find a valid parent for this test element
            JMeterTreeNode validParentNode = currentNode;
            boolean canAdd = false;

            while (validParentNode != null) {
                if (MenuFactory.canAddTo(validParentNode, testElement)) {
                    canAdd = true;
                    break;
                }
                validParentNode = (JMeterTreeNode) validParentNode.getParent();
            }

            if (!canAdd) {
                log.warn("Cannot add component to the selected node or its parents.");
                JOptionPane.showMessageDialog(guiPackage.getMainFrame(),
                        "Cannot add " + testElement.getName() + " to the current location.",
                        "Invalid Location",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Add to tree
            JMeterTreeNode newNode = treeModel.addComponent(testElement, validParentNode);

            if (newNode == null) {
                log.error(
                        "Could not add component to current node. Parent node might not accept this type of component.");
                return;
            }

            // Apply naming policies
            if (guiPackage.getNamingPolicy() != null) {
                guiPackage.getNamingPolicy().nameOnCreation(newNode);
            }

            // Select the new node
            guiPackage.getMainFrame().getTree().setSelectionPath(new TreePath(newNode.getPath()));

        } catch (Exception e) {
            log.error("Error during component injection", e);
        }
    }
}
