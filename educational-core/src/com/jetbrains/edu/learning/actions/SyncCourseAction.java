package com.jetbrains.edu.learning.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.RemoteCourse;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.stepic.StepicAdaptiveConnector;
import com.jetbrains.edu.learning.stepic.StepikSolutionsLoader;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SyncCourseAction extends DumbAwareAction {

  public SyncCourseAction() {
    super("Synchronize Course", "Synchronize Course", EducationalCoreIcons.StepikRefresh);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      doUpdate(project);
    }
  }

  public static void doUpdate(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    if (course.isAdaptive()) {
      updateAdaptiveCourse(project, course);
    }
    else {
      StepikSolutionsLoader courseSynchronizer = StepikSolutionsLoader.getInstance(project);
      courseSynchronizer.loadSolutionsInBackground();
    }
  }

  private static void updateAdaptiveCourse(@NotNull Project project, @NotNull Course course) {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
      EduUtils.execCancelable(() -> {
        Lesson adaptiveLesson = course.getLessons().get(0);
        assert adaptiveLesson != null;

        int taskNumber = adaptiveLesson.getTaskList().size();
        Task lastRecommendationInCourse = adaptiveLesson.getTaskList().get(taskNumber - 1);
        Task lastRecommendationOnStepik = StepicAdaptiveConnector.getNextRecommendation(project, (RemoteCourse) course);

        if (lastRecommendationOnStepik != null && lastRecommendationOnStepik.getStepId() != lastRecommendationInCourse.getStepId()) {
          lastRecommendationOnStepik.initTask(adaptiveLesson, false);
          StepicAdaptiveConnector.replaceCurrentTask(project, lastRecommendationOnStepik, adaptiveLesson);

          ApplicationManager.getApplication().invokeLater(() -> {
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            NavigationUtils.navigateToTask(project, lastRecommendationOnStepik);
          });
          return true;
        }

        return false;
      });
    }, "Synchronizing Course", true, project);
  }

  public static boolean isAvailable(@Nullable Project project) {
    if (project == null) {
      return false;
    }

    if (!EduUtils.isStudyProject(project)) {
      return false;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof RemoteCourse) || !((RemoteCourse) course).isLoadSolutions()) {
      return false;
    }

    return true;
  }

  @Override
  public void update(AnActionEvent e) {
    boolean visible = isAvailable(e.getProject());
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(visible);
  }
}
