package com.jetbrains.edu.learning.stepik;

import com.intellij.ui.components.JBTextField;

import javax.swing.*;

public class AuthorizationPanel {
  private JPanel myContentPanel;
  private JBTextField myCodeTextField;

  public JPanel getContentPanel() {
    return myContentPanel;
  }

  public String getCode() {
    return myCodeTextField.getText();
  }

  public JComponent getPreferableFocusComponent() {
    return myCodeTextField;
  }
}
