package com.jetbrains.edu.java.learning.stepik.alt;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.jetbrains.edu.learning.EduNames;
import org.jetbrains.annotations.NotNull;

public class JHyperskill extends Language implements DependentLanguage {
  protected JHyperskill() {
    super("Hyperskill-JAVA");
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return EduNames.JAVA;
  }
}
