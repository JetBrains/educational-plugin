package com.jetbrains.edu.java;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.utils.EduIntelliJNames;
import com.jetbrains.edu.utils.EduPyCharmTasksChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EduJavaPyCharmTaskChecker extends EduPyCharmTasksChecker{
  public EduJavaPyCharmTaskChecker(@NotNull PyCharmTask task, @NotNull Project project) {
    super(task, project);
  }

  @Nullable
  @Override
  protected VirtualFile getTestsFile() {
    String testFileName = EduJavaPluginConfigurator.TEST_JAVA;
    if (myTask instanceof TaskWithSubtasks) {
      int activeSubtaskIndex = ((TaskWithSubtasks) myTask).getActiveSubtaskIndex();
      testFileName = FileUtil.getNameWithoutExtension(testFileName) + EduNames.SUBTASK_MARKER + activeSubtaskIndex + "." + FileUtilRt.getExtension(EduJavaPluginConfigurator.TEST_JAVA);
    }
    VirtualFile taskDir = myTask.getTaskDir(myProject);
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
    configuration.setMainClassName(EduIntelliJNames.TEST_RUNNER_CLASS);
    configuration.setModule(module);
    configuration.setProgramParameters(testsFile.getNameWithoutExtension());
  }
}
