package com.jetbrains.edu.java;

import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskCheckerProvider;
import com.jetbrains.edu.learning.intellij.GradleConfiguratorBase;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import org.jetbrains.annotations.NotNull;

public class JConfigurator extends GradleConfiguratorBase {

  public static final String TEST_JAVA = "Test.java";
  public static final String TASK_JAVA = "Task.java";
  public static final String SUBTASK_TEST_JAVA = "Subtask_Test.java";
  public static final String MOCK_JAVA = "Mock.java";

  private final JCourseBuilder myCourseBuilder = new JCourseBuilder();

  @NotNull
  @Override
  public EduCourseBuilder<JdkProjectSettings> getCourseBuilder() {
    return myCourseBuilder;
  }

  @NotNull
  @Override
  public String getTestFileName() {
    return TEST_JAVA;
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    return TEST_JAVA.equals(name) || name.contains(FileUtil.getNameWithoutExtension(TEST_JAVA)) && name.contains(EduNames.SUBTASK_MARKER);
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
  public String getMockTemplate() {
    return FileTemplateManager.getDefaultInstance().getInternalTemplate(MOCK_JAVA).getText();
  }
}
