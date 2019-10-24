package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.python.learning.PyCourseBuilderBase;
import org.jetbrains.annotations.Nullable;

public abstract class PyCheckiOCourseBuilderBase extends PyCourseBuilderBase {
  @Nullable
  @Override
  public String getTaskTemplateName() {
    return null;
  }

  @Nullable
  @Override
  public String getTestTemplateName() {
    return null;
  }
}
