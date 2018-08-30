package com.jetbrains.edu.learning.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.courseFormat.remote.CourseRemoteInfo;
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.stepik.StepikAdaptiveConnector;
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater;
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader;
import com.jetbrains.edu.learning.stepik.StepikUpdateDateExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikCourseExt;
import com.jetbrains.edu.learning.stepik.courseFormat.ext.StepikTaskExt;
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
    if (course instanceof StepikCourse) {
      ProgressManager.getInstance().run(new com.intellij.openapi.progress.Task.Backgroundable(project, "Updating Course", true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);

          if (StepikUpdateDateExt.isUpToDate((StepikCourse)course)) {
            ApplicationManager.getApplication().invokeLater(() -> {
              Notification notification = new Notification("Update.course", "Course is up to date", "", NotificationType.INFORMATION);
              notification.notify(project);
            });
          }
          else {
            new StepikCourseUpdater((StepikCourse)course, project).updateCourse();
            StepikUpdateDateExt.setUpdated((StepikCourse)course);
          }
        }
      });

      if (CCUtils.isCourseCreator(project)) {
        return;
      }

      if (StepikCourseExt.isAdaptive((StepikCourse)course)) {
        updateAdaptiveCourse(project, course);
      }
      else {
        StepikSolutionsLoader courseSynchronizer = StepikSolutionsLoader.getInstance(project);
        courseSynchronizer.loadSolutionsInBackground();
      }
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
        Task lastRecommendationOnStepik = StepikAdaptiveConnector.getNextRecommendation(project, (StepikCourse) course);

        if (lastRecommendationOnStepik != null && StepikTaskExt.getStepId(lastRecommendationOnStepik) != StepikTaskExt.getStepId(lastRecommendationInCourse)) {
          lastRecommendationOnStepik.init(course, adaptiveLesson, false);
          StepikAdaptiveConnector.replaceCurrentTask(project, lastRecommendationOnStepik, lastRecommendationInCourse.getName(), adaptiveLesson);

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
    if (course != null) {
      final CourseRemoteInfo remoteInfo = course.getRemoteInfo();
      if (!(course instanceof StepikCourse) || remoteInfo instanceof StepikCourseRemoteInfo && !((StepikCourseRemoteInfo)remoteInfo).isLoadSolutions()) {
        return false;
      }
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
