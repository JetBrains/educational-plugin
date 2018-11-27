package com.jetbrains.edu.javascript.learning.checkio;

import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider;
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.configuration.EduConfigurator;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class JsCheckiOConfigurator implements EduConfigurator<JsNewProjectSettings>, CheckiOConnectorProvider {
  private final JsCheckiOCourseBuilder myCourseBuilder = new JsCheckiOCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<JsNewProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

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

  @Override
  public boolean isEnabled() {
    return !(EduUtils.isAndroidStudio() || PlatformUtils.isCommunityEdition() || PlatformUtils.isPyCharmEducational());
  }

  @Override
  public List<String> pluginRequirements() {
    return Collections.singletonList("NodeJS");
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
}
