package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.python.learning.PyConfigurator;
import com.jetbrains.edu.python.learning.checkio.checker.PyCheckiOTaskCheckerProvider;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOConfigurator extends PyConfigurator {
  private final PyCheckiOCourseBuilder myCourseBuilder = new PyCheckiOCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<PyNewProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new PyCheckiOTaskCheckerProvider();
  }
}
