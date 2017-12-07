package com.jetbrains.edu.kotlin;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.RunConfigurationBasedTaskChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClass;

import java.util.Collection;

public class KtTaskChecker extends RunConfigurationBasedTaskChecker {

  public static final CheckResult FAILED_TO_LAUNCH = new CheckResult(CheckStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH);

  @Override
  public boolean isAccepted(@NotNull Task task) {
    return task instanceof EduTask && !EduUtils.isAndroidStudio();
  }

  @Nullable
  @Override
  protected VirtualFile getTestsFile(Task task, Project project) {
    VirtualFile taskDir = task.getTaskDir(project);
    if (taskDir == null) {
      return null;
    }
    if (task instanceof TaskWithSubtasks) {
      int subTaskIndex = ((TaskWithSubtasks) task).getActiveSubtaskIndex();
      String testFileName = SubtaskUtils.getTestFileName(project, subTaskIndex);
      return testFileName != null ? taskDir.findChild(testFileName) : null;
    }
    for (String testFileName : task.getTestsText().keySet()) {
      VirtualFile testFile = VfsUtil.findRelativeFile(taskDir, testFileName);
      if (testFile != null) {
        return testFile;
      }
    }
    VirtualFile testsFile = taskDir.findChild(KtConfigurator.LEGACY_TESTS_KT);
    if (testsFile != null) {
      return testsFile;
    }
    return taskDir.findChild(KtConfigurator.TESTS_KT);
  }

  @Override
  protected void setProcessParameters(Project project, ApplicationConfiguration configuration, Module module, @NotNull VirtualFile testsFile) {
    configuration.setMainClassName(RunConfigurationBasedTaskChecker.TEST_RUNNER_CLASS);
    configuration.setModule(module);
    PsiFile psiFile = PsiManager.getInstance(project).findFile(testsFile);
    Collection<KtClass> ktClasses = PsiTreeUtil.findChildrenOfType(psiFile, KtClass.class);
    for (KtClass ktClass : ktClasses) {
      String name = ktClass.getName();
      configuration.setProgramParameters(name);
    }
  }
}
