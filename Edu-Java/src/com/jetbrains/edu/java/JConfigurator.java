package com.jetbrains.edu.java;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.EduConfiguratorBase;
import com.jetbrains.edu.learning.intellij.EduIntellijUtils;
import com.jetbrains.edu.learning.intellij.JdkProjectSettings;
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class JConfigurator extends EduConfiguratorBase {
  static final String TEST_JAVA = "Test.java";
  private static final String TASK_JAVA = "Task.java";

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
  private static String getSubtaskTestsClassName(int index) {
    return index == 0 ? TEST_JAVA : FileUtil.getNameWithoutExtension(TEST_JAVA) + EduNames.SUBTASK_MARKER + index;
  }

  @Override
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull TaskWithSubtasks task) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return;
    }
    int prevSubtaskIndex = task.getLastSubtaskIndex();
    PsiDirectory taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir);
    if (taskPsiDir == null) {
      return;
    }
    int nextSubtaskIndex = prevSubtaskIndex + 1;
    String nextSubtaskTestsClassName = getSubtaskTestsClassName(nextSubtaskIndex);
    JavaDirectoryService.getInstance().createClass(taskPsiDir, nextSubtaskTestsClassName);
  }

  @NotNull
  @Override
  public TaskChecker<EduTask> getEduTaskChecker(@NotNull EduTask eduTask, @NotNull Project project) {
    return new JTaskChecker(eduTask, project);
  }

  @Override
  public VirtualFile createTaskContent(@NotNull Project project, @NotNull Task task,
                                       @NotNull VirtualFile parentDirectory, @NotNull Course course) {
    return EduIntellijUtils.createTask(project, task, parentDirectory, TASK_JAVA, TEST_JAVA);
  }

  @Override
  public CourseProjectGenerator<JdkProjectSettings> getEduCourseProjectGenerator(@NotNull Course course) {
    return new JCourseProjectGenerator(course);
  }

  @Override
  public boolean isEnabled() {
    return !EduUtils.isAndroidStudio();
  }
}
