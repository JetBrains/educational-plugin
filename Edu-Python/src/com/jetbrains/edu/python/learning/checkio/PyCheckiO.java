package com.jetbrains.edu.python.learning.checkio;

import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import org.jetbrains.annotations.NotNull;

public class PyCheckiO extends Language implements DependentLanguage {
  protected PyCheckiO() {
    super(PyCheckiONames.CHECKIO_PYTHON);
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return EduNames.PYTHON;
  }
}
