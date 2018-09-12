// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.maxal.dialogResizer;

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

public class DialogResizerAction extends ToggleAction implements DumbAware {

    private DialogResizer myDialogResizer;
    private Integer myHeight = null;
    private Integer myWeight = null;
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
                myHeight = Integer.valueOf(dialogResizerWindow.myHeightTextField.getText());
                myWeight = Integer.valueOf(dialogResizerWindow.myWeightTextField.getText());
                myUseRealSize = dialogResizerWindow.useRealSizeCheckbox.isSelected();
                if (myDialogResizer == null) {
                    myDialogResizer = new DialogResizer();
                }
                Notifications.Bus.notify(new Notification("Resizer", "Dialog Resizer", "Control-Shift-Click to resize the component!",
                        NotificationType.INFORMATION, null));
            }
        } else {
            myDialogResizer = null;
        }
    }

    private class DialogResizerWindow extends DialogWrapper {

        private JBTextField myHeightTextField;
        private JBTextField myWeightTextField;
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
            firstLine.add(new JBLabel("Height: "), BorderLayout.WEST);
            myHeightTextField = new JBTextField(1);
            if (myHeight != null) myHeightTextField.setText(myHeight.toString());
            firstLine.add(myHeightTextField, BorderLayout.CENTER);
            panel.add(firstLine, BorderLayout.NORTH);

            JPanel secondLine = new JPanel(new BorderLayout());
            secondLine.add(new JBLabel("Width:  "), BorderLayout.WEST);
            myWeightTextField = new JBTextField(1);
            if (myWeight != null) myWeightTextField.setText(myWeight.toString());
            secondLine.add(myWeightTextField, BorderLayout.CENTER);
            panel.add(secondLine, BorderLayout.CENTER);

            JPanel thirdLine = new JPanel(new BorderLayout());
            useRealSizeCheckbox = new JCheckBox("Size without OS toolbar", false);
            useRealSizeCheckbox.setSelected(myUseRealSize);
            thirdLine.add(useRealSizeCheckbox, BorderLayout.WEST);
            panel.add(thirdLine, BorderLayout.SOUTH);

            return panel;
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
            if (!me.isShiftDown() || !me.isControlDown()) return;
            if (me.getClickCount() != 1 || me.isPopupTrigger()) return;
            me.consume();
            if (me.getID() != MouseEvent.MOUSE_RELEASED) return;
            Component component = me.getComponent();
            Component parent = component.getParent();
            while (!(parent instanceof Dialog) && !(parent instanceof Frame)) {
                parent = parent.getParent();
                if (parent == null) return;
            }

            int adjustment = 0;
            if (myUseRealSize) {
                //os toolbar height
                adjustment = parent.getHeight() - ((Container) parent).getComponent(0).getHeight();
            }
            parent.setSize(myWeight, myHeight + adjustment);
        }
    }
}
