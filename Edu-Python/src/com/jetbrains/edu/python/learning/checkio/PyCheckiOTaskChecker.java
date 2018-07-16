package com.jetbrains.edu.python.learning.checkio;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOTaskChecker extends TaskChecker<EduTask> {
  public PyCheckiOTaskChecker(@NotNull EduTask task,
                              @NotNull Project project) {
    super(task, project);
  }

  @Override
  public void onTaskSolved(@NotNull String message) {
    super.onTaskSolved(message);
  }

  @Override
  public void onTaskFailed(@NotNull String message) {
    super.onTaskFailed(message);
  }

  @NotNull
  @Override
  public CheckResult check() {
    return super.check();
  }
}
