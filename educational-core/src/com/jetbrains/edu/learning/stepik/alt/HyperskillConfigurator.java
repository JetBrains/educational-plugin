package com.jetbrains.edu.learning.stepik.alt;

import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase;
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase;
import org.jetbrains.annotations.NotNull;

public class HyperskillConfigurator extends GradleConfiguratorBase {
  private final HyperskillCourseBuilder myCourseBuilder = new HyperskillCourseBuilder();

  @NotNull
  @Override
  public GradleCourseBuilderBase getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return null;
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return null;
  }
}
