package com.jetbrains.edu.coursecreator.actions.placeholder;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class CCCreateAnswerPlaceholderPanel {

  private JPanel myPanel;
  private JTextArea myPlaceholderTextArea;
  private JLabel myLabel;

  public CCCreateAnswerPlaceholderPanel(@Nullable String placeholderText) {
    myPanel.setPreferredSize(new Dimension(JBUI.scale(400), JBUI.scale(70)));
    myPlaceholderTextArea.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    myPlaceholderTextArea.setText(placeholderText);
    myPlaceholderTextArea.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        myPlaceholderTextArea.selectAll();
      }
    });
    myPlaceholderTextArea.setFont(UIUtil.getLabelFont());
    myLabel.setForeground(JBColor.GRAY);
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
