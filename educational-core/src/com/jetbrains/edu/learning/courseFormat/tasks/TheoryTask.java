package com.jetbrains.edu.learning.courseFormat.tasks;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import org.jetbrains.annotations.NotNull;

public class TheoryTask extends Task {
  @SuppressWarnings("unused") //used for deserialization
  public TheoryTask() {}

  public TheoryTask(@NotNull final String name) {
    super(name);
  }

  @Override
  public String getTaskType() {
    return "theory";
  }

  @Override
  public TaskChecker getChecker(@NotNull Project project) {
    return new TaskChecker<TheoryTask>(this, project) {
      @Override
      public void onTaskSolved(@NotNull String message) {
      }

      @Override
      public CheckResult check() {
        return new CheckResult(CheckStatus.Solved, "");
      }
    };
  }
}
