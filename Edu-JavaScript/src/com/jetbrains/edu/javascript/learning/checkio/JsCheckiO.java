package com.jetbrains.edu.javascript.learning.checkio;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import org.jetbrains.annotations.NotNull;

public class JsCheckiO extends Language implements DependentLanguage {
  protected JsCheckiO() {
    super(JsCheckiONames.JS_CHECKIO_LANGUAGE);
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return JavascriptLanguage.INSTANCE.getDisplayName();
  }
}
