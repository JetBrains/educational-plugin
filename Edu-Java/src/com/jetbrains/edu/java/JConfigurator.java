package com.jetbrains.edu.java;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduCourseBuilder;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.intellij.EduConfiguratorBase;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import org.jetbrains.annotations.NotNull;

public class JConfigurator extends EduConfiguratorBase {
  static final String TEST_JAVA = "Test.java";
  private static final String TASK_JAVA = "Task.java";

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

  @NotNull
  @Override
  public String getStepikDefaultLanguage() {
    return "java8";
  }

  @Override
  public boolean isTestFile(VirtualFile file) {
    String name = file.getName();
    return TEST_JAVA.equals(name) || name.contains(FileUtil.getNameWithoutExtension(TEST_JAVA)) && name.contains(EduNames.SUBTASK_MARKER);
  }

  @NotNull
  @Override
  public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask eduTask, @NotNull Project project) {
    return new JTaskChecker(eduTask, project);
  }

  @Override
  public boolean isEnabled() {
    return !EduUtils.isAndroidStudio();
  }
}
