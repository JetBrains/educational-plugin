package com.jetbrains.edu.kotlin;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.edu.learning.SubtaskUtils;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.EduIntelliJNames;
import com.jetbrains.edu.learning.intellij.RunConfigurationBasedTaskChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClass;

import java.util.Collection;

public class KtTaskChecker extends RunConfigurationBasedTaskChecker {

  public static final CheckResult FAILED_TO_LAUNCH = new CheckResult(CheckStatus.Unchecked, CheckAction.FAILED_CHECK_LAUNCH);

  public KtTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project);
  }

  @Nullable
  @Override
  protected VirtualFile getTestsFile() {
    VirtualFile taskDir = myTask.getTaskDir(myProject);
    if (taskDir == null) {
      return null;
    }
    if (myTask instanceof TaskWithSubtasks) {
      int subTaskIndex = ((TaskWithSubtasks) myTask).getActiveSubtaskIndex();
      String testFileName = SubtaskUtils.getTestFileName(myProject, subTaskIndex);
      return testFileName != null ? taskDir.findChild(testFileName) : null;
    }
    for (String testFileName : myTask.getTestsText().keySet()) {
      VirtualFile testFile = VfsUtil.findRelativeFile(taskDir, testFileName);
      if (testFile != null) {
        return testFile;
      }
    }
    VirtualFile testsFile = taskDir.findChild(KtPluginConfigurator.LEGACY_TESTS_KT);
    if (testsFile != null) {
      return testsFile;
    }
    return taskDir.findChild(KtPluginConfigurator.TESTS_KT);
  }

  @Override
  protected void setProcessParameters(Project project, ApplicationConfiguration configuration, Module module, @NotNull VirtualFile testsFile) {
    configuration.setMainClassName(EduIntelliJNames.TEST_RUNNER_CLASS);
    configuration.setModule(module);
    PsiFile psiFile = PsiManager.getInstance(project).findFile(testsFile);
    Collection<KtClass> ktClasses = PsiTreeUtil.findChildrenOfType(psiFile, KtClass.class);
    for (KtClass ktClass : ktClasses) {
      String name = ktClass.getName();
      configuration.setProgramParameters(name);
    }
  }
}
