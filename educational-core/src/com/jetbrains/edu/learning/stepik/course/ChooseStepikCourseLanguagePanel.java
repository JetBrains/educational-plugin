package com.jetbrains.edu.learning.stepik.course;

import com.intellij.lang.Language;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.stepik.StepikLanguages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChooseStepikCourseLanguagePanel {
  private JComboBox<StepikLanguages> languageCombobox;
  private JLabel courseNameLabel;
  private JPanel mainPanel;

  public ChooseStepikCourseLanguagePanel(@NotNull List<StepikLanguages> languages, @NotNull String courseName) {
    setLanguages(languages);
    setCourseNameLabel(courseName);
    mainPanel.setPreferredSize(JBUI.size(new Dimension(400, 50)));
  }

  public JPanel constructMainPanel() {
    return mainPanel;
  }


  public StepikLanguages getSelectedLanguage() {
    return (StepikLanguages) languageCombobox.getSelectedItem();
  }

  private void setLanguages(@NotNull List<StepikLanguages> languages) {
    languageCombobox.setRenderer(new ListCellRendererWrapper<StepikLanguages>() {

      @Override
      public void customize(JList list, StepikLanguages value, int index, boolean selected, boolean hasFocus) {
        Language language = Language.findLanguageByID(value.getId());
        if (language != null) {
          setText(language.getDisplayName() + " " + value.getVersion());
        }
      }
    });
    for (StepikLanguages language: languages) {
      languageCombobox.addItem(language);
    }
  }

  private void setCourseNameLabel(@NotNull String courseName) {
    courseNameLabel.setText(courseName);
  }
}
