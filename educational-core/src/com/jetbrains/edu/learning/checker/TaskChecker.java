package com.jetbrains.edu.learning.checker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class TaskChecker<T extends Task> {
  @NotNull protected final T myTask;
  @NotNull protected final Project myProject;

  public TaskChecker(@NotNull T task, @NotNull Project project) {
    myTask = task;
    myProject = project;
  }

  public void onTaskSolved(@NotNull String message) {
    ApplicationManager.getApplication().invokeLater(
      () -> CheckUtils.showTestResultPopUp(message, MessageType.INFO.getPopupBackground(), myProject));
  }

  public void onTaskFailed(@NotNull String message) {
    ApplicationManager.getApplication()
      .invokeLater(() -> CheckUtils.showTestResultPopUp(message, MessageType.ERROR.getPopupBackground(), myProject));
  }

  public CheckResult check()  {
    return new CheckResult(StudyStatus.Unchecked, "Check for " + myTask.getTaskType() + " task isn't available");
  }

  /**
   * Checks solution for a task on Stepik
   * @return result of a check. If remote check is unsupported returns special instance of check result.
   * @see CheckResult#USE_LOCAL_CHECK
   */
  public CheckResult checkOnRemote()  {
    return CheckResult.USE_LOCAL_CHECK;
  }

  public void clearState() {

  }
}
