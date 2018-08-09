package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseContentGenerator extends CheckiOCourseContentGenerator {
  @Override
  protected String getTaskFileExtension() {
    return ".py";
  }

  @Override
  protected CheckiOCourse getNewCourseInstance() {
    return new PyCheckiOCourse();
  }

  private static class Holder {
    private static final PyCheckiOCourseContentGenerator INSTANCE = new PyCheckiOCourseContentGenerator();
  }

  @NotNull
  public static PyCheckiOCourseContentGenerator getInstance() {
    return Holder.INSTANCE;
  }
}
