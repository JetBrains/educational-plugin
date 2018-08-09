package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checkio.CheckiOConfigurator;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.checker.PyCheckiOTaskCheckerProvider;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOConfigurator implements CheckiOConfigurator<PyNewProjectSettings> {

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
  public boolean excludeFromArchive(@NotNull Project project, @NotNull String path) {
    return path.contains("__pycache__") || path.endsWith(".pyc");
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new PyCheckiOTaskCheckerProvider();
  }

  @Override
  public boolean isEnabled() {
    return !(PlatformUtils.isPyCharm() || PlatformUtils.isCLion());
  }

  @NotNull
  @Override
  public CheckiOOAuthConnector getOAuthConnector() {
    return PyCheckiOOAuthConnector.getInstance();
  }
}
