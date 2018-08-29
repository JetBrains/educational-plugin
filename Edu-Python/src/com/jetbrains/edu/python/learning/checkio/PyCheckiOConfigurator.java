package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider;
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.python.learning.PyConfigurator;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
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
    return new TaskCheckerProvider() {
      @NotNull
      @Override
      public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask task, @NotNull Project project) {
        return new CheckiOTaskChecker(
          task,
          project,
          PyCheckiOOAuthConnector.getInstance(),
          PyCheckiONames.PY_CHECKIO_INTERPRETER,
          PyCheckiONames.PY_CHECKIO_TEST_FORM_TARGET_URL
        );
      }
    };
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

  @Override
  public boolean isCourseCreatorEnabled() {
    return false;
  }
}
