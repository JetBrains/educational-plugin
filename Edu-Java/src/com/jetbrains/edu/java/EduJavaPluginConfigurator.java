package com.jetbrains.edu.java;

import com.intellij.ide.IdeView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.utils.EduIntellijUtils;
import com.jetbrains.edu.utils.EduPluginConfiguratorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduJavaPluginConfigurator extends EduPluginConfiguratorBase {
  static final String TEST_JAVA = "Test.java";

  @NotNull
  @Override
  public String getTestFileName() {
    return TEST_JAVA;
  }

  @Override
  public StudyCheckAction getCheckAction() {
    return new EduJavaCheckAction();
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
  public void createTestsForNewSubtask(@NotNull Project project, @NotNull Task task) {
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

  @Override
  public PsiDirectory createTask(@NotNull Project project, @NotNull StudyItem item, @Nullable IdeView view, @NotNull PsiDirectory parentDirectory, @NotNull Course course) {
    return EduIntellijUtils.createTask(project, item, view, parentDirectory, "Task.java", TEST_JAVA);
  }
}
