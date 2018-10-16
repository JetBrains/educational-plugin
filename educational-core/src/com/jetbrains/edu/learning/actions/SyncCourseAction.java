package com.jetbrains.edu.learning.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.stepik.StepikCourseUpdater;
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader;
import com.jetbrains.edu.learning.stepik.StepikUpdateDateExt;
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse;
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo;
import icons.EducationalCoreIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ComponentNotRegistered") // educational-core.xml
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
    assert course instanceof StepikCourse;
    final StepikCourse stepikCourse = (StepikCourse)course;
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating Course", true) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
        if (StepikUpdateDateExt.isUpToDate(stepikCourse)) {
          ApplicationManager.getApplication().invokeLater(() -> {
            Notification notification = new Notification("Update.course", "Course is up to date", "", NotificationType.INFORMATION);
            notification.notify(project);
          });
        }
        else {
          new StepikCourseUpdater(stepikCourse, project).updateCourse();
          StepikUpdateDateExt.setUpdated(stepikCourse);
        }
      }
    });

    if (CCUtils.isCourseCreator(project)) {
      return;
    }

    StepikSolutionsLoader courseSynchronizer = StepikSolutionsLoader.getInstance(project);
    courseSynchronizer.loadSolutionsInBackground();
  }

  public static boolean isAvailable(@Nullable Project project) {
    if (project == null || !EduUtils.isStudyProject(project)) {
      return false;
    }

    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (!(course instanceof StepikCourse)) {
      return false;
    }
    final StepikCourseRemoteInfo remoteInfo = ((StepikCourse)course).getStepikRemoteInfo();
    if (remoteInfo.getLoadSolutions()) {
      return true;
    }
    return false;
  }

  @Override
  public void update(AnActionEvent e) {
    boolean visible = isAvailable(e.getProject());
    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(visible);
  }
}
