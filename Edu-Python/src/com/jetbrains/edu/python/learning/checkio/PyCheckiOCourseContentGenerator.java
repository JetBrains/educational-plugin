package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseContentGenerator extends CheckiOCourseContentGenerator {
  protected PyCheckiOCourseContentGenerator() {
    super(new PyCheckiOCourseProvider(), PythonFileType.INSTANCE.getDefaultExtension());
  }

  private static class Holder {
    private static final PyCheckiOCourseContentGenerator INSTANCE = new PyCheckiOCourseContentGenerator();
  }

  @NotNull
  public static PyCheckiOCourseContentGenerator getInstance() {
    return Holder.INSTANCE;
  }
}
