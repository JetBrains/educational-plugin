package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.python.learning.checkio.checker.PyCheckiOTaskCheckerProvider;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOConfigurator implements EduConfigurator<PyNewProjectSettings> {

  private final PyCheckiOCourseBuilder myCourseBuilder = new PyCheckiOCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<PyNewProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return "";
  }

  @Override
  public boolean excludeFromArchive(@NotNull String name) {
    return name.contains("__pycache__") || name.endsWith(".pyc");
  }

  @Override
  public boolean isEnabled() {
    // TODO
    return true;
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new PyCheckiOTaskCheckerProvider();
  }
}
