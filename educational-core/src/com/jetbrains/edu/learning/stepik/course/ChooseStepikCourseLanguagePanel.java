package com.jetbrains.edu.learning.stepik.course;

import com.intellij.lang.Language;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.stepik.StepikLanguage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChooseStepikCourseLanguagePanel {
  private JComboBox<StepikLanguage> languageCombobox;
  private JLabel courseNameLabel;
  private JPanel mainPanel;

  public ChooseStepikCourseLanguagePanel(@NotNull List<StepikLanguage> languages, @NotNull String courseName) {
    setLanguages(languages);
    setCourseNameLabel(courseName);
    mainPanel.setPreferredSize(JBUI.size(new Dimension(400, 50)));
  }

  public JPanel constructMainPanel() {
    return mainPanel;
  }


  public StepikLanguage getSelectedLanguage() {
    return (StepikLanguage) languageCombobox.getSelectedItem();
  }

  private void setLanguages(@NotNull List<StepikLanguage> languages) {
    languageCombobox.setRenderer(new ListCellRendererWrapper<StepikLanguage>() {

      @Override
      public void customize(JList list, StepikLanguage value, int index, boolean selected, boolean hasFocus) {
        Language language = Language.findLanguageByID(value.getId());
        if (language != null) {
          setText(language.getDisplayName() + " " + value.getVersion());
        }
      }
    });
    for (StepikLanguage language: languages) {
      languageCombobox.addItem(language);
    }
  }

  private void setCourseNameLabel(@NotNull String courseName) {
    courseNameLabel.setText(courseName);
  }
}
