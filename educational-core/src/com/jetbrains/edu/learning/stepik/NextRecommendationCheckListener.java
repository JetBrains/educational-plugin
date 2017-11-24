package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.checker.CheckListener;
import com.jetbrains.edu.learning.checker.CheckResult;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.jetbrains.annotations.NotNull;

public class NextRecommendationCheckListener implements CheckListener {

  private CheckStatus myStatusBeforeCheck;

  @Override
  public void beforeCheck(@NotNull Project project, @NotNull Task task) {
    myStatusBeforeCheck = task.getStatus();
  }

  @Override
  public void afterCheck(@NotNull Project project, @NotNull Task task, @NotNull CheckResult result) {
    Course course = task.getLesson().getCourse();
    if (!(course instanceof RemoteCourse && course.isAdaptive())) {
      return;
    }
    if (myStatusBeforeCheck == CheckStatus.Solved) {
      return;
    }
    CheckStatus statusAfterCheck = task.getStatus();
    if (statusAfterCheck != CheckStatus.Solved) {
      return;
    }
    ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(project, StepikAdaptiveConnector.LOADING_NEXT_RECOMMENDATION, false,
                                                                                            PerformInBackgroundOption.DEAF) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        StepikAdaptiveConnector.addNextRecommendedTask(project, task.getLesson(), indicator, StepikAdaptiveConnector.NEXT_RECOMMENDATION_REACTION);
      }
    });
  }
}
