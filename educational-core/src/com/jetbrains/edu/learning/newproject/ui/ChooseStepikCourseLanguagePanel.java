package com.jetbrains.edu.learning.newproject.ui;

import com.intellij.lang.Language;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChooseStepikCourseLanguagePanel {
  private JComboBox<Language> languageCombobox;
  private JLabel courseNameLabel;
  private JPanel mainPanel;

  public ChooseStepikCourseLanguagePanel(@NotNull List<Language> languages, @NotNull String courseName) {
    setLanguages(languages);
    setCourseNameLabel(courseName);
    mainPanel.setPreferredSize(JBUI.size(new Dimension(400, 50)));
  }

  public JPanel constructMainPanel() {
    return mainPanel;
  }


  public Language getSelectedLanguage() {
    return (Language) languageCombobox.getSelectedItem();
  }

  private void setLanguages(@NotNull List<Language> languages) {
    languageCombobox.setRenderer(new ListCellRendererWrapper<Language>() {

      @Override
      public void customize(JList list, Language value, int index, boolean selected, boolean hasFocus) {
        setText(value.getDisplayName());
      }
    });
    for (Language language: languages) {
      languageCombobox.addItem(language);
    }
  }

  private void setCourseNameLabel(@NotNull String courseName) {
    courseNameLabel.setText(courseName);
  }
}
