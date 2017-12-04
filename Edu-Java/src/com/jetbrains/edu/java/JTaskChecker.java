package com.jetbrains.edu.java;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.RunConfigurationBasedTaskChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JTaskChecker extends RunConfigurationBasedTaskChecker {
  @Override
  public boolean isAccepted(@NotNull Task task) {
    return task instanceof EduTask;
  }

  @Nullable
  @Override
  protected VirtualFile getTestsFile(@NotNull Task task, @NotNull Project project) {
    String testFileName = JConfigurator.TEST_JAVA;
    if (task instanceof TaskWithSubtasks) {
      int activeSubtaskIndex = ((TaskWithSubtasks) task).getActiveSubtaskIndex();
      testFileName = FileUtil.getNameWithoutExtension(testFileName) + EduNames.SUBTASK_MARKER + activeSubtaskIndex + "." + FileUtilRt.getExtension(JConfigurator.TEST_JAVA);
    }
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return null;
    }
    VirtualFile srcDir = taskDir.findChild(EduNames.SRC);
    if (srcDir != null) {
      taskDir = srcDir;
    }
    VirtualFile virtualFile = taskDir.findChild(testFileName);
    if (virtualFile != null) {
      return virtualFile;
    }
    return null;
  }

  @Override
  protected void setProcessParameters(Project project, ApplicationConfiguration configuration, Module module, @NotNull VirtualFile testsFile) {
    configuration.setMainClassName(RunConfigurationBasedTaskChecker.TEST_RUNNER_CLASS);
    configuration.setModule(module);
    configuration.setProgramParameters(testsFile.getNameWithoutExtension());
  }
}
