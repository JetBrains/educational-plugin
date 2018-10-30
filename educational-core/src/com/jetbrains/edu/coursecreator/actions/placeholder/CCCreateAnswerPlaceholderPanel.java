package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class CCCreateAnswerPlaceholderPanel {

  private JPanel myPanel;
  private JTextArea myPlaceholderTextArea;

  public CCCreateAnswerPlaceholderPanel(@Nullable String placeholderText) {
    myPlaceholderTextArea.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    myPlaceholderTextArea.setText(placeholderText);
    myPlaceholderTextArea.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        myPlaceholderTextArea.selectAll();
      }
    });
    myPlaceholderTextArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
    myPlaceholderTextArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    myPlaceholderTextArea.setFont(UIUtil.getLabelFont());
  }

  public String getAnswerPlaceholderText() {
    return myPlaceholderTextArea.getText();
  }

  public JComponent getPreferredFocusedComponent() {
    return myPlaceholderTextArea;
  }

  public JPanel getMailPanel() {
    return myPanel;
  }
}
