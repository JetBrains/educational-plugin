package com.jetbrains.edu.learning.stepik.course;

import com.intellij.util.ui.JBUI;
import com.jetbrains.edu.learning.courseFormat.EduLanguage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChooseStepikCourseLanguagePanel {
  private JComboBox<EduLanguage> languageCombobox;
  private JLabel courseNameLabel;
  private JPanel mainPanel;

  public ChooseStepikCourseLanguagePanel(@NotNull List<EduLanguage> languages, @NotNull String courseName) {
    setLanguages(languages);
    setCourseNameLabel(courseName);
    mainPanel.setPreferredSize(JBUI.size(new Dimension(400, 50)));
  }

  public JPanel constructMainPanel() {
    return mainPanel;
  }


  public EduLanguage getSelectedLanguage() {
    return (EduLanguage) languageCombobox.getSelectedItem();
  }

  private void setLanguages(@NotNull List<EduLanguage> languages) {
    for (EduLanguage language: languages) {
      languageCombobox.addItem(language);
    }
  }

  private void setCourseNameLabel(@NotNull String courseName) {
    courseNameLabel.setText(courseName);
  }
}
