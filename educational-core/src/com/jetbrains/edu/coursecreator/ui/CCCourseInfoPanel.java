package com.jetbrains.edu.coursecreator.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

public class CCCourseInfoPanel {
  private JPanel myPanel;
  private JTextArea myDescription;
  private JTextField myName;
  private JTextField myAuthor;
  private JLabel myLanguageLevelLabel;
  private ComboBox<String> myLanguageLevelCombobox;
  private JLabel myErrorLabel;

  private ValidationListener myValidationListener;

  public CCCourseInfoPanel(String name, String author, String description) {
    myLanguageLevelLabel.setVisible(false);
    myLanguageLevelCombobox.setVisible(false);

    myDescription.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    myDescription.setFont(myAuthor.getFont());

    myErrorLabel.setVisible(false);
    myErrorLabel.setForeground(MessageType.ERROR.getTitleForeground());

    setupValidation();

    myName.setText(name);
    myAuthor.setText(author);
    myDescription.setText(description);
  }

  private void setupValidation() {
    DocumentAdapter validator = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        doValidation();
      }
    };

    myName.getDocument().addDocumentListener(validator);
    myDescription.getDocument().addDocumentListener(validator);
    myAuthor.getDocument().addDocumentListener(validator);
  }

  public JPanel getMainPanel() {
    return myPanel;
  }

  @NotNull
  public String getName() {
    return StringUtil.notNullize(myName.getText());
  }

  @NotNull
  public String getDescription() {
    return StringUtil.notNullize(myDescription.getText());
  }

  @NotNull
  public String[] getAuthors() {
    return StringUtil.splitByLines(StringUtil.notNullize(myAuthor.getText()));
  }

  public void setValidationListener(ValidationListener listener) {
    myValidationListener = listener;
    doValidation();
  }

  private void doValidation() {
    final String message;
    if (StringUtil.isEmpty(myName.getText())) {
      message = "Enter course title";
    } else if (StringUtil.isEmpty(myAuthor.getText())) {
      message = "Enter course instructor";
    } else if (StringUtil.isEmpty(myDescription.getText())) {
      message = "Enter course description";
    } else {
      message = null;
    }

    if (message != null) {
      myErrorLabel.setVisible(true);
      myErrorLabel.setText(message);
    } else {
      myErrorLabel.setVisible(false);
    }

    if (myValidationListener != null) {
      myValidationListener.onInputDataValidated(message == null);
    }
  }

  public JLabel getLanguageLevelLabel() {
    return myLanguageLevelLabel;
  }

  public ComboBox<String> getLanguageLevelCombobox() {
    return myLanguageLevelCombobox;
  }

  @Nullable
  public String getLanguageVersion() {
    if (!myLanguageLevelCombobox.isVisible() || myLanguageLevelCombobox.getItemCount() == 0) {
      return null;
    }
    return (String)myLanguageLevelCombobox.getSelectedItem();
  }

  public interface ValidationListener {
    void onInputDataValidated(boolean isInputDataComplete);
  }
}
