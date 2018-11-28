package com.jetbrains.edu.python.learning.checkio.newProject;

import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.python.learning.PyCourseBuilder;
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOCourseProjectGenerator extends PyCourseProjectGenerator {
  public PyCheckiOCourseProjectGenerator(@NotNull PyCourseBuilder builder,
                                         @NotNull Course course) {
    super(builder, course);
  }

  @Override
  protected boolean beforeProjectGenerated() {
    return true;
  }
}
