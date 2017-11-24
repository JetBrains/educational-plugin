package com.jetbrains.edu.learning.courseFormat.tasks;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.actions.CheckAction;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.CheckUtils;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.stepik.StepicUser;
import com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector;
import org.jetbrains.annotations.NotNull;

public class CodeTask extends Task {
  @SuppressWarnings("unused") //used for deserialization
  public CodeTask() {}

  public CodeTask(@NotNull final String name) {
    super(name);
  }

  @Override
  public String getTaskType() {
    return "code";
  }

  @Override
  public TaskChecker getChecker(@NotNull Project project) {
    return new TaskChecker<CodeTask>(this, project) {
      @Override
      public void onTaskFailed(@NotNull String message) {
        super.onTaskFailed("Wrong solution");
        CheckUtils.showTestResultsToolWindow(myProject, message);
      }

      @Override
      public CheckResult checkOnRemote() {
        StepicUser user = EduSettings.getInstance().getUser();
        if (user == null) {
          return new CheckResult(CheckStatus.Unchecked, CheckAction.LOGIN_NEEDED);
        }
        return StepikAdaptiveConnector.checkCodeTask(myProject, myTask, user);
      }
    };
  }
}
