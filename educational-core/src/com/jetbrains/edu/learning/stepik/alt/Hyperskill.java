package com.jetbrains.edu.learning.stepik.alt;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

public class Hyperskill extends Language implements DependentLanguage {
  protected Hyperskill() {
    super("Hyperskill-JAVA");
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "JAVA";
  }
}
