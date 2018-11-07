package com.jetbrains.edu.python.learning.checkio.checker;

import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.checker.TaskChecker;
import com.jetbrains.edu.learning.checker.details.CheckDetailsView;
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator;
import com.jetbrains.edu.learning.checkio.CheckiOCourseUpdater;
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse;
import com.jetbrains.edu.learning.checkio.notifications.errors.handlers.CheckiOErrorHandler;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import com.jetbrains.python.PythonFileType;
import org.jetbrains.annotations.NotNull;

public class PyCheckiOTaskChecker extends TaskChecker<EduTask> {

  public PyCheckiOTaskChecker(@NotNull EduTask task, @NotNull Project project) {
    super(task, project);
  }

  @Override
  public void onTaskSolved(@NotNull String message) {
    updateCourse();
    super.onTaskSolved(message);
  }

  private void updateCourse() {
    final CheckiOCourse course = (CheckiOCourse) task.getCourse();

    final CheckiOCourseContentGenerator contentGenerator =
      new CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector.getInstance());

    try {
      new CheckiOCourseUpdater(
        course,
        project,
        contentGenerator
      ).doUpdate();
    }
    catch (Exception e) {
      new CheckiOErrorHandler(
        "Failed to update the course",
        PyCheckiOOAuthConnector.getInstance()
      ).handle(e);
    }
  }

  @NotNull
  @Override
  public CheckResult check(@NotNull ProgressIndicator indicator) {
    final PyCheckiOMissionCheck missionCheck = new PyCheckiOMissionCheck(project, task);

    try {
      final CheckResult checkResult =
        ApplicationUtil.runWithCheckCanceled(missionCheck, indicator);

      if (checkResult.getStatus() != CheckStatus.Unchecked) {
        CheckDetailsView.getInstance(project).showJavaFXResult("CheckiO Response", missionCheck.getBrowserPanel());
      }

      return checkResult;
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
      return CheckResult.FAILED_TO_CHECK;
    }
  }
}
