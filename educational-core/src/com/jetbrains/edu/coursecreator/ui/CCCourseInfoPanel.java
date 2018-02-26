package com.jetbrains.edu.coursecreator.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CCCourseInfoPanel {
  private JPanel myPanel;
  private JTextArea myDescription;
  private JTextField myName;
  private JTextField myAuthor;
  private JLabel myLanguageLevelLabel;
  private ComboBox<String> myLanguageLevelCombobox;
  private JLabel myErrorLabel;
  private JLabel myAuthorLabel;

  private List<Validator> myValidators = new ArrayList<>();

  public CCCourseInfoPanel(String name, String author, String description) {
    myLanguageLevelLabel.setVisible(false);
    myLanguageLevelCombobox.setVisible(false);

    myDescription.setBorder(BorderFactory.createLineBorder(JBColor.border()));
    myDescription.setFont(myAuthor.getFont());

    myErrorLabel.setVisible(false);
    myErrorLabel.setForeground(MessageType.ERROR.getTitleForeground());

    Validator inputValidator = createInputValidator();
    myValidators.add(inputValidator);

    myName.setText(name);
    myAuthor.setText(author);
    myDescription.setText(description);
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


  public ValidationInfo validate() {
    for (Validator validator : myValidators) {
      ValidationInfo validationInfo = validator.validate();
      if (validationInfo != null) {
        return validationInfo;
      }
    }
    return null;
  }

  private Validator createInputValidator() {
    return new Validator() {
      @Override
      public ValidationInfo validate() {
        if (StringUtil.isEmpty(myName.getText())) {
          return new ValidationInfo("Enter course title", myName);
        }
        else if (StringUtil.isEmpty(myAuthor.getText())) {
          return new ValidationInfo("Enter course instructor", myAuthor);
        }
        else if (StringUtil.isEmpty(myDescription.getText())) {
          return new ValidationInfo("Enter course description", myDescription);
        }
        else {
          return null;
        }
      }
    };
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

  public void showAuthorField(boolean isVisible) {
    myAuthor.setVisible(isVisible);
    myAuthorLabel.setVisible(isVisible);
  }

  public interface Validator {
    ValidationInfo validate();
  }
}
