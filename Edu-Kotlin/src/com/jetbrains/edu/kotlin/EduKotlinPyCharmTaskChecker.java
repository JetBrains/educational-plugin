package com.jetbrains.edu.kotlin;

import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.edu.learning.StudySubtaskUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.checker.StudyCheckResult;
import com.jetbrains.edu.learning.core.EduNames;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.PyCharmTask;
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks;
import com.jetbrains.edu.learning.intellij.EduIntelliJNames;
import com.jetbrains.edu.learning.intellij.EduPyCharmTasksChecker;
import com.jetbrains.edu.learning.stepic.EduStepicConnector;
import com.jetbrains.edu.learning.stepic.StepicUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtClass;

import java.util.Collection;

public class EduKotlinPyCharmTaskChecker extends EduPyCharmTasksChecker {

  public static final StudyCheckResult FAILED_TO_LAUNCH = new StudyCheckResult(StudyStatus.Unchecked, StudyCheckAction.FAILED_CHECK_LAUNCH);

  public EduKotlinPyCharmTaskChecker(@NotNull PyCharmTask task, @NotNull Project project) {
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
      for (String testFileName : myTask.getTestsText().keySet()) {
        String subTaskFileName = FileUtil.getNameWithoutExtension(testFileName) + EduNames.SUBTASK_MARKER + subTaskIndex + ".kt";
        VirtualFile testFile = VfsUtil.findRelativeFile(taskDir, subTaskFileName);
        if (testFile != null) {
          return testFile;
        }
      }
      String testFileName = StudySubtaskUtils.getTestFileName(myProject, subTaskIndex);
      if (testFileName != null) {
        return taskDir.findChild(testFileName);
      }
      return null;
    }
    else {
      for (String testFileName : myTask.getTestsText().keySet()) {
        VirtualFile testFile = VfsUtil.findRelativeFile(taskDir, testFileName);
        if (testFile != null) {
          return testFile;
        }
      }
      VirtualFile testsFile = taskDir.findChild(EduKotlinPluginConfigurator.LEGACY_TESTS_KT);
      if (testsFile != null) {
        return testsFile;
      }
      return taskDir.findChild(EduKotlinPluginConfigurator.TESTS_KT);
    }
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

  // copy-pasted from PyStudyTaskChecker
  @Override
  public StudyCheckResult checkOnRemote(@Nullable StepicUser user) {
    StudyCheckResult result = check();
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    StudyStatus status = result.getStatus();
    if (user != null && course != null && course.isStudy() && status != StudyStatus.Unchecked) {
      EduStepicConnector.postSolution(myTask, status == StudyStatus.Solved, myProject);
    }
    return result;
  }
}
