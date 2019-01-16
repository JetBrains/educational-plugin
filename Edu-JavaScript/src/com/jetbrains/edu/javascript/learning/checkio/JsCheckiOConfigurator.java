package com.jetbrains.edu.javascript.learning.checkio;

import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.javascript.learning.JsConfigurator;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOApiConnector;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
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
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JsCheckiOConfigurator extends JsConfigurator implements CheckiOConnectorProvider {
  private final CheckiOCourseContentGenerator myContentGenerator =
    new CheckiOCourseContentGenerator(JavaScriptFileType.INSTANCE, JsCheckiOApiConnector.getInstance());

  @NotNull
  @Override
  public String getTestFileName() {
    return "";
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
          JsCheckiOOAuthConnector.getInstance(),
          JsCheckiONames.JS_CHECKIO_INTERPRETER,
          JsCheckiONames.JS_CHECKIO_TEST_FORM_TARGET_URL
        );
      }
    };
  }

  @NotNull
  @Override
  public CheckiOOAuthConnector getOAuthConnector() {
    return JsCheckiOOAuthConnector.getInstance();
  }

  @NotNull
  @Override
  public Icon getLogo() {
    return EducationalCoreIcons.JSCheckiO;
  }

  @Override
  public boolean isCourseCreatorEnabled() {
    return false;
  }

  @Override
  public void beforeCourseStarted(@NotNull Course course) throws CourseCantBeStartedException {
    CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress(myContentGenerator, (CheckiOCourse)course,
                                                                  JsCheckiOSettings.getInstance().getAccount(),
                                                                  JsCheckiONames.JS_CHECKIO_API_HOST);
  }
}
