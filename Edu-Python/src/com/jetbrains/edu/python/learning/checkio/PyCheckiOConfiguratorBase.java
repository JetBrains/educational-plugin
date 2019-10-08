package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils;
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.python.learning.PyConfiguratorBase;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames;
import com.jetbrains.python.PythonFileType;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class PyCheckiOConfiguratorBase extends PyConfiguratorBase implements CheckiOConnectorProvider {

  private final CheckiOCourseContentGenerator myContentGenerator =
    new CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector.getInstance());

  public PyCheckiOConfiguratorBase(@NotNull PyCheckiOCourseBuilderBase courseBuilder) {
    super(courseBuilder);
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

  @Override
  public void beforeCourseStarted(@NotNull Course course) throws CourseCantBeStartedException {
    CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress(myContentGenerator, (CheckiOCourse)course,
                                                                  PyCheckiOSettings.INSTANCE.getAccount(),
                                                                  PyCheckiONames.PY_CHECKIO_API_HOST);
  }
}
