package com.jetbrains.edu.learning.newproject.ui;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.edu.learning.stepic.StepicConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class ImportStepikCoursePanel {
  private JTextField myCourseLinkTextField;
  private JPanel myPanel;
  private JLabel helpLabel;

  public ImportStepikCoursePanel() {
    myPanel.setPreferredSize(JBUI.size(new Dimension(400, 30)));
    helpLabel.setForeground(UIUtil.getLabelDisabledForeground());
    Font font = helpLabel.getFont();
    float newFontSize = Math.max(10f, font.getSize() - 2f);
    helpLabel.setFont(font.deriveFont(Float.valueOf(JBUI.scaleFontSize(newFontSize))));
    helpLabel.setBorder(JBUI.Borders.emptyLeft(JBUI.scale(10)));
  }

  public String getCourseLink() {
    return myCourseLinkTextField.getText();
  }

  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return myCourseLinkTextField;
  }

  public JPanel getMainPanel() {
    return myPanel;
  }

  public boolean validate() {
    String text = myCourseLinkTextField.getText();
    return !text.isEmpty() && (isDigit(text) || isValidStepikLink(text));
  }

  private static boolean isDigit(@NotNull String text) {
    for (int i = 0; i < text.length(); i++) {
      if (!Character.isDigit(text.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  private static boolean isValidStepikLink(@NotNull String text) {
    return StepicConnector.getCourseIdFromLink(text) != -1;
  }
}
