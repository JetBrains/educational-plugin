package com.jetbrains.edu.learning.checkio.checker;

import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.EnvironmentChecker;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.details.CheckDetailsView;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class CheckiOTaskChecker extends TaskChecker<EduTask> {
  private final CheckiOMissionCheck myMissionCheck;
  private final EnvironmentChecker myEnvChecker;

  public CheckiOTaskChecker(
    @NotNull EduTask task,
    @NotNull EnvironmentChecker envChecker,
    @NotNull Project project,
    @NotNull CheckiOOAuthConnector oAuthConnector,
    @NotNull String interpreterName,
    @NotNull String testFormTargetUrl
  ) {
    super(task, project);

    myMissionCheck = new CheckiOMissionCheck(
      task,
      project,
      oAuthConnector,
      interpreterName,
      testFormTargetUrl
    );
    myEnvChecker = envChecker;
  }

  @NotNull
  @Override
  public CheckResult check(@NotNull ProgressIndicator indicator) {
    try {
      String possibleError = myEnvChecker.checkEnvironment(project);
      if (possibleError != null) return new CheckResult(CheckStatus.Unchecked, possibleError);

      final CheckResult checkResult =
        ApplicationUtil.runWithCheckCanceled(myMissionCheck, ProgressManager.getInstance().getProgressIndicator());

      if (checkResult.getStatus() != CheckStatus.Unchecked) {
        CheckDetailsView.getInstance(project).showJavaFXResult("CheckiO Response", myMissionCheck.getBrowserPanel());
      }

      return checkResult;
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
      return CheckResult.getFAILED_TO_CHECK();
    }
  }
}
