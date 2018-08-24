package com.jetbrains.edu.javascript.learning;

import com.jetbrains.edu.learning.LanguageSettings;
import org.jetbrains.annotations.NotNull;

public class JsLanguageSettings extends LanguageSettings<JsNewProjectSettings> {
  private final JsNewProjectSettings mySettings = new JsNewProjectSettings();

  @NotNull
  @Override
  public JsNewProjectSettings getSettings() {
    return mySettings;
  }
}
