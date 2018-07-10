package com.jetbrains.edu.learning;

import com.intellij.openapi.ui.LabeledComponent;
import com.jetbrains.edu.learning.courseFormat.Course;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main interface responsible for course project language settings such as JDK or interpreter
 *
 * @param <Settings> container type holds project settings state
 */
public abstract class LanguageSettings<Settings> {

  protected List<SettingsChangeListener> myListeners = new ArrayList<>();

  /**
   * Returns list of UI components that allows user to select course project settings such as project JDK or interpreter.
   *
   * @param course course of creating project
   * @return list of UI components with project settings
   */
  @NotNull
  public List<LabeledComponent<JComponent>> getLanguageSettingsComponents(@NotNull Course course) {
    return Collections.emptyList();
  }

  public void addSettingsChangeListener(@NotNull SettingsChangeListener listener) {
    myListeners.add(listener);
  }

  @Nullable
  public String validate() {
    return null;
  }

  /**
   * Returns project settings associated with state of language settings UI component.
   * It should be passed into project generator to set chosen settings in course project.
   *
   * @return project settings object
   */
  @NotNull
  public abstract Settings getSettings();

  protected void notifyListeners() {
    for (SettingsChangeListener listener : myListeners) {
      listener.settingsChanged();
    }
  }

  public interface SettingsChangeListener {
    void settingsChanged();
  }
}
