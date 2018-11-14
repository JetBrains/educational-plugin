package com.jetbrains.edu.learning.checkio.checker;

import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.details.CheckDetailsView;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import org.jetbrains.annotations.NotNull;

public class CheckiOTaskChecker extends TaskChecker<EduTask> {
  private final CheckiOMissionCheck myMissionCheck;

  public CheckiOTaskChecker(
    @NotNull EduTask task,
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
  }

  @NotNull
  @Override
  public CheckResult check(@NotNull ProgressIndicator indicator) {
    try {
      final CheckResult checkResult =
        ApplicationUtil.runWithCheckCanceled(myMissionCheck, ProgressManager.getInstance().getProgressIndicator());

      if (checkResult.getStatus() != CheckStatus.Unchecked) {
        CheckDetailsView.getInstance(project).showJavaFXResult("CheckiO Response", myMissionCheck.getBrowserPanel());
      }

      return checkResult;
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
      return CheckResult.FAILED_TO_CHECK;
    }
  }
}
