package com.jetbrains.edu.kotlin.twitter;

import com.intellij.openapi.project.Project;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.CheckStatus;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.twitter.TwitterSettings;
import com.jetbrains.edu.learning.twitter.TwitterPluginConfigurator;
import com.jetbrains.edu.learning.twitter.TwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KtTwitterConfigurator implements TwitterPluginConfigurator {

  @Override
  public boolean askToTweet(@NotNull Project project, @NotNull Task solvedTask, @NotNull CheckStatus statusBeforeCheck) {
    StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    Course course = studyTaskManager.getCourse();
    if (course != null && course.getName().equals("Kotlin Koans")) {
      TwitterSettings settings = TwitterSettings.getInstance();
      return settings.askToTweet()
             && solvedTask.getStatus() == CheckStatus.Solved
             && (statusBeforeCheck == CheckStatus.Unchecked || statusBeforeCheck == CheckStatus.Failed)
             && calculateTaskNumber(solvedTask) % 8 == 0;
    }
    return false;
  }

  @Nullable
  @Override
  public TwitterUtils.TwitterDialogPanel getTweetDialogPanel(@NotNull Task solvedTask) {
    return new KtTwitterDialogPanel(solvedTask);
  }

  public static int calculateTaskNumber(@NotNull final Task solvedTask) {
    Lesson lesson = solvedTask.getLesson();
    Course course = lesson.getCourse();
    int solvedTaskNumber = 0;
    for (Lesson currentLesson : course.getLessons()) {
      for (Task task : currentLesson.getTaskList()) {
        if (task.getStatus() == CheckStatus.Solved) {
          solvedTaskNumber++;
        }
      }
    }
    return solvedTaskNumber;
  }
}
