package com.jetbrains.edu.python.learning.checkio;

import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.PyConfigurator;
import com.jetbrains.edu.python.learning.checkio.checker.PyCheckiOTaskCheckerProvider;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.python.newProject.PyNewProjectSettings;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PyCheckiOConfigurator extends PyConfigurator implements CheckiOConnectorProvider {
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

  @NotNull
  @Override
  public CheckiOOAuthConnector getOAuthConnector() {
    return PyCheckiOOAuthConnector.getInstance();
  }

  @NotNull
  @Override
  public Icon getLogo() {
    return EducationalCoreIcons.CheckiO;
  }

}
