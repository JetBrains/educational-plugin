package com.jetbrains.edu.javascript.learning.checkio;

import com.jetbrains.edu.learning.EduLanguageDecorator;
import org.jetbrains.annotations.NotNull;

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
}
