package com.jetbrains.edu.javascript.learning.checkio;

import com.jetbrains.edu.learning.EduLanguageDecorator;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JsCheckiOLanguageDecorator implements EduLanguageDecorator {
  @NotNull
  @Override
  public String getLanguageScriptUrl() {
    return getClass().getResource("/code-mirror/javascript.js").toExternalForm();
  }

  @NotNull
  @Override
  public String getDefaultHighlightingMode() {
    return "javascript";
  }

  @NotNull
  @Override
  public Icon getLogo() {
    return EducationalCoreIcons.CheckiO; // TODO
  }
}
