// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.maxal.dialogResizer;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.EmptyClipboardOwner;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class DialogResizerAction extends ToggleAction implements DumbAware {

    private static final String WIDTH_KEY = "DialogResizer_Width";
    private static final String HEIGHT_KEY = "DialogResizer_Height";
    private DialogResizer myDialogResizer;
    private Boolean myUseRealSize = false;

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return myDialogResizer != null;
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (state) {
            DialogResizerWindow dialogResizerWindow = new DialogResizerWindow(false);
            boolean isOK = dialogResizerWindow.showAndGet();
            if (isOK) {
                setMyHeight(Integer.valueOf(dialogResizerWindow.myHeightTextField.getText()));
                setMyWidth(Integer.valueOf(dialogResizerWindow.myWidthTextField.getText()));
                myUseRealSize = dialogResizerWindow.useRealSizeCheckbox.isSelected();
                if (myDialogResizer == null) {
                    myDialogResizer = new DialogResizer();
                }
                Notifications.Bus.notify(new Notification("Resizer", "Dialog Resizer", "Control-Shift-Click to resize the component.\n Alt-Shift-Click to capture screenshot.",
                        NotificationType.INFORMATION, null));
            }
        } else {
            myDialogResizer = null;
        }
    }

    private Integer getMyHeight() {
        int savedInt = PropertiesComponent.getInstance().getInt(HEIGHT_KEY, -1);
        return savedInt == -1 ? null : savedInt;
    }

    private void setMyHeight(Integer myHeight) {
        PropertiesComponent.getInstance().setValue(HEIGHT_KEY, myHeight, -1);
    }

    private Integer getMyWidth() {
        int savedInt = PropertiesComponent.getInstance().getInt(WIDTH_KEY, -1);
        return savedInt == -1 ? null : savedInt;
    }

    private void setMyWidth(Integer myWidth) {
        PropertiesComponent.getInstance().setValue(WIDTH_KEY, myWidth, -1);
    }

    private class DialogResizerWindow extends DialogWrapper {

        private JBTextField myHeightTextField;
        private JBTextField myWidthTextField;
        private JCheckBox useRealSizeCheckbox;

        DialogResizerWindow(boolean canBeParent) {
            super(canBeParent);
            setTitle("Required Size");
            init();
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());

            JPanel firstLine = new JPanel(new BorderLayout());
            firstLine.add(new JBLabel("Width:  "), BorderLayout.WEST);
            myWidthTextField = new JBTextField(1);
            if (getMyWidth() != null) myWidthTextField.setText(getMyWidth().toString());
            firstLine.add(myWidthTextField, BorderLayout.CENTER);
            panel.add(firstLine, BorderLayout.NORTH);

            JPanel secondLine = new JPanel(new BorderLayout());
            secondLine.add(new JBLabel("Height: "), BorderLayout.WEST);
            myHeightTextField = new JBTextField(1);
            if (getMyHeight() != null) myHeightTextField.setText(getMyHeight().toString());
            secondLine.add(myHeightTextField, BorderLayout.CENTER);
            panel.add(secondLine, BorderLayout.CENTER);

            JPanel thirdLine = new JPanel(new BorderLayout());
            useRealSizeCheckbox = new JCheckBox("Size without OS toolbar", false);
            useRealSizeCheckbox.setSelected(myUseRealSize);
            thirdLine.add(useRealSizeCheckbox, BorderLayout.WEST);
            panel.add(thirdLine, BorderLayout.SOUTH);

            return panel;
        }
    }

    private static void copyImageToClipboard(Component comp) {
        int w = comp.getWidth();
        int h = comp.getHeight();
        BufferedImage bi = UIUtil.createImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        comp.paint(g);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(bi), EmptyClipboardOwner.INSTANCE);
    }

    private static class ImageTransferable implements Transferable {
        private final BufferedImage myImage;

        ImageTransferable(@NotNull BufferedImage image) {
            myImage = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor dataFlavor) {
            return DataFlavor.imageFlavor.equals(dataFlavor);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor dataFlavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(dataFlavor)) {
                throw new UnsupportedFlavorException(dataFlavor);
            }
            return myImage;
        }
    }

    private class DialogResizer implements AWTEventListener, Disposable {
        DialogResizer() {
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
        }

        @Override
        public void dispose() {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            if (event instanceof MouseEvent) {
                processMouseEvent((MouseEvent)event);
            }
        }

        private void processMouseEvent(MouseEvent me) {
            if (me.getClickCount() != 1 || me.isPopupTrigger()) return;
            if (me.getID() != MouseEvent.MOUSE_RELEASED) return;
            if (me.isShiftDown() && me.isControlDown()) {
                me.consume();
                Window parent = getTopWindowComponent(me);
                if (parent == null) return;

                int adjustment = 0;
                if (myUseRealSize) {
                    //os toolbar height
                    adjustment = parent.getHeight() - parent.getComponent(0).getHeight();
                }
                parent.setSize(getMyWidth(), getMyHeight() + adjustment);
            } else if (me.isShiftDown() && me.isAltDown()) {
                me.consume();
                Window parent = getTopWindowComponent(me);
                if (parent != null && parent.getComponent(0) != null) {
                    copyImageToClipboard(parent.getComponent(0));
                }
            }
        }

        private Window getTopWindowComponent(MouseEvent me) {
            Component component = me.getComponent();
            Component parent = component.getParent();
            while (!(parent instanceof Dialog) && !(parent instanceof Frame)) {
                parent = parent.getParent();
                if (parent == null) return null;
            }
            return (Window) parent;
        }
    }
}
