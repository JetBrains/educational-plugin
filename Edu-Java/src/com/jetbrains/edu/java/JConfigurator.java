package com.jetbrains.edu.java;

import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.lang.java.JavaLanguage;
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase;
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase;
import com.jetbrains.edu.jvm.stepik.CodeTaskHelper;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JConfigurator extends GradleConfiguratorBase {

  public static final String TEST_JAVA = "Tests.java";
  public static final String TASK_JAVA = "Task.java";
  public static final String MOCK_JAVA = "Mock.java";

  private final JCourseBuilder myCourseBuilder = new JCourseBuilder();

  @NotNull
  @Override
  public GradleCourseBuilderBase getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return TEST_JAVA;
  }

  @Override
  public boolean isEnabled() {
    return !EduUtils.isAndroidStudio();
  }

  @NotNull
  @Override
  public TaskCheckerProvider getTaskCheckerProvider() {
    return new JTaskCheckerProvider();
  }

  @Override
  public String getMockFileName(@NotNull String text) {
    return CodeTaskHelper.fileName(JavaLanguage.INSTANCE, text);
  }

  @Override
  public String getMockTemplate() {
    return FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_JAVA).getText();
  }

  @NotNull
  @Override
  public Icon getLogo() {
    return EducationalCoreIcons.JavaLogo;
  }
}
