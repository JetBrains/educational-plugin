package com.jetbrains.edu.javascript.learning.checkio;

import com.intellij.util.PlatformUtils;
import com.jetbrains.edu.javascript.learning.JsNewProjectSettings;
import com.jetbrains.edu.javascript.learning.checkio.checker.JsCheckiOTaskCheckerProvider;
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.learning.EduConfigurator;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import org.jetbrains.annotations.NotNull;

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
    return new JsCheckiOTaskCheckerProvider();
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
}
